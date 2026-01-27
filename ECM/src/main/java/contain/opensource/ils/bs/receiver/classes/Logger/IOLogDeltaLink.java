package contain.opensource.ils.bs.receiver.classes.Logger;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "tblSPDeltalinkRepository", schema = "dbo")
public class IOLogDeltaLink {

    @Id
    @Column(name = "SourceID")
    private String sourceId;   // lowercase s, camelCase

    @Column(name = "TokenID")
    private String tokenId;    // lowercase t

   @Column(name = "LogDateTime", nullable = false)
    private LocalDateTime logDateTime;
    public IOLogDeltaLink() { }

    public IOLogDeltaLink(String sourceId, String tokenId) {
        this.sourceId = sourceId;
        this.tokenId = tokenId;
        this.logDateTime = LocalDateTime.now();
    }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public LocalDateTime getLogDateTime() { return logDateTime; }
    public void setLogDateTime(LocalDateTime logDateTime) { this.logDateTime = logDateTime; }
}