package contain.opensource.ils.bs.receiver.services;

import java.util.List;
import contain.opensource.ils.bs.receiver.Interfaces.IOLogBallenbakRepositoryMongo;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbakMongo;

//import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogServiceMongo {

    @Autowired
    private IOLogBallenbakRepositoryMongo logRepository;

    public void saveLog(IOLogBallenbakMongo log) {
        logRepository.save(log);
    }

    public List<IOLogBallenbakMongo> getAllLogs() {
        return logRepository.findAll();
    }
}
