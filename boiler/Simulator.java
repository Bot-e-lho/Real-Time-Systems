package boiler;

import javax.realtime.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;


public class Simulator {

    private static final double VALVE_DRAIN_PER_SEC = 10.0;

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
                    System.out.println("[TF] q=" + String.format("%.2f", boiler.getQ())
                            + " p=" + String.format("%.2f", boiler.getP()) + " v=" + boiler.getV());

                    if (boiler.isUnderMin()) {
                        System.out.println("[TF][ALERT] q <= M1 (" + boiler.M1 + ")");
                    }
                    if (boiler.isOverMax()) {
                        System.out.println("[TF][ALERT] q >= M2 (" + boiler.M2 + ")");
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

        final Random random = new Random();

        Thread faultInjector = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    int sleepTimeSeconds = 10 + random.nextInt(51); 
                    TimeUnit.SECONDS.sleep(sleepTimeSeconds);

                    int device = random.nextInt(4);

                    switch (device) {
                        case 0:
                            if (p1.isWorking()) {
                                p1.fail();
                            } else {
                                p1.repair();
                            }
                            break;
                        case 1:
                            if (p2.isWorking()) {
                                p2.fail();
                            } else {
                                p2.repair();
                            }
                            break;
                        case 2:
                            if (levelSensor.isWorking()) {
                                levelSensor.fail();
                            } else {
                                levelSensor.repair();
                            }
                            break;
                        case 3:
                            if (vaporSensor.isWorking()) {
                                vaporSensor.fail();
                            } else {
                                vaporSensor.repair();
                            }
                            break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "fault-injector");

        faultInjector.setDaemon(true);

        physicsThread.start();
        controlThread.start();
        faultInjector.start();

        if (args.length > 0) {
            try {
                int runSeconds = Integer.parseInt(args[0]);
                Thread.sleep(runSeconds * 1000L);
            } catch (NumberFormatException ignored) {}
            physicsThread.interrupt();
            controlThread.interrupt();
        }

        physicsThread.join();
        controlThread.join();
    }
}
