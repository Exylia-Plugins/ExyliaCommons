package net.exylia.commons.redis.pubsub;

@Deprecated
public class PatternMessage {
    private final String pattern;
    private final String channel;
    private final String message;
    private final long timestamp;

    public PatternMessage(String pattern, String channel, String message) {
        this.pattern = pattern;
        this.channel = channel;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public PatternMessage(String pattern, String channel, String message, long timestamp) {
        this.pattern = pattern;
        this.channel = channel;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getPattern() {
        return pattern;
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
        return "PatternMessage{" +
                "pattern='" + pattern + '\'' +
                ", channel='" + channel + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PatternMessage that = (PatternMessage) o;

        if (timestamp != that.timestamp) return false;
        if (!pattern.equals(that.pattern)) return false;
        if (!channel.equals(that.channel)) return false;
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int result = pattern.hashCode();
        result = 31 * result + channel.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
