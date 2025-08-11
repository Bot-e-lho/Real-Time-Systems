package boiler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageBus {
    private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

    public void publish(Message m) {
        queue.offer(m);
    }

    public Message take() throws InterruptedException {
        return queue.take();
    }

    public Message poll() {
        return queue.poll();
    }
}