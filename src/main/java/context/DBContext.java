package context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBContext - Quản lý kết nối JDBC tới MySQL Database
 * Khối Context trong Data Layer
 */
public class DBContext {

    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "cafe_management_system";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("=========================================");
                System.out.println(" SUCCESS: Connected to MySQL Database!");
                System.out.println(" Catalog / Database: " + conn.getCatalog());
                System.out.println(" Database Product: " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
                System.out.println(" Driver Name: " + conn.getMetaData().getDriverName() + " " + conn.getMetaData().getDriverVersion());
                System.out.println("=========================================");
            }
        } catch (Exception e) {
            System.err.println(" ERROR: Connection failed!");
            e.printStackTrace();
        }
    }
}
