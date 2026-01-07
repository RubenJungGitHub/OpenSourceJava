package contain.opensource.ils.bs.receiver.classes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UUIDUtil {

    // Application-wide static method
    public static String getUUID() {
        String uuid = null;
        String urlString = "http://localhost:5000/GetUUID"; // replace with your REST endpoint

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                uuid = in.readLine(); // assuming the API returns plain UUID text
                in.close();
            } else {
                System.err.println("REST call failed with status: " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return uuid;
    }

    // Test main
    public static void main(String[] args) {
        String guid = UUIDUtil.getUUID();
        System.out.println("UUID from REST API: " + guid);
    }
}