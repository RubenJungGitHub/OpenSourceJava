package contain.opensource.ils.bs.receiver.classes.sharepoint;

/**
 * SharePointDriveInfo represents metadata information for a SharePoint drive.
 * 
 * This class encapsulates details about a SharePoint drive including tenant
 * information,
 * site and web identifiers, drive characteristics, and associated list
 * information.
 * It serves as a data transfer object (DTO) for SharePoint drive configuration
 * and metadata.
 * 
 * 
 * Attributes:
 *
 * {@code tenantID} - The unique identifier of the Microsoft 365 tenant
 * {@code siteUrl} - The URL of the SharePoint site
 * {@code driveId} - The unique identifier of the drive
 * {@code driveName} - The display name of the drive
 * {@code driveType} - The type of the drive (e.g., "personal",
 * "documentLibrary")
 * {@code siteId} - The unique identifier of the SharePoint site
 * {@code webId} - The unique identifier of the SharePoint web
 * {@code listId} - The unique identifier of the associated SharePoint
 * list
 * {@code listItemId} - The unique identifier of the list item
 * {@code listName} - The display name of the SharePoint list
 *
 *
 * 
 * @author [Author Name]
 * @version 1.0
 */
public class SharePointDriveInfo {

    private String tenantID;
    private String siteUrl;
    private String driveId;
    private String driveName;
    private String driveType;
    private String siteId;
    private String webId;
    private String listId;
    private String listItemId;
    private String listName;

    // Getters and Setters
    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getDriveName() {
        return driveName;
    }

    public void setDriveName(String driveName) {
        this.driveName = driveName;
    }

    public String getDriveType() {
        return driveType;
    }

    public void setDriveType(String driveType) {
        this.driveType = driveType;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getWebId() {
        return webId;
    }

    public void setWebId(String webId) {
        this.webId = webId;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getListItemId() {
        return listItemId;
    }

    public void setListItemId(String listItemId) {
        this.listItemId = listItemId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }
}
