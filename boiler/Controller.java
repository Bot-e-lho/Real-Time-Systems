package boiler;

import javax.realtime.*;
import java.time.LocalTime;

public class Controller {
    private final Boiler boiler;
    private final Pump pump1, pump2;
    private final Sensors.LevelSensor levelSensor;
    private final Sensors.VaporSensor vaporSensor;
    private final EvacuationValve valve;
    private Mode mode = Mode.INITIALIZATION;
    private int safeCycles = 0;
    private static final int SAFE_CYCLES_NEEDED = 3;

    public Controller(Boiler b, Pump p1, Pump p2, Sensors.LevelSensor ls, Sensors.VaporSensor vs, EvacuationValve v) {
        this.boiler = b;
        this.pump1 = p1;
        this.pump2 = p2;
        this.levelSensor = ls;
        this.vaporSensor = vs;
        this.valve = v;
    }

    public Mode getMode() { 
        return mode; 
    }

    public void controlCycle() {
        Double level = levelSensor.read();
        Double vapor = vaporSensor.read();
        double totalPumpThroughput = pump1.throughput() + pump2.throughput();
        boiler.setPumpThroughput(totalPumpThroughput);
        boiler.setVaporThroughput(boiler.V);

        log("START cycle mode=" + mode + " level=" + (level==null?"NULL":String.format("%.2f", level))
                + " vapor=" + (vapor==null?"NULL":String.format("%.2f", vapor))
                + " pumpsOK=" + pumpsWorking());

        boolean safetyViolation = false;
        if (mode != Mode.INITIALIZATION) {
            safetyViolation =
                (level != null && (level <= boiler.M1 || level >= boiler.M2)) ||
                (level == null && (vapor == null || (vapor != null && vapor > boiler.V * 1.5)));
        } else {
             safetyViolation = (level == null || (vapor != null && vapor > 0));
        }

        if (safetyViolation) {
            safeCycles = 0;
            if (mode != Mode.EMERGENCY_STOP) {
                System.out.println("[ALERTA] Violação de segurança -> EMERGENCY_STOP");
            }
            mode = Mode.EMERGENCY_STOP;
        } else {
            if (mode == Mode.EMERGENCY_STOP) {
                safeCycles++;
                if (safeCycles >= SAFE_CYCLES_NEEDED) {
                    System.out.println("[RECOVER] Condição de segurança restaurada.");
                    if (levelSensor.isWorking()) {
                        mode = (pumpsWorking() < 2) ? Mode.DEGRADED : Mode.NORMAL;
                        System.out.println("[RECOVER] Transicionando para " + mode);
                    } else {
                        mode = Mode.SALVAMENTO;
                        System.out.println("[RECOVER] Sensor de nível falho. Transicionando para SALVAMENTO.");
                    }
                    safeCycles = 0;
                }
            } else {
                safeCycles = Math.min(safeCycles + 1, SAFE_CYCLES_NEEDED);
            }
        }

        if (mode != Mode.EMERGENCY_STOP && mode != Mode.INITIALIZATION) {
            if (!levelSensor.isWorking()) {
                if (mode != Mode.SALVAMENTO) {
                    System.out.println("[MODO] Falha no sensor de nível -> SALVAMENTO");
                }
                mode = Mode.SALVAMENTO;
            } else if (mode == Mode.SALVAMENTO && levelSensor.isWorking()) {
                mode = (pumpsWorking() < 2) ? Mode.DEGRADED : Mode.NORMAL;
                System.out.println("[MODO] Sensor de nivel reparado -> " + mode);
            } else if (pumpsWorking() < 2) {
                if (mode != Mode.DEGRADED) {
                    System.out.println("[MODO] Entrando em DEGRADADO devido a falha de bomba.");
                }
                mode = Mode.DEGRADED;
            } else if (mode == Mode.DEGRADED && pumpsWorking() == 2) {
                mode = Mode.NORMAL;
                System.out.println("[MODO] Recuperado -> NORMAL");
            }
        }
        double currentVaporThroughput = 0.0;
        switch (mode) {
            case INITIALIZATION:
                if (level != null) {
                    if (level < boiler.N1) {
                        pump1.setRunning(true);
                        pump2.setRunning(true);
                    } else if (level > boiler.N2) {
                        valve.open();
                    } else {
                        valve.close();
                        pump1.setRunning(false);
                        pump2.setRunning(false);
                        mode = (pumpsWorking() < 2) ? Mode.DEGRADED : Mode.NORMAL;
                        System.out.println("[INIT] Nível ajustado. Transicionando para: " + mode);
                    }
                }
                if (level == null) {
                    System.out.println("[INIT] Falha de sensor de nível -> EMERGENCY_STOP");
                    mode = Mode.EMERGENCY_STOP;
                    break;
                }
                if (vapor != null && vapor > 0) {
                    System.out.println("[INIT] Vapor presente na inicialização -> EMERGENCY_STOP");
                    mode = Mode.EMERGENCY_STOP;
                    break;
                }
                break;

            case EMERGENCY_STOP:
                pump1.setRunning(false); 
                pump2.setRunning(false);
                valve.close();
                log("EMERGENCY_STOP: bombas desligadas e vazão de vapor interrompida");
                break;

            case NORMAL:
                currentVaporThroughput = boiler.V;
                controlNormal(level);
                break;

            case DEGRADED:
                currentVaporThroughput = boiler.V;
                controlDegraded(level);
                break;

            case SALVAMENTO:
                controlSalvamento(vapor);
                break;

            default: break;
        }

        boiler.setVaporThroughput(currentVaporThroughput);
        double total = pump1.throughput() + pump2.throughput();
        boiler.setPumpThroughput(total);
        log("END cycle pumps=" + pump1.isRunning() + "," + pump2.isRunning() + " throughput=" + String.format("%.2f", total));
    }

    private void controlNormal(Double level) {
        if (level == null) {
            //mode = Mode.SALVAMENTO;
            return;
        }

        if (level < boiler.N1) {
            pump1.setRunning(true);
            pump2.setRunning(true);
        } else if (level > boiler.N2) {
            pump1.setRunning(false);
            pump2.setRunning(false);
        } else {
            if (level < boiler.N1 + 50) {
                if (pump1.isWorking()) 
                    pump1.setRunning(true);
                if (pumpsWorking() == 2) 
                    pump2.setRunning(false);
            } else if (level > boiler.N2 - 50) {
                 pump1.setRunning(false);
                 pump2.setRunning(false);
            }
        }
    }

    private void controlDegraded(Double level) {
        Pump avail = pump1.isWorking() ? pump1 : (pump2.isWorking() ? pump2 : null);
        if (avail == null || level == null) 
            return;
        avail.setRunning(level < boiler.N1);
        Pump other = (avail == pump1) ? pump2 : pump1;
        other.setRunning(false);
    }

    private void controlSalvamento(Double vapor) {
        Pump candidate = pump1.isWorking() ? pump1 : (pump2.isWorking() ? pump2 : null);
        if (candidate == null)
            return; 
        candidate.setRunning(true);
        if (candidate == pump1) pump2.setRunning(false);
        else pump1.setRunning(false);
    }

    private int pumpsWorking() {
        int c = 0; 
        if (pump1.isWorking()) 
            c++; 
        if (pump2.isWorking()) 
            c++; 
        return c;
    }

    private void log(String s) {
        System.out.println("[" + LocalTime.now() + "][Controller] " + s);
    }
}
