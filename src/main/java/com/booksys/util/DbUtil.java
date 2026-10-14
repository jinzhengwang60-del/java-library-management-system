package com.booksys.util;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.dbutils.QueryRunner;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/** Database settings come from environment variables or a private external file. */
public final class DbUtil {
    private static final ComboPooledDataSource DATA_SOURCE = createDataSource();
    private static final ThreadLocal<Connection> CONNECTION = new ThreadLocal<>();
    private DbUtil() {}

    private static ComboPooledDataSource createDataSource() {
        Properties properties = new Properties();
        String config = System.getProperty("booksys.config");
        if (config == null || config.trim().isEmpty()) config = System.getenv("BOOKSYS_CONFIG");
        if (config != null && !config.trim().isEmpty()) {
            try (InputStream stream = Files.newInputStream(Paths.get(config))) {
                properties.load(stream);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read private database configuration", e);
            }
        }
        ComboPooledDataSource pool = new ComboPooledDataSource();
        try {
            pool.setDriverClass("com.mysql.cj.jdbc.Driver");
        } catch (PropertyVetoException e) {
            throw new IllegalStateException("MySQL JDBC driver is unavailable", e);
        }
        pool.setJdbcUrl(setting(properties, "jdbc.url", "BOOKSYS_DB_URL",
                "jdbc:mysql://127.0.0.1:3306/booksys?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"));
        pool.setUser(setting(properties, "jdbc.username", "BOOKSYS_DB_USER", "booksys_app"));
        pool.setPassword(setting(properties, "jdbc.password", "BOOKSYS_DB_PASSWORD", ""));
        pool.setInitialPoolSize(1);
        pool.setMinPoolSize(1);
        pool.setMaxPoolSize(10);
        pool.setCheckoutTimeout(5000);
        pool.setAcquireRetryAttempts(2);
        pool.setAcquireRetryDelay(500);
        return pool;
    }

    private static String setting(Properties properties, String key, String environment, String fallback) {
        String value = System.getenv(environment);
        return value != null ? value : properties.getProperty(key, fallback);
    }

    public static DataSource getDataSource() { return DATA_SOURCE; }
    public static QueryRunner getQueryRunner() { return new QueryRunner(DATA_SOURCE); }

    public static Connection getConnection() throws SQLException {
        Connection connection = CONNECTION.get();
        if (connection == null) {
            connection = DATA_SOURCE.getConnection();
            CONNECTION.set(connection);
        }
        return connection;
    }

    public static void startTransaction() throws SQLException { getConnection().setAutoCommit(false); }
    public static void commit() throws SQLException { finishTransaction(true); }
    public static void rollback() throws SQLException { finishTransaction(false); }

    private static void finishTransaction(boolean commit) throws SQLException {
        Connection connection = CONNECTION.get();
        if (connection == null) return;
        try {
            if (commit) connection.commit(); else connection.rollback();
        } finally {
            try { connection.setAutoCommit(true); }
            finally { connection.close(); CONNECTION.remove(); }
        }
    }

    public static void close() { DATA_SOURCE.close(); }
}
