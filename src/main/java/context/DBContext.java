package context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBContext - Quản lý kết nối JDBC tới MySQL Database
 * Khối Context trong Data Layer
 */
public class DBContext {

    private static String getEnv(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.trim().isEmpty()) ? val.trim() : defaultValue;
    }

    private static final String HOST = getEnv("DB_HOST", "localhost");
    private static final String PORT = getEnv("DB_PORT", "3306");
    private static final String DB_NAME = getEnv("DB_NAME", "aura_cafe_erp");
    private static final String USER = getEnv("DB_USER", "root");
    private static final String PASSWORD = getEnv("DB_PASSWORD", "1234");

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=UTF-8&characterSetResults=UTF-8";

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
                System.out.println(" Database Product: " + conn.getMetaData().getDatabaseProductName() + " "
                        + conn.getMetaData().getDatabaseProductVersion());
                System.out.println(" Driver Name: " + conn.getMetaData().getDriverName() + " "
                        + conn.getMetaData().getDriverVersion());
                System.out.println("=========================================");
            }
        } catch (Exception e) {
            System.err.println(" ERROR: Connection failed!");
            e.printStackTrace();
        }
    }
}
