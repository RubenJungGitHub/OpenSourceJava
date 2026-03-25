package contain.opensource.shared.classes;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SharepointQueMessage {

    private String key;
    private List<Item> items;
    private String deltaLink;

    // getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public String getDeltaLink() { return deltaLink; }
    public void setDeltaLink(String deltaLink) { this.deltaLink = deltaLink; }

    // -------------------------------
    // Nested classes
    // -------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String id;
        private ParentReference parentReference;
        private Deleted deleted;
        private Map<String, Object> fields;
        private CreatedBy createdBy;
        private String webUrl;
        private ContentType contentType;
        private String createdDateTime;
        private String lastModifiedDateTime;

        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public ParentReference getParentReference() { return parentReference; }
        public void setParentReference(ParentReference parentReference) { this.parentReference = parentReference; }

        public Deleted getDeleted() { return deleted; }
        public void setDeleted(Deleted deleted) { this.deleted = deleted; }

        public Map<String, Object> getFields() { return fields; }
        public void setFields(Map<String, Object> fields) { this.fields = fields; }

        public CreatedBy getCreatedBy() { return createdBy; }
        public void setCreatedBy(CreatedBy createdBy) { this.createdBy = createdBy; }

        public String getWebUrl() { return webUrl; }
        public void setWebUrl(String webUrl) { this.webUrl = webUrl; }

        public ContentType getContentType() { return contentType; }
        public void setContentType(ContentType contentType) { this.contentType = contentType; }

        public String getCreatedDateTime() { return createdDateTime; }
        public void setCreatedDateTime(String createdDateTime) { this.createdDateTime = createdDateTime; }

        public String getLastModifiedDateTime() { return lastModifiedDateTime; }
        public void setLastModifiedDateTime(String lastModifiedDateTime) { this.lastModifiedDateTime = lastModifiedDateTime; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParentReference {
        private String siteId;
        private String id;
        private String path;

        public String getSiteId() { return siteId; }
        public void setSiteId(String siteId) { this.siteId = siteId; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Deleted {
        private String state;

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreatedBy {
        private User user;

        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class User {
            private String displayName;
            private String email;
            private String id;

            public String getDisplayName() { return displayName; }
            public void setDisplayName(String displayName) { this.displayName = displayName; }

            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentType {
        private String id;
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
