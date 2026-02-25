package contain.opensource.ils.bs.receiver.classes.Logger;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "tbl_sp_deltalink_repository")
public class IOLogDeltaLink {

    @Id
    @Column(name = "source_id", columnDefinition = "TEXT")
    private String sourceId;

    @Column(name = "token_id", columnDefinition = "TEXT")
    private String tokenId;

    @Column(name = "last_delta_link", columnDefinition = "TEXT")
    private String lastDeltaLink;

    @Column(name = "log_datetime", nullable = false)
    private LocalDateTime logDateTime;

    protected IOLogDeltaLink() {}

    public IOLogDeltaLink(String sourceId, String tokenId, String lastDeltaLink) {
        this.sourceId = sourceId;
        this.tokenId = tokenId;
        this.lastDeltaLink = lastDeltaLink;
        this.logDateTime = LocalDateTime.now();
    }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getLastDeltaLink() { return lastDeltaLink; }
    public void setLastDeltaLink(String lastDeltaLink) { this.lastDeltaLink = lastDeltaLink; }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public LocalDateTime getLogDateTime() { return logDateTime; }
    public void setLogDateTime(LocalDateTime logDateTime) { this.logDateTime = logDateTime; }
}
