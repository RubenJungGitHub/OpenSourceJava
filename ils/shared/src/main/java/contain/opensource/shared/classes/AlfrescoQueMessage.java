package contain.opensource.shared.classes;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a message structure for Alfresco queue processing.
 * 
 * This class is used to encapsulate information about Alfresco-related events
 * or actions,
 * including metadata such as node details, user information, and additional
 * properties.
 * It is designed to be deserialized from JSON, ignoring unknown properties.
 *
 *
 * 
 * Fields:
 *
 * <b>id</b>: Unique identifier for the message.
 * <b>type</b>: Type of the message or event.
 * <b>username</b>: The user associated with the event.
 * <b>timestamp</b>: Time when the event occurred (epoch milliseconds).
 * <b>seqNumber</b>: Sequence number for ordering messages.
 * <b>txnId</b>: Transaction ID related to the event.
 * <b>networkId</b>: Network identifier in Alfresco context.
 * <b>client</b>: Client information (can be any object).
 * <b>nodeId</b>: Alfresco node identifier.
 * <b>siteId</b>: Site identifier where the node resides.
 * <b>nodeType</b>: Type of the Alfresco node.
 * <b>name</b>: Name of the node.
 * <b>nodeModificationTime</b>: Last modification time of the node (epoch
 * milliseconds).
 * <b>paths</b>: List of paths (can be ArrayList representations) associated
 * with the node.
 * <b>parentNodeIds</b>: List of parent node identifiers.
 * <b>aspects</b>: List of aspects applied to the node.
 * <b>nodeProperties</b>: Map of node properties and their values.
 * 
 *
 *
 * 
 * Getters and setters are provided for all fields.
 *
 *
 * 
 * Annotated with {@code @JsonIgnoreProperties(ignoreUnknown = true)} to allow
 * for flexible JSON deserialization.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoQueMessage {
    private String id;
    private String type;
    private String username;
    private long timestamp;
    private int seqNumber;
    private String txnId;
    private String networkId;
    private Object client;

    // Alfresco specific
    private String nodeId;
    private String siteId;
    private String nodeType;
    private String name;
    private long nodeModificationTime;

    private List<Object> paths; // Can store ArrayList representation
    private List<Object> parentNodeIds;
    private List<Object> aspects;

    private Map<String, Object> nodeProperties;

    // getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getSeqNumber() {
        return seqNumber;
    }

    public void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public Object getClient() {
        return client;
    }

    public void setClient(Object client) {
        this.client = client;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getNodeModificationTime() {
        return nodeModificationTime;
    }

    public void setNodeModificationTime(long nodeModificationTime) {
        this.nodeModificationTime = nodeModificationTime;
    }

    public List<Object> getPaths() {
        return paths;
    }

    public void setPaths(List<Object> paths) {
        this.paths = paths;
    }

    public List<Object> getParentNodeIds() {
        return parentNodeIds;
    }

    public void setParentNodeIds(List<Object> parentNodeIds) {
        this.parentNodeIds = parentNodeIds;
    }

    public List<Object> getAspects() {
        return aspects;
    }

    public void setAspects(List<Object> aspects) {
        this.aspects = aspects;
    }

    public Map<String, Object> getNodeProperties() {
        return nodeProperties;
    }

    public void setNodeProperties(Map<String, Object> nodeProperties) {
        this.nodeProperties = nodeProperties;
    }
}