package contain.opensource.ils.bs.receiver.Interfaces;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import contain.opensource.ils.bs.receiver.classes.Logger.IOLogDeltaLink;

@Repository
public interface IODeltalinkRepository extends JpaRepository<IOLogDeltaLink, String> {

    // Fetch by SourceID
    Optional<IOLogDeltaLink> findBySourceIdContaining(String sourceId);  
}