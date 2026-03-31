package contain.opensource.shared.classes;

public class MigrationQueueMessage {

    private String id;
    private String key;
    private String action;
    private String source;
    private String platformto;
    private String containerto;
    private String deltalink;
    private String listid = "-";

    // Default constructor (required for Jackson)
    public MigrationQueueMessage() {
    }

    // Constructor with all properties
    public MigrationQueueMessage(String key, String action, String source, String id, String deltalink,
            String platformto, String containerto) {
        this.id = id;
        this.key = key;
        this.action = action;
        this.source = source;
        this.platformto = platformto;
        this.containerto = containerto;
        this.deltalink = deltalink;
        if (this.source.equals("SPO")) {
            this.listid = this.deltalink.split("/lists/")[1].split("/")[0];
        }
    }

    // Getters and setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    // Getters and setters
    public String getID() {
        return id;
    }

    public String getlistid() {
        return listid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getplatformto() {
        return platformto;
    }

    public void setplatformto(String platformto) {
        this.platformto = platformto;
    }

    public String getcontainerto() {
        return containerto;
    }

    public void setcontainerto(String containerto) {
        this.containerto = containerto;
    }
}