package contain.opensource.ils.bs.receiver.classes.sharepoint;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import contain.opensource.ils.bs.receiver.classes.Binding.SecuredDocument;
import contain.opensource.shared.classes.IOObjectProperties;

/**
 * Represents a SharePoint item response from the SharePoint API.
 * 
 * This class extends IOObjectProperties and is designed to deserialize JSON
 * responses
 * from SharePoint into Java objects. It uses Jackson annotations for JSON
 * mapping and
 * ignores unknown properties to handle API changes gracefully.
 * 
 * The class includes the following main fields:
 * - title: The title of the SharePoint item
 * - mimetype: The MIME type of the SharePoint item
 * - description: A description of the SharePoint item
 * - filecontent: The binary content of the file
 * - fields: A map of additional metadata fields
 * - contentType: Information about the content type
 * - createdBy: Identity information of the creator
 * - createdDateTime: The creation timestamp
 * 
 * Nested classes:
 * - ContentTypeInfo: Contains metadata about the item's content type (id and
 * name)
 * - IdentitySet: Contains creator identity information with nested IdentityUser
 * class
 * 
 * The class provides a ToSecuredDocument() method to convert a SharePoint
 * response
 * into a SecuredDocument object, mapping the relevant fields and converting the
 * creation timestamp to an Instant for internal use.
 * 
 * @see IOObjectProperties
 * @see SecuredDocument
 * @see ContentTypeInfo
 * @see IdentitySet
 */
@JsonIgnoreProperties(ignoreUnknown = true) // top-level
public class SharePointItemResponse extends IOObjectProperties {



    @JsonProperty("title") // <-- map directly
    public String title;

    /**
     * The MIME type of the SharePoint item.
     * This field is mapped directly from the JSON response using the property name
     * "mimetype".
     */
    @JsonProperty("mimetype") // <-- map directly
    public String mimetype;

    public String Path;

    @JsonProperty("containIOdescription") // <-- map directly
    public String description;

    @JsonProperty("file") // <-- map directly
    public byte[] filecontent;

    @JsonProperty("fields")
    public Map<String, Object> fields;

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
        SecuredDocument secdoc = new SecuredDocument(
                this.filecontent,
                this.id,
                this.id,
                this.UUID,
                this.title,
                this.filename,
                this.description,
                this.mimetype,
                createdAtInstant,
                null, // to be iplemented modifiedAtInstant
                this.marking,
                this.classification,
                this.version);
        return secdoc;
    }
}
