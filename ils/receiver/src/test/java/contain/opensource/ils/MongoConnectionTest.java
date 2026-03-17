package contain.opensource.ils;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import contain.opensource.ils.bs.receiver.Interfaces.IOLogBallenbakRepositoryMongo;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbakMongo;
import contain.opensource.shared.constants.AlfrescoConstants.eActionPerformed;

//@SpringBootTest(classes = startreceiver.class)
@SpringBootTest
class MongoConnectionTest {

    @Autowired
    private IOLogBallenbakRepositoryMongo repository;

    @Autowired
    private MongoClient mongoClient;

    @Test
    void checkConnection() {

        System.out.println("MongoClient class: " + mongoClient.getClass());
        MongoDatabase db = mongoClient.getDatabase("ilstools");
        System.out.println("Database name: " + db.getName());
        System.out.println("Collections: " + db.listCollectionNames().into(new java.util.ArrayList<>()));
        String ID = "Test-UUID_Ruben";
        IOLogBallenbakMongo log = new IOLogBallenbakMongo();
        log.setId(ID); // generates a new UUID
        log.setContainIoUuid("containIOUUID");
        log.setPlatformId("PlatformID");
        log.setPath("IOpath");
        log.setIoAction("action");
        log.setIoSource("source");
        log.setIoDestination("destination");
        log.setPkiHash("pkiHash");
        log.setIoReference("reference");
        log.setAdditionalInfo("info");
        log.setLogDateTime(LocalDateTime.now());
        log.setActionPerformed(eActionPerformed.ASSIGNUUID);
        log.setActionPerformedBy("ActionPerformedBy");
        log.setMarking("Marking");
        log.setClassification("Classification");
        log.setVersion("version");

        // Save to Mongo
        repository.save(log);

        // Retrieve from Mongo
        var found = repository.findById(ID);
        assert (found.isPresent());
        System.out.println("Connection Successful! Found: " + found.get());
        System.out.println("Test completed");
    }
}