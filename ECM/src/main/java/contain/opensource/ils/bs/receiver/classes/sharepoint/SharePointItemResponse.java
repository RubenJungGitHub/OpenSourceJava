package contain.opensource.ils.bs.receiver.classes.sharepoint;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import  contain.opensource.ils.bs.receiver.classes.Binding.SecuredDocument;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;

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

    @JsonProperty("version")           // <-- map directly
    public String version;

     @JsonProperty("marking")           // <-- map directly
    public String marking;

     @JsonProperty("label")           // <-- map directly
    public String label;

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

    public SecuredDocument ToSecuredDocument() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    Instant createdAtInstant = null;
    if (this.createdDateTime != null) {
        createdAtInstant = this.createdDateTime.toInstant();
    }
        return new SecuredDocument(
                this.file,
                "objectid",
                this.UUID,
                this.title,
                this.filename,
                this.description,
                this.mimetype,
                createdAtInstant,
                null,  // to be iplemented modifiedAtInstant
                this.marking,
                this.label,
                this.version
        );
    }
}
