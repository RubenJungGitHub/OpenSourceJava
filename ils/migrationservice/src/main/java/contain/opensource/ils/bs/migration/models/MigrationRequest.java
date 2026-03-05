package contain.opensource.ils.bs.migration.models;

import java.util.Objects;

import contain.opensource.shared.constants.AlfrescoConstants.ContainPlatforms;

public class MigrationRequest {

    private final String ioId;                 // Unique IO identifier
    private final String fileName;             // File name
    private final ContainPlatforms source;     // Source platform (ALFRESCO, SPO, etc.)
    private final ContainPlatforms destination;// Target platform
    private final byte[] content;              // File content
    private final String uuid;                 // UUID assigned to the IO
    private final String marking;              // Optional marking
    private final String classification;       // Optional classification
    private final String version;              // Optional version

    public MigrationRequest(
            String ioId,
            String fileName,
            ContainPlatforms source,
            ContainPlatforms destination,
            byte[] content,
            String uuid,
            String marking,
            String classification,
            String version) {
        this.ioId = ioId;
        this.fileName = fileName;
        this.source = source;
        this.destination = destination;
        this.content = content;
        this.uuid = uuid;
        this.marking = marking;
        this.classification = classification;
        this.version = version;
    }

    // Getters
    public String getIoId() { return ioId; }
    public String getFileName() { return fileName; }
    public ContainPlatforms getSource() { return source; }
    public ContainPlatforms getDestination() { return destination; }
    public byte[] getContent() { return content; }
    public String getUuid() { return uuid; }
    public String getMarking() { return marking; }
    public String getClassification() { return classification; }
    public String getVersion() { return version; }

    // equals and hashCode for storing in maps or sets (important for locking or deduplication)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MigrationRequest)) return false;
        MigrationRequest that = (MigrationRequest) o;
        return Objects.equals(ioId, that.ioId) &&
               source == that.source &&
               destination == that.destination;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ioId, source, destination);
    }

    @Override
    public String toString() {
        return "MigrationRequest{" +
                "ioId='" + ioId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", source=" + source +
                ", destination=" + destination +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}