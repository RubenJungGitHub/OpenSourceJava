package contain.opensource.ils.bs.receiver.services;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import contain.opensource.ils.bs.receiver.Interfaces.IOLogBallenbakRepositoryMongo;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbakMongo;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.shared.constants.AlfrescoConstants.eActionPerformed;

@Service
public class IOLogBallenbakServiceMongo {

    // @Autowired
    private final IOLogBallenbakRepositoryMongo repository;
    private final ILSRestProperties ilsrestproperties;
    


    @Autowired
    public IOLogBallenbakServiceMongo(IOLogBallenbakRepositoryMongo repository, ILSRestProperties ilsrestproperties) {
        this.repository = repository;
        this.ilsrestproperties = ilsrestproperties;

    }

    @Transactional
    public void saveLog(IOLogBallenbakMongo log) {
        repository.save(log);
    }

       private String getloguuid() {
        String uuid = "";
        try {
            // String uuid =
            //
            System.out.println(
                    AlfrescoConstants.RED + "Get UUID for logging @ endpoint  : "
                            + ilsrestproperties.getUudiutilendpoint() + AlfrescoConstants.RESET);

            String urlString = ilsrestproperties.getUudiutilendpoint();

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            System.out.println("Accessing uuid rest url on " + ilsrestproperties.getUudiutilendpoint() +
                    " return code -> " + status);

            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                uuid = in.readLine(); // assuming API returns plain UUID
                in.close();
            }
        } catch (Exception ex) {
            return "exception returning loguuid";
        }
        return uuid;
    }


    // Delegate method to get the most recent entry
    public Optional<IOLogBallenbakMongo> GetLog(String uuid) {
      return repository.findTopByContainIoUuidOrderByLogDateTimeDesc(uuid);
    }

    // Optional helper method to create and save in one step
    @Transactional
    public void log(String containIOUUID, String PlatformID, String IOpath, String action, String source,
            String destination,
            String pkiHash, String reference, String info, eActionPerformed actionPerformed, String ActionPerformedBy, String Marking,String Classification,String version) {


     IOLogBallenbakMongo log = new IOLogBallenbakMongo();
        log.setId(getloguuid()); // generates a new UUID
        log.setContainIoUuid(containIOUUID);
        log.setPlatformId(PlatformID);
        log.setPath(IOpath);
        log.setIoAction(action);
        log.setIoSource(source);
        log.setIoDestination(destination);
        log.setPkiHash(pkiHash);
        log.setIoReference(reference);
        log.setAdditionalInfo(info);
        log.setLogDateTime(ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime());
        log.setActionPerformed(actionPerformed);
        log.setActionPerformedBy(ActionPerformedBy);
        log.setMarking(Marking);
        log.setClassification(Classification);
        log.setVersion(version);
    
        // Update Redis
        if (actionPerformed != eActionPerformed.IODELETED) {
            //RedisManager.putHash("IOLogs", containIOUUID, containIOUUID, 1200);
            RedisManager.putHash("IOLog", containIOUUID, pkiHash, 2400);

        }
    
        try {
            repository.save(log);
        } catch (Exception ex) {
            System.out.println("Error saving IOLogBallenbak: " + ex.getMessage());
        }
    }
    
}