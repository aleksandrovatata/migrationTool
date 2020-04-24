import java.sql.*;

public class Joomla {

    private final static String sourceDbConnectionString = "jdbc:mysql://localhost:3306/jumla?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private final static String sourceDbUsername ="root";
    private final static String sourceDbPassword = "";

    public static String getSourceDbConnectionString() {
        return sourceDbConnectionString;
    }

    public static String getSourceDbUsername() {
        return sourceDbUsername;
    }

    public static String getSourceDbPassword() {
        return sourceDbPassword;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getSourceDbConnectionString(), getSourceDbUsername(), getSourceDbPassword());
    }
}
