import java.sql.*;

public class Main {

    public static void main(String[] args) throws SQLException {
        Diplomas diplomas = new Diplomas();
            diplomas.Migrate();

        System.out.println("Migration completed.");
    }
}
