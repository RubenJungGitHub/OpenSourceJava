package contain.opensource.ils.bs.receiver.Interfaces;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbak;

@Repository
public interface IOLogBallenbakRepository extends JpaRepository<IOLogBallenbak, String> {

    // Fetch the most recent log entry for a given containIOUUID
    Optional<IOLogBallenbak> findTopByContainIOUUIDOrderByLogDateTimeDesc(String containIOUUID);
}




