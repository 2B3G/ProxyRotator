package com.example.proxyrotator;

import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    public static boolean login(String email, String password) throws SQLException {
        password = hash(password);

        int uid = checkUser(email, password);

        if(uid == -1){
            return false;
        }else{
            UserPrefs.setUserId(uid);
            return true;
        }
    }

    public static boolean register(String email, String password, String name, String last_name) throws SQLException {
        password = hash(password);

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

    private static String hash(String s) {
        try {
            // Get a MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert the input string to bytes and hash it
            byte[] hash = digest.digest(s.getBytes("UTF-8"));

            // Convert the hashed bytes to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
