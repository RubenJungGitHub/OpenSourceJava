package contain.opensource.ils.bs.receiver.classes.migration;


public class MigrationQueueMessage {

    private String id;
    private String key;
    private String action;
    private String source;
    private String destination;

    // Default constructor (required for Jackson)
    public MigrationQueueMessage() {
    }

    // Constructor with all properties
    public MigrationQueueMessage(String key, String action,  String source, String destination, String id) {
        this.id =id;
        this.key = key;
        this.action = action;
        this.source = source;
        this.destination = destination;
    }

    // Getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    // Getters and setters
    public String getID() { return id; }
    public void sertIDy(String id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}