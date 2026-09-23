package dao;

import context.DBContext;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * BaseDAO - Lớp cha cho các DAO trong hệ thống
 */
public abstract class BaseDAO {

    protected Connection getConnection() throws SQLException, ClassNotFoundException {
        return DBContext.getConnection();
    }
}
