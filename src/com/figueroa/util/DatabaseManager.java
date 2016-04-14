package com.figueroa.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 *
 * Manages database tables and queries
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * January 2013
 */
public class DatabaseManager {

    Properties p;
    private Connection connection;
    private String connectionString;

    public DatabaseManager(String dbClassName, String conStr, String user, String password) 
            throws SQLException, ClassNotFoundException {

        connectionString = conStr;

        // Class.forName(xxx) loads the jdbc classes and
        // creates a drivermanager class factory
        Class.forName(dbClassName);

        // Properties for user and password
        p = new Properties();
        p.put("user", user);
        p.put("password", password);

        // Now try to connect
        connection = DriverManager.getConnection(connectionString, p);
    }

    /**
     * Close the current database connection.
     */
    public void closeConnection() {
        try {
            connection.close();
        }
        catch (SQLException e) {
            System.err.print("SQL Exception in closeConnection: " + e.getMessage());
        }
    }

    /**
     * Open a new database connection.
     * @throws SQLException 
     */
    public void openConnection() throws SQLException {
        connection = DriverManager.getConnection(connectionString, p);
    }

    /**
     * Truncate a table
     * @param tableName
     * @throws SQLException 
     */
    public void truncateTable(String tableName) throws SQLException {

        String truncateString = "TRUNCATE " + tableName;

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(truncateString);

        stmt.close();
    }

    /**
     * Execute and SQL UPDATE statement
     * @param updateString
     * @throws SQLException 
     */
    public void executeUpdate(String updateString) throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(updateString);

        stmt.close();
    }

    /**
     * Execute an SQL SELECT statement
     * @param selectString
     * @return
     * @throws SQLException 
     */
    public ResultSet executeQuery(String selectString) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(selectString);
        //stmt.close();

        return rs;
    }

    /**
     * Check whether the connection is open or closed.
     * @return
     * @throws SQLException 
     */
    public boolean connectionIsClosed() throws SQLException {
        return connection.isClosed();
    }
}
