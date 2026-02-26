package contain.opensource.uuidutil.classes;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.shared.constants.AlfrescoConstants.ContainPlatforms;

@Component
@ConfigurationProperties(prefix = "ils.rest")
public class UUIDUtil {

    private final ILSRestProperties iLSRestProperties;
    private static UUIDUtil instance;

    public UUIDUtil(ILSRestProperties ilsRestProperties) {
        this.iLSRestProperties = ilsRestProperties;
        // instance = this; // store bean in static reference
    }
    // To avoid rest endpoint http call within the same container
    public static String getUUID(ContainPlatforms prefix) {
        try {
            UUID uuid = UUID.randomUUID();
            System.out.println(
                    AlfrescoConstants.RED + "GUID RETURNED : "
                            + uuid.toString() + AlfrescoConstants.RESET);
            return prefix.toString() + "-" + uuid.toString();
        } catch (Exception e) {
            System.err.println("exception in getUUID : " + e);
            e.printStackTrace();
            return null;
        }
    }
}