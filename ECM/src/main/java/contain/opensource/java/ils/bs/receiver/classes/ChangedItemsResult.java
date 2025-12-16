package contain.opensource.java.ils.bs.receiver.classes;
import contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.eItemtype;

import java.util.*;
import java.util.List;
import java.util.ArrayList;
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
