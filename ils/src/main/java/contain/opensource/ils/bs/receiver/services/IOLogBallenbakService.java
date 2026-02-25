package contain.opensource.ils.bs.receiver.services;

//import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import contain.opensource.ils.bs.receiver.Interfaces.IOLogBallenbakRepository;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbak;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.ils.bs.receiver.classes.UUIDUtil;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.eActionPerformed;

@Service
public class IOLogBallenbakService {

    // @Autowired
    private final IOLogBallenbakRepository repository;

    @Autowired
    public IOLogBallenbakService(IOLogBallenbakRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveLog(IOLogBallenbak log) {
        repository.save(log);
    }


    // Delegate method to get the most recent entry
    public Optional<IOLogBallenbak> GetLog(String uuid) {
        return repository.findTopByContainIoUuidOrderByLogDateTimeDesc(uuid);
    }

    // Optional helper method to create and save in one step
    @Transactional
    public void log(String containIOUUID, String PlatformID, String IOpath, String action, String source,
            String destination,
            String pkiHash, String reference, String info, eActionPerformed actionPerformed, String ActionPerformedBy, String Marking,String Classification,String version) {
        IOLogBallenbak log = new IOLogBallenbak(
                UUIDUtil.getUUIDOverHTTP(Optional.empty()),
                containIOUUID,
                PlatformID,
                IOpath,
                action,
                source,
                destination,
                pkiHash,
                reference,
                info,
                actionPerformed,
                ActionPerformedBy,
                Marking,
                Classification,
                version);
        // Update Redis
        if (actionPerformed != eActionPerformed.IODELETED) {
            RedisManager.putHash("IOLogs", containIOUUID, pkiHash, 120);
        }

        try {
            repository.save(log);
        } catch (Exception ex) {
            System.out.println("Error saving IOLogBallenbak: " + ex.getMessage());
        }
    }
}