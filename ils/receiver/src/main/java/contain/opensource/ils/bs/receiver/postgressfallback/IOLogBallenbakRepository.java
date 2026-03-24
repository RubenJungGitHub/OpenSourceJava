package contain.opensource.ils.bs.receiver.postgressfallback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IOLogBallenbakRepository extends JpaRepository<IOLogBallenbak, String> {

    // Fetch the most recent log entry for a given containIOUUID
    Optional<IOLogBallenbak> findTopByContainIoUuidOrderByLogDateTimeDesc(String containIoUuid);
}