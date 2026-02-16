package contain.opensource.ils.bs.receiver.classes.Binding;
import contain.opensource.ils.bs.receiver.classes.IOObjectProperies;
import java.time.Instant;

/**
 * A secured document class that encapsulates document metadata and content with timestamp information.
 * 
 * This class extends {@link IOObjectProperies} and represents a document entity with security properties,
 * including creation and modification timestamps. It maintains immutability for temporal attributes through
 * final fields.
 * 
 * @author [Author Name]
 * @version 1.0
 * @since [Date]
 */
public final class SecuredDocument extends IOObjectProperies {

    private final Instant created;
    private final Instant lastModified;

    public SecuredDocument(
            byte[] content,
            String objectId,
            String id,
            String UUID,
            String Title,
            String filename,
            String description,
            String mimeType,
            Instant created,
            Instant lastModified,
            String marking,
            String classification,
            String version
    ) {
        this.content = content;
        this.objectId = objectId;
        this.id = id;
        this.UUID = UUID;
        this.Title = Title;
        this.filename = filename;
        this.Description = description;
        this.mimeType = mimeType;
        this.created = created;
        this.lastModified = lastModified;
        this.marking = marking;
        this.classification = classification;
        this.version = version;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getLastModified() {
        return lastModified;
    }
}
