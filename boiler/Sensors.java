package boiler;
public class Sensors {
    public static class LevelSensor {
        private volatile boolean working = true;
        private final Boiler boiler;
        public LevelSensor(Boiler b) { 
            this.boiler = b; 
        }
        public Double read() {
            if (!working) return null;
            return boiler.getQ();
        }
        public void fail() { 
            working = false; 
            System.out.println("[SENSOR] Sensor Falhou"); }
        public void repair() { working = true; System.out.println("[SENSOR] Sensor Reparado"); }
        public boolean isWorking() { 
            return working; 
        }
    }

    public static class VaporSensor {
        private volatile boolean working = true;
        private final Boiler boiler;
        public VaporSensor(Boiler b) { 
            this.boiler = b; 
        }

        public Double read() {
            if (!working) 
                return null;
            return boiler.getV();
        }

        public void fail() { 
            working = false; 
            System.out.println("[SENSOR] Vapor sensor falhou"); }
        public void repair() { 
            working = true; 
            System.out.println("[SENSOR] Vapor sensor reparado"); }
        public boolean isWorking() { 
            return working; 
        }
    }
}