package boiler;


import java.util.concurrent.TimeUnit;

public class Pump {
    private final String id;
    private final double capacity;
    private volatile boolean working = true;
    private volatile boolean running = false;
    private long startCommandTime = -1;
    private final long startupDelayMillis = TimeUnit.SECONDS.toMillis(2);


    public Pump(String id, double capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public double throughput() {
        if (!working || !running) 
            return 0.0;

        if (running && startCommandTime != -1 && System.currentTimeMillis() - startCommandTime < startupDelayMillis) 
            return 0.0;
        
        return running ? capacity : 0.0;
    }

    public void setRunning(boolean r) {
        if (r && !running) {
            startCommandTime = System.currentTimeMillis();
        } else if (!r){
            startCommandTime = -1;
        }
        running = r;
        
    }

    public boolean isRunning() { 
        return running; 
    }
    public boolean isWorking() { 
        return working; 
    }

    public void fail() {
        working = false; 
        running = false;
        startCommandTime = -1;
        System.out.println("[EVENT] Bomba " + id + " Falhou");
    }

    public void repair() {
        working = true;
        System.out.println("[EVENT] Bomba " + id + " Reparada");
    }

    public String getId() { 
        return id; 
    }

    @Override
    public String toString() {
        return String.format("Bomba{id=%s,cap=%.1f,working=%b,running=%b}", id, capacity, working, running);
    }
}