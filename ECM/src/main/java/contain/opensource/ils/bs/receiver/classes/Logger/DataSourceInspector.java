package contain.opensource.ils.bs.receiver.classes.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DataSourceInspector implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    private static final String HOST = "192.168.178.210";
    private static final int PORT = 14330;



    @PostConstruct
    public void checkConnection() {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("DB reachable: " + conn.getMetaData().getURL());
        } catch (Exception e) {
            System.err.println("Cannot connect to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("======================================");
        // 1️⃣ Test TCP port reachability
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 3000); // 3s timeout
            System.out.println("TCP check passed: " + HOST + ":" + PORT + " is reachable");
        } catch (Exception e) {
            System.err.println("TCP check failed: Cannot reach " + HOST + ":" + PORT);
            e.printStackTrace();
        }

        // 2️⃣ Test JDBC connection
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("JDBC URL: " + conn.getMetaData().getURL());
            System.out.println("Database Product: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("Database Version: " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("Connected User: " + conn.getMetaData().getUserName());
        } catch (Exception e) {
            System.err.println("Failed to obtain database connection via JDBC: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("======================================");
    }
}
