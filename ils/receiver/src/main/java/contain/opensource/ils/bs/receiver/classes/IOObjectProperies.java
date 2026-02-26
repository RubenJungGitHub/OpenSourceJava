package contain.opensource.ils.bs.receiver.classes;

//import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import contain.opensource.shared.constants.AlfrescoConstants;

public abstract class IOObjectProperies {

    public boolean HasUUID = false;
    public boolean MustMove = false;
    public String UUID;
    public AlfrescoConstants.ContainPlatforms MoveTo;

    @JsonProperty("name")
    public String filename;

    @JsonProperty("content")
    public byte[] content;

    @JsonProperty("Title")
    public String Title;

    @JsonProperty("HASH")
    public String HASH;

    @JsonProperty("Description")
    public String Description;

    @JsonProperty("version")
    public String version;

    @JsonProperty("marking")
    public String marking;

    @JsonProperty("classification")
    public String classification;

    @JsonProperty("mimeType")
    public String mimeType;

    @JsonProperty("id")
    public String id;

    public String objectId;

    @JsonProperty("containplatformfrom")
    public AlfrescoConstants.ContainPlatforms containplatformfrom;

    @JsonProperty("containplatformto")
    public AlfrescoConstants.ContainPlatforms containplatformto;

    // Public getters/setters are void as long as members are public

    // getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return filename;
    }

    public void setFileName(String fileName) {
        this.filename = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    @JsonGetter("content")
    public byte[] getContent() {
        return content;
    }

    @JsonSetter("content")
    public void setContent(byte[] content) {
        this.content = content;
    }

    @JsonGetter("description")
    public String getDescription() {
        return Description;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        this.Title = title;
    }

    public String getUuid() {
        String uuid = (UUID == null ? null : UUID.replace("\"", "").trim());
        return uuid.replaceAll("^\"|\"$", "");
    }

    public void setUuid(String UUID) {
        this.UUID = UUID.replace("\"", "").trim();
    }

    public String getHash() {
        return this.HASH;
    }

    public void setHash(String hash) {
        this.HASH = hash;
    }

    public String getMarking() {
        return this.marking;
    }

    public void setMarking(String marking) {
        this.marking = marking;
    }

    public String getclassification() {
        return this.classification;
    }

    public void setclassification(String classification) {
        this.classification = classification;
    }

    public String getVersion() {
        return this.version;
    }

    public void setversion(String version) {
        this.version = version;
    }

    public AlfrescoConstants.ContainPlatforms getPlatfrom() {
        return containplatformfrom;
    }

    public void setPlatfrom(AlfrescoConstants.ContainPlatforms pf) {
        this.containplatformfrom = pf;
    }

    public AlfrescoConstants.ContainPlatforms getPlatformTo() {
        return containplatformto;
    }

    public void setPlatformTo(AlfrescoConstants.ContainPlatforms pt) {
        this.containplatformto = pt;
    }
}
