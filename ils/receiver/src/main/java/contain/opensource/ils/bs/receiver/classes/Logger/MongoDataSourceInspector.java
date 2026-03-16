package contain.opensource.ils.bs.receiver.classes.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import jakarta.annotation.PostConstruct;

@Component
public class MongoDataSourceInspector implements CommandLineRunner {

    @Autowired
    private MongoClient mongoClient;


    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("======================================");
            // 2️⃣ Test JDBC connection
            // try (Connection conn = dataSource.getConnection()) {
            
            System.out.println("MongoClient class: " + mongoClient.getClass());
            MongoDatabase db = mongoClient.getDatabase("ilstools");
            System.out.println("Database name: " + db.getName());
            System.out.println("Collections: " + db.listCollectionNames().into(new java.util.ArrayList<>()));
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_YELLOW
                    + ("MOGNO DB reachable: " + db.getName())
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);

        } catch (Exception e) {
            System.err.println("Failed to obtain database connection via JDBC: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        System.out.println("======================================");
    }
}
