package contain.opensource.ils.bs.receiver.classes;

//import com.fasterxml.jackson.annotation.JsonGetter;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.annotation.JsonSetter;

import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeResponse;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
//import contain.opensource.ils.bs.receiver.classes.IOObjectProperies;

//Generic IO object Class for move between environments 
//======================================================
public class RelocateInformationObject  extends IOObjectProperies  {
    //@JsonProperty("title")
    //private String title;

    //@JsonProperty("uuid")
    //private String uuid;

    //@JsonProperty("HASH")
    //private String HASH;


    //@JsonProperty("fileName")
    //private String filename;

    //@JsonProperty("description")
   // private String description;

    //@JsonProperty("mimetype")
    //private String mimetype;

    //@JsonProperty("id")
    //private String id;

    //@JsonProperty("content")
    // @JsonDeserialize(using = Base64Deserializer.class)
    //private byte[] content;

    //@JsonProperty("platfrom")
    //private AlfrescoConstants.ContainPlatforms containplatformfrom;

    //@JsonProperty("platformTo")
    //private AlfrescoConstants.ContainPlatforms containplatformto;

    public RelocateInformationObject() {
        // needed for Jackson
    }

    // Constructor for Alfresco
    public RelocateInformationObject(AlfrescoNodeResponse Anode,  String hash, AlfrescoConstants.ContainPlatforms containplatformfrom,
            AlfrescoConstants.ContainPlatforms containPlatFormTo) {
        // Map to this object
        this.filename = Anode.filename;
        this.UUID = Anode.UUID;
        this.HASH = hash;
        this.id = Anode.entry.id;
        this.Title = Anode.Title;
        this.Description = Anode.Description;
        this.filename = Anode.entry.filename;
        this.containplatformfrom = containplatformfrom;
        this.containplatformto = containPlatFormTo;
    }

    // Constructor for SP
    public RelocateInformationObject(SharePointItemResponse SPItem) {
        // Map to this object
        // this.content = SPItem.cont;
        this.UUID = SPItem.UUID;
        this.id = SPItem.id;
        this.Title = SPItem.title;
        this.filename = SPItem.filename;
        this.Description = SPItem.description;
        this.mimeType = SPItem.mimetype;
        this.content = SPItem.file;
        this.containplatformfrom = AlfrescoConstants.ContainPlatforms.SPO;
        this.containplatformto = SPItem.MoveTo;

    }

    /*
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
        return mimetype;
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
    this.UUID = UUID.replace("\"", "").trim();
        return UUID;
    }

    public void setUuid(String UUID) {
        this.UUID = UUID.replace("\"", "").trim();
    }

        public String getHash() {
            return this.HASH;
    }

    public void setHash(String hash) {
        this.HASH  =hash;
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
    */
}
