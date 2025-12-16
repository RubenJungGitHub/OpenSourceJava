package contain.opensource.java.ils.bs.receiver.classes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonGetter;
import contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Base64;

//Generic IO object Class for move between environments 
//======================================================
public class InformationObject {
    @JsonProperty("title")
    private String title;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("fileName")
    private String filename;

    @JsonProperty("id")
    private String id;

    @JsonProperty("content")
    //@JsonDeserialize(using = Base64Deserializer.class)
    private byte[] content;

    @JsonProperty("platformFrom")
    private AlfrescoConstants.ContainPlatforms containplatformfrom;

    @JsonProperty("platformTo")
    private AlfrescoConstants.ContainPlatforms containplatformto;

    public InformationObject() {
    // needed for Jackson
    }

    // Constructor for Alfresco
    public InformationObject(AlfrescoNodeResponse Anode, AlfrescoConstants.ContainPlatforms containPlatformfrom,
            AlfrescoConstants.ContainPlatforms containPlatFormTo) {
        // Map to this object
        this.content = Anode.file;
        this.uuid = Anode.UUID;
        this.id = Anode.entry.id;
        this.title = Anode.Title;
        this.filename = Anode.entry.name;
        this.containplatformfrom = containPlatformfrom;
        this.containplatformto = containPlatFormTo;
    }

    // Constructor for SP
    public InformationObject(String SPItem) {
    }

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

    @JsonGetter("content")
    public byte[] getContent() {
        return content;
    }

    @JsonSetter("content")
    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String UUID) {
        this.uuid = UUID;
    }

    public AlfrescoConstants.ContainPlatforms getPlatformFrom() {
        return containplatformfrom;
    }

    public void setPlatformFrom(AlfrescoConstants.ContainPlatforms pf) {
        this.containplatformfrom = pf;
    }

    public AlfrescoConstants.ContainPlatforms getPlatformTo() {
        return containplatformto;
    }

    public void setPlatformTo(AlfrescoConstants.ContainPlatforms pt) {
        this.containplatformto = pt;
    }
}
