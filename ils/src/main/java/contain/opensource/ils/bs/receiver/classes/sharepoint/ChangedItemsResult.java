package contain.opensource.ils.bs.receiver.classes.sharepoint;

import java.util.List;

import contain.opensource.shared.constants.AlfrescoConstants;

/**
 * Represents the result of a query for changed items from a SharePoint source.
 * This class encapsulates the list of items that have been modified, along with
 * pagination information and the item type being tracked.
 * 
 * @author ECM System
 * @version 1.0
 */
public class ChangedItemsResult {

    /**
     * List of item identifiers that have been changed or modified.
     */
    public List<String> changedItems;

    /**
     * Delta link token for retrieving subsequent changes in a paginated manner.
     * Used to avoid re-fetching previously retrieved items in subsequent queries.
     */
    public String newDeltaLink;

    /**
     * The type of item being tracked (e.g., document, folder, etc.).
     * Defines the category of items in the changedItems list.
     */
    public AlfrescoConstants.eItemtype itemType;

    /**
     * Constructs a ChangedItemsResult with the specified changed items, delta link,
     * and item type.
     * 
     * @param changedItems the list of identifiers for items that have changed
     * @param newDeltaLink the delta link token for retrieving subsequent changes
     * @param itemType     the type of items being tracked
     */
    public ChangedItemsResult(List<String> changedItems, String newDeltaLink, AlfrescoConstants.eItemtype itemType) {
        this.changedItems = changedItems;
        this.newDeltaLink = newDeltaLink;
        this.itemType = itemType;
    }
}