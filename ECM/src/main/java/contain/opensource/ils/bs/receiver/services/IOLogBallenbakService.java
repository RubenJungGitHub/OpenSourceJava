package contain.opensource.ils.bs.receiver.services;

//import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import contain.opensource.ils.bs.receiver.Interfaces.IOLogBallenbakRepository;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbak;
import contain.opensource.ils.bs.receiver.classes.UUIDUtil;

@Service
public class IOLogBallenbakService {

    private final IOLogBallenbakRepository repository;

    public IOLogBallenbakService(IOLogBallenbakRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveLog(IOLogBallenbak log) {
        repository.save(log);
    }

    // Optional helper method to create and save in one step
    @Transactional
    public void log(String containIOUUID, String action, String source, String destination,
                    String pkiHash, String reference, String info) {
        IOLogBallenbak log = new IOLogBallenbak(
            UUIDUtil.getUUID(),
            containIOUUID,
            action,
            source,
            destination,
            pkiHash,
            reference,
            info,
            LocalDateTime.now()
        );
        repository.save(log);
    }
}