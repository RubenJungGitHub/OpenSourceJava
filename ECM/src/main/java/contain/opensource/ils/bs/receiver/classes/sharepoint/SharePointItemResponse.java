package contain.opensource.ils.bs.receiver.classes.sharepoint;

import java.time.OffsetDateTime;
import java.util.Map;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true) // top-level
public class SharePointItemResponse {

    @JsonProperty("id")
    public String id;

   
    @JsonProperty("title")           // <-- map directly
    public String title;

    @JsonProperty("mimetype")           // <-- map directly
    public String mimetype;

   
    @JsonProperty("filename")           // <-- map directly
    public String filename;

    @JsonProperty("containIOdescription")     // <-- map directly
    public String description;

    @JsonProperty("file")     // <-- map directly
    public byte[] file;

    @JsonProperty("fields")
    public Map<String, Object> fields;

    public boolean HasUUID = false;
    public boolean MustMove = false;
    public String UUID;
    public AlfrescoConstants.ContainPlatforms MoveTo;

    @JsonProperty("contentType")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public ContentTypeInfo contentType;

    @JsonProperty("createdBy")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public IdentitySet createdBy;

    @JsonProperty("createdDateTime")
    public OffsetDateTime createdDateTime;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentTypeInfo {
        @JsonProperty("id")
        public String id;

        @JsonProperty("name")
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentitySet {
        @JsonProperty("user")
        public IdentityUser user;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IdentityUser {
            @JsonProperty("displayName")
            public String displayName;

            @JsonProperty("id")
            public String id;
        }
    }
}
