package boiler;

public class Message {
    public final String src;
    public final String topic;
    public final Object payload;

    public Message(String src, String topic, Object payload) {
        this.src = src;
        this.topic = topic;
        this.payload = payload;
    }

    public String toString() {
        return "Msg[from=" + src + ",topic=" + topic + ",payload=" + payload + "]";
    }
}
