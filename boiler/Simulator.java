package boiler;

import javax.realtime.*;
import java.util.concurrent.TimeUnit;


public class Simulator {

    private static final double VALVE_DRAIN_PER_SEC = 100.0;

    public static void main(String[] args) throws Exception {
        final Boiler boiler = new Boiler(500.0);
        final Pump p1 = new Pump("P1", boiler.P);
        final Pump p2 = new Pump("P2", boiler.P);
        final Sensors.LevelSensor levelSensor = new Sensors.LevelSensor(boiler);
        final Sensors.VaporSensor vaporSensor = new Sensors.VaporSensor(boiler);
        final EvacuationValve valve = new EvacuationValve();
        final Controller controller = new Controller(boiler, p1, p2, levelSensor, vaporSensor, valve);

        // THREAD DE FÍSICA (1s)
        PeriodicParameters physicsPeriod = new PeriodicParameters(null, new RelativeTime(1000, 0));
        RealtimeThread physicsThread = new RealtimeThread(new PriorityParameters(PriorityScheduler.MAX_PRIORITY - 10), physicsPeriod) {
            public void run() {
                while (!isInterrupted()) {
                    if (valve.isOpen()) 
                        boiler.drain(VALVE_DRAIN_PER_SEC);
                    boiler.updateOneSecond();
                    System.out.println("[PHYS] q=" + String.format("%.2f", boiler.getQ())
                            + " p=" + String.format("%.2f", boiler.getP()) + " v=" + boiler.getV());

                    if (boiler.isUnderMin()) {
                        System.out.println("[PHYS][ALERT] q <= M1 (" + boiler.M1 + ")");
                    }
                    if (boiler.isOverMax()) {
                        System.out.println("[PHYS][ALERT] q >= M2 (" + boiler.M2 + ")");
                    }
                    waitForNextPeriod();
                }
            }
        };

        // CONTROLE (5s)
        PeriodicParameters controlPeriod = new PeriodicParameters(null, new RelativeTime(5000, 0));
        RealtimeThread controlThread = new RealtimeThread(new PriorityParameters(PriorityScheduler.MAX_PRIORITY - 20), controlPeriod) {
            public void run() {
                while (!isInterrupted()) {
                    controller.controlCycle();
                    waitForNextPeriod();
                }
            }
        };

        Thread faultInjector = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(30);
                p1.fail();
                TimeUnit.SECONDS.sleep(30);
                levelSensor.fail();
                TimeUnit.SECONDS.sleep(30);
                vaporSensor.fail();
                // reparos
                TimeUnit.SECONDS.sleep(30);
                p1.repair();
                TimeUnit.SECONDS.sleep(30);
                levelSensor.repair();
            } catch (InterruptedException ignored) {}
        }, "fault-injector");
        faultInjector.setDaemon(true);

        physicsThread.start();
        controlThread.start();
        faultInjector.start();

        if (args.length > 0) {
            try {
                int runSeconds = Integer.parseInt(args[0]);
                Thread.sleep(runSeconds * 1000L);
                System.out.println(">>> Requested shutdown after " + runSeconds + "s");
            } catch (NumberFormatException ignored) {}
            physicsThread.interrupt();
            controlThread.interrupt();
        }

        physicsThread.join();
        controlThread.join();
        System.out.println("Simulator terminated.");
    }
}
