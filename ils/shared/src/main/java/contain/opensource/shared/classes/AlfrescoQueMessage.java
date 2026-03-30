package contain.opensource.shared.classes;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Enhanced Alfresco Message POJO
 * Handles both standard Node events and "Nuclear" Metadata Update events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoQueMessage {
    // Root Level Fields (Common to all events)
    private String id;
    private String type;
    private String username;
    private long timestamp;
    private int seqNumber;
    private String txnId;
    private String networkId;
    private Object client;

    // Alfresco Specific (Root level for Add/Delete events)
    private String nodeId;
    private String siteId;
    private String nodeType;
    private String name;
    private long nodeModificationTime;

    private List<Object> paths; 
    private List<Object> parentNodeIds;
    private List<Object> aspects;
    private Map<String, Object> nodeProperties;

    // --- CRITICAL ADDITIONS FOR METADATA UPDATES ---
    // These capture the "Nuclear" property maps sent during updates
    private Map<String, Object> before;
    private Map<String, Object> after;

    /**
     * Smart Getter for NodeId.
     * Metadata Updates often hide the ID inside the 'after' block.
     */
    public String getNodeId() {
        if (nodeId != null) return nodeId;
        if (after != null && after.containsKey("id")) {
            return (String) after.get("id");
        }
        return null;
    }

    /**
     * Smart Getter for Name.
     * Updates might store the filename inside the 'after' properties.
     */
    public String getName() {
        if (name != null) return name;
        if (after != null && after.containsKey("name")) {
            return (String) after.get("name");
        }
        return "Unknown_Node";
    }

    // --- Standard Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getSeqNumber() { return seqNumber; }
    public void setSeqNumber(int seqNumber) { this.seqNumber = seqNumber; }

    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }

    public String getNetworkId() { return networkId; }
    public void setNetworkId(String networkId) { this.networkId = networkId; }

    public Object getClient() { return client; }
    public void setClient(Object client) { this.client = client; }

    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public void setName(String name) { this.name = name; }

    public long getNodeModificationTime() { return nodeModificationTime; }
    public void setNodeModificationTime(long nodeModificationTime) { this.nodeModificationTime = nodeModificationTime; }

    public List<Object> getPaths() { return paths; }
    public void setPaths(List<Object> paths) { this.paths = paths; }

    public List<Object> getParentNodeIds() { return parentNodeIds; }
    public void setParentNodeIds(List<Object> parentNodeIds) { this.parentNodeIds = parentNodeIds; }

    public List<Object> getAspects() { return aspects; }
    public void setAspects(List<Object> aspects) { this.aspects = aspects; }

    public Map<String, Object> getNodeProperties() { return nodeProperties; }
    public void setNodeProperties(Map<String, Object> nodeProperties) { this.nodeProperties = nodeProperties; }

    public Map<String, Object> getBefore() { return before; }
    public void setBefore(Map<String, Object> before) { this.before = before; }

    public Map<String, Object> getAfter() { return after; }
    public void setAfter(Map<String, Object> after) { this.after = after; }
}