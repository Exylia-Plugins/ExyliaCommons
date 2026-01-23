package net.exylia.commons.redis.pubsub;

@Deprecated
public class ChannelMessage {
    private final String channel;
    private final String message;
    private final long timestamp;

    public ChannelMessage(String channel, String message) {
        this.channel = channel;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ChannelMessage(String channel, String message, long timestamp) {
        this.channel = channel;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ChannelMessage{" +
                "channel='" + channel + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChannelMessage that = (ChannelMessage) o;

        if (timestamp != that.timestamp) return false;
        if (!channel.equals(that.channel)) return false;
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int result = channel.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
