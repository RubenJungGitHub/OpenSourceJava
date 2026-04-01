package contain.opensource.ils.bs.receiver.classes;

import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeResponse;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.shared.classes.IOObjectProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

//Generic IO object Class for move between environments 
//======================================================
public class RelocateInformationObject extends IOObjectProperties {


    public String containerto;
    public AlfrescoConstants.ContainPlatforms platformto;

    public RelocateInformationObject() {
        // needed for Jackson

    }

    // Constructor for Alfresco
    public RelocateInformationObject(AlfrescoNodeResponse Anode)
    {
        // Map to this object
        this.UUID = Anode.UUID;
        this.HASH = "1234567890";
        this.id = Anode.entry.id;
        this.Title = Anode.Title;
        this.content = Anode.content;
        this.Description = Anode.Description;
        this.filename = Anode.entry.filename;
        this.marking = Anode.marking;
        this.classification = Anode.classification;
        this.platformfrom = AlfrescoConstants.ContainPlatforms.ALFRESCO;
        this.version = Anode.version;
    }

    // Constructor for SP
    public RelocateInformationObject(SharePointItemResponse SPItem) {
        // Map to this object
        this.UUID = SPItem.UUID;
        this.id = SPItem.id;
        this.Title = SPItem.title;
        this.filename = SPItem.filename;
        this.Description = SPItem.description;
        this.mimeType = SPItem.mimetype;
        this.content = SPItem.filecontent;
        this.marking = SPItem.marking;
        this.classification = SPItem.classification;
        this.version = SPItem.version;
        this.platformfrom = AlfrescoConstants.ContainPlatforms.SPO;
     }

    public void setcontainerto(String containerto) {
        this.containerto = containerto;
    }

    public String getcontainerto() {
        return this.containerto;
    }


    public void setplatformto(AlfrescoConstants.ContainPlatforms platformto) {
        this.platformto = platformto;
    }

    public AlfrescoConstants.ContainPlatforms  getplatformto() {
        return this.platformto;
    }

}
