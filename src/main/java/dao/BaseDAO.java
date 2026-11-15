package dao;

import context.DBContext;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * BaseDAO - Lớp cha cho các DAO trong hệ thống
 */
public abstract class BaseDAO {

    // . Các DAO lấy kết nối qua DBContext.getConnection(); câu SQL được giữ trong DAO.
    // .. Controller chỉ gọi phương thức DAO và nhận kết quả xử lý dữ liệu.
    protected Connection getConnection() throws SQLException, ClassNotFoundException {
        return DBContext.getConnection();
    }
}
