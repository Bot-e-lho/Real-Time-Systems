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

    private double estimatedLevel = -1.0;
    private Double lastKnownLevel = null;


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

        log("Inicio mode=" + mode + " level=" + (level == null ? "NULL" : String.format("%.2f", level))
                + " vapor=" + (vapor == null ? "NULL" : String.format("%.2f", vapor))
                + " pumpsOK=" + pumpsWorking());

        if (level != null) {
            lastKnownLevel = level;
        }

        boolean safetyViolation = false;
        if (mode != Mode.INITIALIZATION) {
            boolean levelOutOfBounds = (level != null) && (level <= boiler.M1 || level >= boiler.M2);
            boolean bothSensorsFailed = (level == null && vapor == null);
            boolean vaporTooHigh = (vapor != null && vapor > boiler.V * 1.5);
            safetyViolation = levelOutOfBounds || bothSensorsFailed || vaporTooHigh;
        } else {
            safetyViolation = (level == null || (vapor != null && vapor > 0));
        }

        if (safetyViolation) {
            safeCycles = 0;
            if (mode != Mode.EMERGENCY_STOP) {
                System.out.println("[ALERTA] Violacao de seguranca -> EMERGENCY_STOP");
            }
            mode = Mode.EMERGENCY_STOP;
        } else {
            if (mode == Mode.EMERGENCY_STOP) {
                safeCycles++;
                if (safeCycles >= SAFE_CYCLES_NEEDED) {
                    if (levelSensor.isWorking() && level > boiler.M1 && level < boiler.M2) {
                        System.out.println("[RECOVER] Condição de segurança fisica restaurada.");
                        mode = (pumpsWorking() < 2) ? Mode.DEGRADED : Mode.NORMAL;
                        System.out.println("[RECOVER] Transicionando para " + mode);
                        safeCycles = 0;
                    } else if (!levelSensor.isWorking()) {
                        System.out.println("[RECOVER] Sensor de nivel falho. Transicionando para SALVAMENTO para tentar recuperacao.");
                        mode = Mode.SALVAMENTO;
                        estimatedLevel = -1.0; 
                        safeCycles = 0;
                    }
                }
            } else {
                safeCycles = Math.min(safeCycles + 1, SAFE_CYCLES_NEEDED);
            }
        }

        if (mode != Mode.EMERGENCY_STOP && mode != Mode.INITIALIZATION) {
            if (!levelSensor.isWorking()) {
                if (mode != Mode.SALVAMENTO) {
                    System.out.println("[MODO] Falha no sensor de nivel -> SALVAMENTO");
                    estimatedLevel = -1.0; 
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
                        System.out.println("[INIT] Nivel ajustado. Transicionando para: " + mode);
                    }
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
                currentVaporThroughput = (vapor != null) ? vapor : boiler.V; 
                controlSalvamento(currentVaporThroughput);
                break;

            default:
                break;
        }

        boiler.setVaporThroughput(currentVaporThroughput);
        double total = pump1.throughput() + pump2.throughput();
        boiler.setPumpThroughput(total);
        log("Fim pumps=" + pump1.isRunning() + "," + pump2.isRunning() + " throughput=" + String.format("%.2f", total));
    }

    private void controlNormal(Double level) {
        if (level == null) return;

        if (level < boiler.N1) {
            log("[CONTROL NORMAL] Nivel abaixo de N1. Ligando ambas as bombas.");
            pump1.setRunning(true);
            pump2.setRunning(true);
        } else if (level > boiler.N2) {
            log("[CONTROL NORMAL] Nivel acima de N2. Desligando ambas as bombas.");
            pump1.setRunning(false);
            pump2.setRunning(false);
        }
    }

    private void controlDegraded(Double level) {
        Pump avail = pump1.isWorking() ? pump1 : (pump2.isWorking() ? pump2 : null);
        if (avail == null || level == null)
            return;

        if (level < boiler.N1) {
            avail.setRunning(true);
        } else if (level > boiler.N2) {
            avail.setRunning(false);
        }

        Pump other = (avail == pump1) ? pump2 : pump1;
        other.setRunning(false);
    }

    private void controlSalvamento(double vaporThroughput) {
        if (estimatedLevel < 0) {
            if (lastKnownLevel != null) {
                estimatedLevel = lastKnownLevel;
            } else {
                estimatedLevel = (boiler.N1 + boiler.N2) / 2.0;
            }
            log("[SALVAMENTO] Iniciando estimativa de nivel em: " + estimatedLevel);
        }

        Pump candidate = pump1.isWorking() ? pump1 : (pump2.isWorking() ? pump2 : null);
        if (candidate == null) {
            log("[SALVAMENTO] Nenhuma bomba disponivel.");
            return;
        }

        double pumpThroughput = candidate.isRunning() ? candidate.throughput() : 0;
        estimatedLevel += (pumpThroughput - vaporThroughput) * 5.0; 

        log("[SALVAMENTO] Nível estimado: " + String.format("%.2f", estimatedLevel));

        if (estimatedLevel < boiler.N1) {
            candidate.setRunning(true);
        } else if (estimatedLevel > boiler.N2) {
            candidate.setRunning(false);
        }

        Pump other = (candidate == pump1) ? pump2 : pump1;
        other.setRunning(false);
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
