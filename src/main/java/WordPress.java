import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class WordPress {

    private final static String destinationDbConnectionString = "jdbc:mysql://localhost:3306/wordpress?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private final static String destinationDbUsername = "root";
    private final static String destinationDbPassword = "";

    public static String getDestinationDbConnectionString() {
        return destinationDbConnectionString;
    }

    public static String getDestinationDbUsername() {
        return destinationDbUsername;
    }

    public static String getDestinationDbPassword() {
        return destinationDbPassword;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getDestinationDbConnectionString(), getDestinationDbUsername(), getDestinationDbPassword());
    }
}
