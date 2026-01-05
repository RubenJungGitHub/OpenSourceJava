package contain.opensource.ils.bs.receiver.classes.alfresco;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AlfrescoNodeResponse {
    public boolean HasUUID = false;
    public boolean MustMove = false;
    public String  UUID;
    public  String MoveTo;
    public String  Content;
    public Entry entry;
    public byte[] file;
    public String Title;
    public String Description;


    public static class Entry {
        public boolean isFile;
        public CreatedByUser createdByUser;
        public String modifiedAt;
        public String nodeType;
        public Content content;
        public String parentId;
        public List<String> aspectNames;
        public String createdAt;
        public boolean isFolder;
        public ModifiedByUser modifiedByUser;
        public String name;
        public String id;
        public Properties properties;
    }

    public static class CreatedByUser {
        public String id;
        public String displayName;
    }

    public static class ModifiedByUser {
        public String id;
        public String displayName;
    }

    public static class Content {
        public String mimeType;
        public String mimeTypeName;
        public long sizeInBytes;
        public String encoding;
    }

    public static class Properties {

        @JsonProperty("cm:versionType")
        public String versionType;

        @JsonProperty("cm:versionLabel")
        public String versionLabel;

        @JsonProperty("cm:lastThumbnailModification")
        public List<String> lastThumbnailModification;

    // Store any unknown or custom properties (like RJTM:UUID)
    public Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void setOtherProperty(String key, Object value) {
        otherProperties.put(key, value);
    }
        public String actionContext;
    }
}