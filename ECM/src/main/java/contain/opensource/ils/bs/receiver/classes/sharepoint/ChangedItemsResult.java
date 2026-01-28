package contain.opensource.ils.bs.receiver.classes.sharepoint;

import java.util.List;

import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;

public class ChangedItemsResult {
    public List<String> changedItems;
    public String newDeltaLink;
    public AlfrescoConstants.eItemtype itemType;

    public ChangedItemsResult(List<String> changedItems, String newDeltaLink, AlfrescoConstants.eItemtype itemType) {
        this.changedItems = changedItems;
        this.newDeltaLink = newDeltaLink;
        this.itemType = itemType;
    }
}