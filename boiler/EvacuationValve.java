package boiler;

public class EvacuationValve {
    private volatile boolean open = false;

    public synchronized void open() {
        if (!open) {
            open = true;
            System.out.println("[VALVE] Valvula aberta");
        }
    }

    public synchronized void close() {
        if (open) {
            open = false;
            System.out.println("[VALVE] Valvula fechada");
        }
    }

    public synchronized boolean isOpen() {
        return open;
    }
}