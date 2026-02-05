package contain.opensource.ils.bs.receiver.classes.alfresco;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import contain.opensource.ils.bs.receiver.classes.Binding.SecuredDocument;
import contain.opensource.ils.bs.receiver.classes.IOObjectProperies;

/**
 * Represents the response from Alfresco for a node (file or folder).
 * Contains details about the node, its properties, content, and metadata.
 *
 * 
 * The main entry is represented by the {@link Entry} class, which includes
 * information such as node type, creation/modification details, content info,
 * and custom properties.
 * 
 *
 * 
 * The {@link Properties} class is designed to handle both standard and custom
 * Alfresco properties, including dynamic properties via {@link JsonAnySetter}.
 * 
 *
 * 
 * The {@code ToSecuredDocument()} method converts the Alfresco node response
 * into a {@link SecuredDocument} object, mapping relevant fields and parsing
 * date/time information.
 * 
 *
 * pul>
 * {@link Entry} - Main node details
 * {@link CreatedByUser} - Creator information
 * {@link ModifiedByUser} - Modifier information
 * {@link Content} - Content metadata (MIME type, size, encoding)
 * {@link Properties} - Node properties, including custom ones
 *
 * 
 * b>Note:</b> This class extends {@code IOObjectProperies} (possible typo,
 * should be "IOObjectProperties").
 */
public class AlfrescoNodeResponse extends IOObjectProperies {
    public Entry entry;

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
        @JsonProperty("name")
        public String filename; // <- map JSON "name" here
        public String id;
        public Properties properties;
        public String version;
        public String marking;
        public String label;
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

    public SecuredDocument ToSecuredDocument() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        Instant createdAtInstant = OffsetDateTime.parse(this.entry.createdAt, formatter).toInstant();
        Instant modifiedAtInstant = OffsetDateTime.parse(this.entry.modifiedAt, formatter).toInstant();
        SecuredDocument secdoc = new SecuredDocument(
                this.content,
                this.entry.id,
                this.entry.id,
                this.UUID,
                this.Title,
                this.entry.filename,
                this.Description,
                this.entry.content.mimeType,
                createdAtInstant,
                modifiedAtInstant,
                this.marking,
                this.label,
                this.version);
        return secdoc;
    }

}