package com.example.proxyrotator;

import java.util.prefs.Preferences;

public class UserPrefs {
    private static final Preferences prefs = Preferences.userNodeForPackage(UserPrefs.class);

    public static void setUserId(int userId) {
        prefs.putInt("user_id", userId);
    }

    public static int getUserId() {
        return prefs.getInt("user_id", -1);
    }

    public static void removeUserId(){
        prefs.remove("user_id");
    }

    public static void setLastProxyFolder(String path){
        prefs.put("proxy_folder", path);
    }

    public static String getLastProxyFolder(){
        return prefs.get("proxy_folder", "");
    }
}
