import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:postgresql://ep-winter-grass-a9ayf8vb-pooler.gwc.azure.neon.tech:5432/MiniProjekt?sslmode=require";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_dOKcqEp8Rvo2";
    public static Connection conn = connect();

    public static Connection connect() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Povezava uspešna!");
            return conn;
        } catch (SQLException e) {
            System.out.println("❌ Napaka pri povezavi: " + e.getMessage());
            return null;
        }
    }
}

