package com.example.proxyrotator;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    public static boolean login(String email, String password) throws SQLException {
        int uid = checkUser(email, password);

        if(uid == -1){
            return false;
        }else{
            UserPrefs.setUserId(uid);
            return true;
        }
    }

    public static boolean register(String email, String password, String name, String last_name) throws SQLException {
        if(checkUser(email, password) != -1){
            return false;
        }else{
            DatabaseManager.executeUpdate("INSERT INTO users (name, last_name, email, password) VALUES (?, ?, ?, ?);", name, last_name, email, password);

            ResultSet rs = DatabaseManager.executeQuery("SELECT LAST_INSERT_ID()");

            if (rs.next()) {
                int lastInsertId = rs.getInt(1);

                UserPrefs.setUserId(lastInsertId);
            }

            rs.close();

            return true;
        }
    }

    private static int checkUser(String email, String password) throws SQLException {
        ResultSet rs = DatabaseManager.executeQuery("SELECT id FROM users WHERE email = ? AND password = ? ;", email, password);

        if(rs.next())
            return rs.getInt("id");

        rs.close();

        return -1;
    }
}
