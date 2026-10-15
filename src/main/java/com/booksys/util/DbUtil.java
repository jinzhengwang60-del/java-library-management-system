package com.booksys.util;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.dbutils.QueryRunner;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

public final class DbUtil {
    private static final ComboPooledDataSource DATA_SOURCE = createDataSource();
    private DbUtil() {}

    private static ComboPooledDataSource createDataSource() {
        ComboPooledDataSource pool = new ComboPooledDataSource();
        try { pool.setDriverClass("com.mysql.cj.jdbc.Driver"); }
        catch (PropertyVetoException e) { throw new IllegalStateException("MySQL JDBC driver is unavailable", e); }
        pool.setJdbcUrl(AppConfig.get("jdbc.url", "BOOKSYS_DB_URL",
                "jdbc:mysql://127.0.0.1:3306/booksys?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"));
        pool.setUser(AppConfig.get("jdbc.username", "BOOKSYS_DB_USER", "booksys_app"));
        pool.setPassword(AppConfig.get("jdbc.password", "BOOKSYS_DB_PASSWORD", ""));
        pool.setInitialPoolSize(1);
        pool.setMinPoolSize(1);
        pool.setMaxPoolSize(10);
        pool.setCheckoutTimeout(5000);
        pool.setAcquireRetryAttempts(2);
        pool.setAcquireRetryDelay(500);
        return pool;
    }

    public static DataSource getDataSource() { return DATA_SOURCE; }
    public static QueryRunner getQueryRunner() { return new QueryRunner(DATA_SOURCE); }

    @FunctionalInterface
    public interface Transaction<T> { T run(Connection connection) throws SQLException; }

    /** One connection, one commit; checked and unchecked failures always roll back. */
    public static <T> T transaction(Transaction<T> operation) {
        try (Connection connection = DATA_SOURCE.getConnection()) {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = operation.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                if (e instanceof Error) throw (Error) e;
                throw new IllegalStateException("Database transaction failed", e);
            } finally { connection.setAutoCommit(previous); }
        } catch (SQLException e) { throw new IllegalStateException("Database connection failed", e); }
    }

    public static void close() { DATA_SOURCE.close(); }
}
