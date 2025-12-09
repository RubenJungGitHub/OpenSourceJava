package contain.opensource.java.helloworld.classes;

public class InformationObject {
    private String Title;
    private String UUID;
    private String id;
    private String Content;
        // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return Content; }
    public void  setContent (String content) { this.Content = content; }
    
       public String getTitle() { return Title; }
    public void  setTitle (String title) { this.Title  = title; }
    
    public String getUUID() { return UUID; }
    public void  setUUID (String UUID) { this.UUID = UUID; }
    
}
