package contain.opensource.ils.bs.receiver.classes.Binding;

import contain.opensource.ils.bs.receiver.classes.IOObjectProperies;
import java.time.Instant;
import java.util.List;

public final class SecuredDocument extends IOObjectProperies {

    // Content
    // private final byte[] content;

    // Identity


    // File metadata
    // private final String filename;
    // private final String description;
    // private final String mimeType;
    private final Instant created;
    private final Instant lastModified;

    // Security metadata
    // private final List<String> classifications; // canonical codes
    // private final List<String> labels;
    // private final List<String> markings;

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
            String label,
            String version
    // List<String> classifications,
    // List<String> labels,
    // List<String> markings
    ) {
        this.content = content;
        this.objectId = objectId;
        this.id= id;
        this.UUID = UUID;
        this.Title = Title;
        this.filename = filename;
        this.Description = description;
        this.mimeType = mimeType;
        this.created = created;
        this.lastModified = lastModified;
        this.marking = marking;
        this.label = label;
        this.version = version;
        // this.classifications = List.copyOf(classifications);
        // this.labels = List.copyOf(labels);
        // this.markings = List.copyOf(markings);
    }

    // getters only. VOID Because parent class has them

    /*
     * 
     * public byte[] getContent() {
     * return content;
     * }
     * 
     * public String getObjectId() {
     * return objectId;
     * }
     * 
     * public String getFilename() {
     * return filename;
     * }
     * 
     * public String getMimeType() {
     * return mimeType;
     * }
     * 
     * 
     * public String getDescription() {
     * return Description;
     * }
     * 
     */

    public Instant getCreated() {
        return created;
    }

    public Instant getLastModified() {
        return lastModified;
    }
    /*
     * //public List<String> getClassifications() {
     * // return classifications;
     * //}
     * 
     * //public List<String> getLabels() {
     * // return labels;
     * //}
     * 
     * //public List<String> getMarkings() {
     * // return markings;
     * //}
     * 
     */
}
