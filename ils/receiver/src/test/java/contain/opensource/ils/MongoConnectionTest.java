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

IOLogBallenbakMongo log =
    IOLogBallenbakMongo.builder()
        .id("test-uuid")
        .containIoUuid("io-123")
        .platformId("platform-A")
        .path("/home/path")
        .ioAction("READ")
        .ioSource("source")
        .ioDestination("dest")
        .pkiHash("hash")
        .ioReference("ref")
        .additionalInfo("info")
        .actionPerformed(eActionPerformed.ASSIGNUUID)
        .actionPerformedBy("admin")
        .marking("marking")
        .classification("class")
        .version("1.0")
        .logDateTime(java.time.LocalDateTime.now())
        .build();

   //     IOLogBallenbakMongo log = new IOLogBallenbakMongo("test-uuid", "platform-A", "path","Marking", "classification", "version", "containuuid", "IOAction", "source", "dest", "hash", "ref", " additionalinfo",LocalDateTime.now();, eActionPerformed.ASSIGNUUID, "actionpreformedby");

        // Save to Mongo
        repository.save(log);

        // Retrieve from Mongo
        var found = repository.findById("test-uuid");
        assert (found.isPresent());
        System.out.println("Connection Successful! Found: " + found.get());
        System.out.println("Test completed");
    }
}