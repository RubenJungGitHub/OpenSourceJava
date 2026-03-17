package contain.opensource.ils.bs.receiver.classes.Logger;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import contain.opensource.shared.constants.AlfrescoConstants.eActionPerformed;

@Document(collection = "tbl_iolog")
public class IOLogBallenbakMongo {

    @Id
    private String id;

    @Field("platform_id")
    private String platformId;

    private String path;
    private String marking;

    @Field("classification")
    private String classification;

    private String version;

    @Field("contain_io_uuid")
    private String containIoUuid;

    @Field("io_action")
    private String ioAction;

    @Field("io_source")
    private String ioSource;

    @Field("io_destination")
    private String ioDestination;

    @Field("pki_hash")
    private String pkiHash;

    @Field("io_reference")
    private String ioReference;

    @Field("additional_info")
    private String additionalInfo;

    @Field("log_datetime")
    private LocalDateTime logDateTime;

    @Field("action_performed")
    private eActionPerformed actionPerformed;

    @Field("action_performed_by")
    private String actionPerformedBy;

    // ======= Getters =======
    public String getId() { return id; }
    public String getPlatformId() { return platformId; }
    public String getPath() { return path; }
    public String getMarking() { return marking; }
    public String getClassification() { return classification; }
    public String getVersion() { return version; }
    public String getContainIoUuid() { return containIoUuid; }
    public String getIoAction() { return ioAction; }
    public String getIoSource() { return ioSource; }
    public String getIoDestination() { return ioDestination; }
    public String getPkiHash() { return pkiHash; }
    public String getIoReference() { return ioReference; }
    public String getAdditionalInfo() { return additionalInfo; }
    public LocalDateTime getLogDateTime() { return logDateTime; }
    public eActionPerformed getActionPerformed() { return actionPerformed; }
    public String getActionPerformedBy() { return actionPerformedBy; }

    // ======= Setters =======
    public void setId(String id) { this.id = id; }
    public void setPlatformId(String platformId) { this.platformId = platformId; }
    public void setPath(String path) { this.path = path; }
    public void setMarking(String marking) { this.marking = marking; }
    public void setClassification(String classification) { this.classification = classification; }
    public void setVersion(String version) { this.version = version; }
    public void setContainIoUuid(String containIoUuid) { this.containIoUuid = containIoUuid; }
    public void setIoAction(String ioAction) { this.ioAction = ioAction; }
    public void setIoSource(String ioSource) { this.ioSource = ioSource; }
    public void setIoDestination(String ioDestination) { this.ioDestination = ioDestination; }
    public void setPkiHash(String pkiHash) { this.pkiHash = pkiHash; }
    public void setIoReference(String ioReference) { this.ioReference = ioReference; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    public void setLogDateTime(LocalDateTime logDateTime) { this.logDateTime = logDateTime; }
    public void setActionPerformed(eActionPerformed actionPerformed) { this.actionPerformed = actionPerformed; }
    public void setActionPerformedBy(String actionPerformedBy) { this.actionPerformedBy = actionPerformedBy; }
}