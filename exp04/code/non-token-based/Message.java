import java.io.Serializable;

/**
 * Message class for Ricart-Agrawala Algorithm
 * Supports REQUEST and REPLY message types
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        REQUEST, // Request to enter critical section
        REPLY, // Permission granted
        START, // Signal to start experiment
        DONE // Signal that node completed
    }

    private Type type;
    private int senderId;
    private int timestamp; // Lamport timestamp
    private int csNumber; // Which CS request this is for

    public Message(Type type, int senderId, int timestamp) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.csNumber = 0;
    }

    public Message(Type type, int senderId, int timestamp, int csNumber) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.csNumber = csNumber;
    }

    public Type getType() {
        return type;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getCsNumber() {
        return csNumber;
    }

    @Override
    public String toString() {
        return String.format("%s(from=%d, ts=%d, cs=%d)", type, senderId, timestamp, csNumber);
    }

    /**
     * Compare priority for Ricart-Agrawala algorithm
     * Returns true if this message has HIGHER priority (should go first)
     * Priority: lower timestamp wins, ties broken by lower nodeId
     */
    public boolean hasHigherPriorityThan(Message other) {
        if (this.timestamp != other.timestamp) {
            return this.timestamp < other.timestamp;
        }
        return this.senderId < other.senderId;
    }
}
