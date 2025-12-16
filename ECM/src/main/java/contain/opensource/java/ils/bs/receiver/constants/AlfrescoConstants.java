package contain.opensource.java.ils.bs.receiver.constants;

import java.util.Collections;
import java.util.Set;

public class AlfrescoConstants {

    public final static String RED = "\u001B[31m";
    public final static String RESET = "\u001B[0m";
    public final static String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String DeltaLinkFile = "C:\\Temp\\storeddeltalinks.csv";
    public static final String clientId = "f590b477-5bd7-47d6-8bda-36f77fa10afd";
    public static final String clientSecret = "pE.8Q~ZQRGngJ1YliTP4EDC5bejaEl72LlBAzb50";
    // public static final String GraphScopes =
    // "https://graph.microsoft.com/.default";
    public static final Set<String> GraphScopes = Collections.singleton("https://graph.microsoft.com/.default");
    public static final String tenantId = "9a1b5f77-1f1a-40ac-b1a1-38617300f02a";

    public static String username = "admin";
    public static String password = "admin";

    public enum NodeTypeFields {
        UUID,
        Title
    }

    public enum eItemtype {
        Graph,
        SharePoint
    }

    public enum ContainPlatforms {
        ALFRESCO,
        SPO
    }

    public enum NodeType {
        NODEADDED,
        NODEUPDATED, // unsure if this is correct
        NODEREMOVED,
        CONTENTPUT;

        public static NodeType fromString(String value) {
            if (value == null)
                return null;

            try {
                return NodeType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null; // not a valid enum
            }
        }
    }
}
