package contain.opensource.ils.bs.receiver.classes;

//import com.fasterxml.jackson.annotation.JsonGetter;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.annotation.JsonSetter;

import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeResponse;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.shared.constants.AlfrescoConstants;

//Generic IO object Class for move between environments 
//======================================================
public class RelocateInformationObject  extends IOObjectProperies  {

    public RelocateInformationObject() {
        // needed for Jackson
    }

    // Constructor for Alfresco
    public RelocateInformationObject(AlfrescoNodeResponse Anode,  String hash, AlfrescoConstants.ContainPlatforms containplatformfrom,
            AlfrescoConstants.ContainPlatforms containPlatFormTo) {
        // Map to this object
        this.UUID = Anode.UUID;
        this.HASH = hash;
        this.id = Anode.entry.id;
        this.Title = Anode.Title;
        this.content = Anode.content;
        this.Description = Anode.Description;
        this.filename = Anode.entry.filename;
        this.containplatformfrom = containplatformfrom;
        this.containplatformto = containPlatFormTo;
        this.marking  = Anode.marking;
        this.classification = Anode.classification;
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
        this.containplatformfrom = AlfrescoConstants.ContainPlatforms.SPO;
        this.containplatformto = SPItem.MoveTo;
        this.marking  = SPItem.marking;
        this.classification = SPItem.classification;
        this.version = SPItem.version;
    }
}
