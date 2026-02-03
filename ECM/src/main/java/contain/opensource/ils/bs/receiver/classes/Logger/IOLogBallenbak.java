package contain.opensource.ils.bs.receiver.classes.Logger;

import java.time.LocalDateTime;

import contain.opensource.ils.bs.receiver.classes.UUIDUtil;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.eActionPerformed;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "tbl_iolog")
public class IOLogBallenbak {

    @Id
    @Column(name = "uuid", nullable = false)
    private String uuid;

    @Column(name = "platform_id", nullable = false)
    private String platformId;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "contain_io_uuid", nullable = false)
    private String containIoUuid;

    @Column(name = "io_action", nullable = false)
    private String ioAction;

    @Column(name = "io_source", columnDefinition = "TEXT", nullable = false)
    private String ioSource;

    @Column(name = "io_destination", columnDefinition = "TEXT", nullable = false)
    private String ioDestination;

    @Column(name = "pki_hash", columnDefinition = "TEXT", nullable = false)
    private String pkiHash;

    @Column(name = "io_reference", columnDefinition = "TEXT", nullable = false)
    private String ioReference;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(name = "log_datetime", nullable = false)
    private LocalDateTime logDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_performed", nullable = false)
    private eActionPerformed actionPerformed;

    @Column(name = "action_performed_by", nullable = false)
    private String actionPerformedBy;

    // Required by JPA
    protected IOLogBallenbak() {}
    
    // Optional convenience constructor
    public IOLogBallenbak(String uuid, String containIOUUID, String PlatformID, String IOpath, String ioAction, String ioSource,
                 String ioDestination, String pkiHash, String ioReference,
                 String additionalInfo, eActionPerformed actionPerformed, String ActionPerformedBy) {
        this.uuid = uuid != null ? uuid : UUIDUtil.getUUIDOverHTTP(); // use passed uuid if not null
        this.containIoUuid = containIOUUID;
        this.platformId = PlatformID;
        this.path = IOpath;
        this.ioAction = ioAction;
        this.ioSource = ioSource;
        this.ioDestination = ioDestination;
        this.pkiHash = pkiHash;
        this.ioReference = ioReference;
        this.additionalInfo = additionalInfo;
        this.actionPerformed = actionPerformed;
        this.actionPerformedBy = ActionPerformedBy;
        this.logDateTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getContainIOUUID() { return containIoUuid; }
    public void setContainIOUUID(String containIoUuid) { this.containIoUuid = containIoUuid; }

    public String getIoAction() { return ioAction; }
    public void setIoAction(String ioAction) { this.ioAction = ioAction; }

    public String getIoSource() { return ioSource; }
    public void setIoSource(String ioSource) { this.ioSource = ioSource; }

    public String getIoDestination() { return ioDestination; }
    public void setIoDestination(String ioDestination) { this.ioDestination = ioDestination; }

    public String  getPkiHash() { return pkiHash; }
    public void setPkiHash(String pkiHash) { this.pkiHash = pkiHash; }

    public String getIoReference() { return ioReference; }
    public void setIoReference(String ioReference) { this.ioReference = ioReference; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public String getPlatformID() { return platformId; }
    public void setPlatformID(String platformId) { this.platformId = platformId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getActionPerformedBy() { return actionPerformedBy; }
    public void setActionPerformedBy(String actionperformedby) { this.actionPerformedBy = actionperformedby; }

    public eActionPerformed getActionPerformed() { return actionPerformed; }
    public void setActionPerformed(eActionPerformed actionPerformed) { this.actionPerformed = actionPerformed; }

    public LocalDateTime getLogDateTime() { return logDateTime; }
    public void setLogDateTime(LocalDateTime logDateTime) { this.logDateTime = logDateTime; }
}
