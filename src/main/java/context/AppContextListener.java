package context;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * AppContextListener - Quản lý vòng đời của ứng dụng web
 *
 * Giải quyết cảnh báo "Illegal access: this web application instance has been stopped already"
 * từ MySQL Connector/J AbandonedConnectionCleanupThread khi Tomcat hot-redeploy.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Không cần khởi tạo thêm
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 1. Dừng MySQL background cleanup thread đúng cách
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Exception ignored) {
        }

        // 2. Hủy đăng ký JDBC drivers để tránh memory leak
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException ignored) {
            }
        }
    }
}
