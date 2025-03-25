package com.example.proxyrotator;

import java.sql.*;
import java.util.List;

public class DatabaseManager {
    private static String url = "jdbc:mysql://avnadmin:AVNS_k4Hk07gPm1CbVU7IBDU@proxy-rotator-aplochocki124-9f19.h.aivencloud.com:25607/proxy-rotator-db?ssl-mode=REQUIRED";
    private static  String user = "avnadmin";
    private static String password = "AVNS_k4Hk07gPm1CbVU7IBDU";

    private static Connection connection;

    public static void connect() throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
    }

    public static void executeUpdate(String query, Object... params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        setParameters(statement, params);

        statement.executeUpdate();
    }

    public static ResultSet executeQuery(String query, Object... params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        setParameters(statement, params);

        return statement.executeQuery();
    }

    public static void executeBatch(String query, List<Object[]> batches) throws SQLException {
        connection.setAutoCommit(false);
        PreparedStatement statement = connection.prepareStatement(query);

        for (Object[] batch : batches) {
            for(int i=0; i<batch.length;i++){
                statement.setObject(i + 1, batch[i]);
            }
            statement.addBatch();
        }

        statement.executeBatch();
        connection.commit();
        connection.setAutoCommit(true);
    }

    private static void setParameters(PreparedStatement statement, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}
