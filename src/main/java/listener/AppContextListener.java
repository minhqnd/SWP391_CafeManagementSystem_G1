package listener;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * AppContextListener - Quản lý vòng đời ứng dụng web.
 * Ngăn chặn rò rỉ bộ nhớ (Memory Leak) khi Tomcat reload hoặc undeploy:
 * 1. Hủy đăng ký JDBC Driver (com.mysql.cj.jdbc.Driver)
 * 2. Dừng luồng dọn dẹp MySQL AbandonedConnectionCleanupThread
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Khởi động ứng dụng
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 1. Hủy đăng ký tất cả các JDBC Drivers đã nạp bởi classloader này
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException e) {
                // Bỏ qua nếu không thể hủy đăng ký
            }
        }

        // 2. Dừng triệt để luồng dọn dẹp kết nối ngầm của MySQL Driver
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable t) {
            // Bỏ qua nếu lớp hoặc luồng đã được giải phóng
        }
    }
}
