package contain.opensource.ils.bs.receiver.classes.Logger;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;

import jakarta.annotation.PostConstruct;


//Disable bean for now because MongoDB is now in scope and not postgress
//@Component
public class PostGresDataSourceInspector implements CommandLineRunner {

  //  @Autowired
    private DataSource dataSource;

    //Hardcoded for now
    
    //private static final String HOST = "192.168.178.210";
    //private static final int PORT = 14330;



    @PostConstruct
    public void checkConnection() {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_GREEN
                    + ("POSTGRESS DB reachable: " + conn.getMetaData().getURL())
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
        } catch (Exception e) {
            System.err.println("Cannot connect to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }



    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("======================================");
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
