package contain.opensource.ils.bs.receiver.classes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeResponse;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;

//Generic IO object Class for move between environments 
//======================================================
public class RelocateInformationObject {
    @JsonProperty("title")
    private String title;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("fileName")
    private String filename;

   @JsonProperty("description")
    private String description;


    @JsonProperty("id")
    private String id;

    @JsonProperty("content")
    // @JsonDeserialize(using = Base64Deserializer.class)
    private byte[] content;

    @JsonProperty("platformFrom")
    private AlfrescoConstants.ContainPlatforms containplatformfrom;

    @JsonProperty("platformTo")
    private AlfrescoConstants.ContainPlatforms containplatformto;

    public RelocateInformationObject() {
        // needed for Jackson
    }

    // Constructor for Alfresco
    public RelocateInformationObject(AlfrescoNodeResponse Anode, AlfrescoConstants.ContainPlatforms containPlatformfrom,
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
    public RelocateInformationObject(SharePointItemResponse SPItem) {
      // Map to this object
        //this.content = SPItem.cont;
        this.uuid = SPItem.UUID;
        this.id = SPItem.id;
        this.title = SPItem.title;
        this.filename = SPItem.filename;
        this.description  = SPItem.description;
        //this.filename = Anode.entry.name;
        this.containplatformfrom = AlfrescoConstants.ContainPlatforms.SPO;
        this.containplatformto = SPItem.MoveTo;

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
