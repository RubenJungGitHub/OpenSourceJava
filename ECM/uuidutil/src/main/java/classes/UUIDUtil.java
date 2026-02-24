package contain.opensource.uuidutil.classes;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import contain.opensource.ils.shared.constants.AlfrescoConstants.ContainPlatforms;
import java.util.Optional;
import org.springframework.stereotype.Component;
import contain.opensource.ils.shared.ConfigurationProperties.ILSRestProperties;
import contain.opensource.ils.shared.classes.SpringContext;
import contain.opensource.ils.shared.constants.AlfrescoConstants;

@Component
public class UUIDUtil {

    private final ILSRestProperties iLSRestProperties;
    private static UUIDUtil instance;

    public UUIDUtil(ILSRestProperties ilsRestProperties) {
        this.iLSRestProperties = ilsRestProperties;
        // instance = this; // store bean in static reference
    }

    // Application-wide static method

    // ===================================================================================================================================================
    // Inside a container calling a method over http from within the container is a
    // bad pattern.
    // When in future a GetGUID over HTTP is to be realized, which it is, it should
    // be a separate microservice runnning in a separate container.
    // Now this rus fine in development on the host but once processed to the
    // contain-Ils container having this endpoint inside the same container, the
    // next axception is logged:
    // Get UUID endpoint : http://host.docker.internal:5000/GetUUID⁠ (This is by
    // design and in the app log.)
    // Bit this throws an exception: REST call failed with exception :
    // java.net.SocketException: Unexpected end of file from server
    // ===================================================================================================================================================

    public static String getUUIDOverHTTP(Optional<ContainPlatforms> prefix) {
        try {
            // Fetch Spring-managed bean
            UUIDUtil uuidUtil = SpringContext.getApplicationContext().getBean(UUIDUtil.class);
            String query = prefix.map(p -> "?prefix=" + p.name()).orElse("");

            String urlString = uuidUtil.iLSRestProperties.getBaseUrl() + "/api/GetUUID" + query;

            System.out.println(
                    AlfrescoConstants.RED + "Get UUID endpoint  : "
                            + urlString + AlfrescoConstants.RESET);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            System.out.println("Accessing uuid rest url on " + urlString + " return code -> " + status);

            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String uuid = in.readLine(); // assuming API returns plain UUID
                in.close();

                return uuid;
            } else {
                System.err.println("REST call failed with status: " + status);
            }
        } catch (Exception e) {

            System.err.println("REST call failed with exception : " + e);
            e.printStackTrace();
        }

        return null;
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