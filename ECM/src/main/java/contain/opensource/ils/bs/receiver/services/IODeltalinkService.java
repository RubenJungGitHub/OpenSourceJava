package contain.opensource.ils.bs.receiver.services;

//import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import contain.opensource.ils.bs.receiver.Interfaces.IODeltalinkRepository;

import contain.opensource.ils.bs.receiver.classes.Logger.IOLogDeltaLink;

@Service
public class IODeltalinkService {

    private final IODeltalinkRepository repository;

    @Autowired
    public IODeltalinkService(IODeltalinkRepository linkrepository) {
        this.repository = linkrepository;
    }

    @Transactional
    public void saveLog(IOLogDeltaLink log) {
        repository.save(log);
    }

    // Delegate method to get the most recent entry
    public Optional<IOLogDeltaLink> GetLog(String SourceID) {
        return repository.findBySourceId(SourceID);
    }

    // Optional helper method to create and save in one step
    @Transactional
    public void log(String SourceID, String TokenID) {
        IOLogDeltaLink log = new IOLogDeltaLink(
                SourceID,
                TokenID
        );
        try {
            repository.save(log);
        } catch (Exception ex) {
            System.out.println("Error saving IODeltalink: " + ex.getMessage());
        }
    }

    // Fetch log by SourceId
    // -------------------------
    public Optional<IOLogDeltaLink> getLog(String sourceId) {
        return repository.findBySourceId(sourceId);
    }

    // -------------------------
    // Update tokenId for a specific SourceId
    // -------------------------
    @Transactional
    public boolean updateTokenForSourceId(String sourceId, String newTokenId) {
        Optional<IOLogDeltaLink> recordOpt = repository.findBySourceId(sourceId);

        if (recordOpt.isPresent()) {
            IOLogDeltaLink record = recordOpt.get();
            record.setTokenId(newTokenId);
            repository.save(record); // JPA will perform UPDATE
            return true;
        } else {
            return false; // no record found
        }
    }

}