package contain.opensource.shared.constants;

import java.util.Collections;
import java.util.Set;

public class AlfrescoConstants {
    // Use your own tenant, clientId, clientSecret
    // ====================================================================
    // ====================================================================
    // This obviously should be stored secure somewhere in the future!!!!
    // ====================================================================
    // ====================================================================

    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String RESET = "\u001B[0m";

    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_MAGENTA = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_WHITE = "\u001B[47m";

    public static final String BG_BRIGHT_BLACK = "\u001B[100m";
    public static final String BG_BRIGHT_RED = "\u001B[101m";
    public static final String BG_BRIGHT_GREEN = "\u001B[102m";
    public static final String BG_BRIGHT_YELLOW = "\u001B[103m";
    public static final String BG_BRIGHT_BLUE = "\u001B[104m";
    public static final String BG_BRIGHT_MAGENTA = "\u001B[105m";
    public static final String BG_BRIGHT_CYAN = "\u001B[106m";
    public static final String BG_BRIGHT_WHITE = "\u001B[107m";

    public static final String clientId = "f590b477-5bd7-47d6-8bda-36f77fa10afd";
    public static final String clientSecret = "pE.8Q~ZQRGngJ1YliTP4EDC5bejaEl72LlBAzb50";

    public static final String alfrescoDemoSiteName = "ontobind";
    public static final String alfrescoDemoSiteDropLib = "documentLibrary";
    public static final String alfrescoBaseUrl = "http://localhost:8080";
    public static final String p12PrivateKeyFile = "C:\\ContainOpenSource\\Java\\OpenSourceJava\\ils\\src\\main\\resources\\Containselfsigned_cert.p12";
    public static final String p12PrivateKeyFilePassword = "changeitsosecure";
    public static final String p12PrivateKeyFileAlias = "mykey";

    // public static final String GraphScopes =
    // "https://graph.microsoft.com/.default";
    public static final Set<String> GraphScopes = Collections.singleton("https://graph.microsoft.com/.default");
    public static final String tenantId = "9a1b5f77-1f1a-40ac-b1a1-38617300f02a";

    public static String username = "admin";
    public static String password = "admin";

    

    public enum PlatformPrefix {
        Alfresco,
        SPO
    }

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

    public enum eActionPerformed {
        IOCOPIED,
        ASSIGNUUID,
        IORENAMED,
        IOCLASSIIFIED,
        COPIEDUUID,
        IODELETED,
        IOBOUND
    }
}
