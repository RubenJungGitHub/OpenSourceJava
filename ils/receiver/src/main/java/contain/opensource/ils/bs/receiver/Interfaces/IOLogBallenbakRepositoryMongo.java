package contain.opensource.ils.bs.receiver.Interfaces;

import org.springframework.stereotype.Repository;
import java.util.Optional;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbakMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

@Repository
public interface IOLogBallenbakRepositoryMongo extends MongoRepository<IOLogBallenbakMongo, String> {
    
    // Custom query: Spring generates the logic based on the method name!
    List<IOLogBallenbakMongo> findByPlatformId(String platformId);
    
    List<IOLogBallenbakMongo> findByActionPerformedBy(String user);

    Optional<IOLogBallenbakMongo> findTopByContainIoUuidOrderByLogDateTimeDesc(String containIoUuid);
}
