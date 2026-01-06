
package contain.opensource.ils.bs.receiver.classes.Logger;

import java.time.LocalDateTime;

import contain.opensource.ils.bs.receiver.classes.UUIDUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "tblIOLog", schema = "dbo")
public class IOLogBallenbak {

    @Id
    @Column(name = "UUID", length = 36, nullable = false)
    private String uuid;

    @Column(name = "containIOUUID", length = 36, nullable = false)
    private String containIOUUID;

    @Column(name = "IOAction", columnDefinition = "varchar(max)", nullable = false)
    private String ioAction;

    @Column(name = "IOSource", length = 50, nullable = false)
    private String ioSource;

    @Column(name = "IODestination", length = 50, nullable = false)
    private String ioDestination;

    @Column(name = "PKIHash", length = 50, nullable = false)
    private String pkiHash;

    @Column(name = "IOreference", length = 50, nullable = false)
    private String ioReference;

    @Column(name = "AddiionalInfo", columnDefinition = "varchar(max)")
    private String additionalInfo;

    @Column(name = "LogDateTime", nullable = false)
    private LocalDateTime logDateTime;

    // Default constructor (required by JPA)
    public IOLogBallenbak() { }

    // Optional convenience constructor
    public IOLogBallenbak(String uuid, String containIOUUID, String ioAction, String ioSource,
                 String ioDestination, String pkiHash, String ioReference,
                 String additionalInfo, LocalDateTime logDateTime) {
        //UUID uuid = UUID.randomUUID();
        String guid = UUIDUtil.getUUID();
        System.out.println("UUID from REST API: " + guid);
        this.uuid = uuid.toString();
        this.containIOUUID = containIOUUID;
        this.ioAction = ioAction;
        this.ioSource = ioSource;
        this.ioDestination = ioDestination;
        this.pkiHash = pkiHash;
        this.ioReference = ioReference;
        this.additionalInfo = additionalInfo;
        this.logDateTime = logDateTime;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getcontainIOUUID() { return containIOUUID; }
    public void setcontainIOUUID(String containIOUUID) { this.containIOUUID = containIOUUID; }

    public String getIoAction() { return ioAction; }
    public void setIoAction(String ioAction) { this.ioAction = ioAction; }

    public String getIoSource() { return ioSource; }
    public void setIoSource(String ioSource) { this.ioSource = ioSource; }

    public String getIoDestination() { return ioDestination; }
    public void setIoDestination(String ioDestination) { this.ioDestination = ioDestination; }

    public String getPkiHash() { return pkiHash; }
    public void setPkiHash(String pkiHash) { this.pkiHash = pkiHash; }

    public String getIoReference() { return ioReference; }
    public void setIoReference(String ioReference) { this.ioReference = ioReference; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public LocalDateTime getLogDateTime() { return logDateTime; }
    public void setLogDateTime(LocalDateTime logDateTime) { this.logDateTime = logDateTime; }
}
