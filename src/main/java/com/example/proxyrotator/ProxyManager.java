package com.example.proxyrotator;

import javafx.scene.control.Alert;
import javafx.stage.WindowEvent;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProxyManager {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(50);

    public static void readFromFile(File file) throws SQLException, IOException {
        List<String> newProxiesList = Files.readAllLines(file.toPath());
        Set<String> newProxiesHashSet = new HashSet<>(newProxiesList);
        String regex = "\\b(?:(?:https?:\\/\\/)?((?:[0-9]{1,3}\\.){3}[0-9]{1,3}):([1-9][0-9]{0,4}|[1-5][0-9]{4}|[0-5][0-9]{0,3}|[0-9]{1,4})\\b)";
        Pattern pattern = Pattern.compile(regex);

        newProxiesHashSet = newProxiesHashSet.stream().filter(proxy -> pattern.matcher(proxy).find()).collect(Collectors.toSet());
        try{
            addProxies(newProxiesHashSet);
        }
        catch(InterruptedException e){
            System.out.println("Main thread interrupted while getting proxies countries !");
        }
    }

    public static void addProxies(Set<String> proxies) throws SQLException, InterruptedException {
        List<Future<ProxyElement>> futures = new ArrayList<>();

        int uid = UserPrefs.getUserId();
        List<Object[]> batches = new ArrayList<>();

        proxies.forEach(proxy -> {
            Future<ProxyElement> future = executorService.submit(() -> {
                String[] countryData = getProxyCountry(proxy);

                if(countryData == null) return new ProxyElement(null, null, null);

                return new ProxyElement(proxy, countryData[0], countryData[1]);
            });
            futures.add(future);
        });

        for (Future<ProxyElement> future : futures) {
            try{
                ProxyElement result = future.get();

                if(result.countryName == null || result.countryCode == null) continue;

                batches.add(new Object[]{uid, result.address, result.countryName, result.countryCode});
            }
            catch (ExecutionException e){}
        }

        String sql = "INSERT IGNORE INTO proxies (user_id, proxy_address, countryName, countryCode) VALUES (?, ?, ?, ?)";
        DatabaseManager.executeBatch(sql, batches);

        MainApplication.showDialog("Success", "Proxies have been added!", Alert.AlertType.INFORMATION);

        MainApplication.mainController.reloadProxies();
    }

    public static List<ProxyElement> getProxies() throws SQLException{
        int uid = UserPrefs.getUserId();
        ResultSet rs = DatabaseManager.executeQuery("SELECT proxy_address, countryName, countryCode FROM proxies WHERE user_id = ?", uid);
        List<ProxyElement> proxies = new ArrayList<>();

        while(rs.next()){
            ProxyElement proxy = new ProxyElement(
                    rs.getString("proxy_address"),
                    rs.getString("countryName"),
                    rs.getString("countryCode"));

            proxies.add(proxy);
        }
        rs.close();

        return proxies;
    }

    public static void deleteProxies(String[] proxies) throws SQLException {
        int uid = UserPrefs.getUserId();
        List<Object[]> baches = new ArrayList<>();

        for(String proxy: proxies){
            baches.add(new Object[]{uid, proxy});
        }

        DatabaseManager.executeBatch("DELETE FROM proxies WHERE proxy = ? AND user_id = ?", baches);
        MainApplication.mainController.reloadProxies();
    }

    private static String[] getProxyCountry(String proxy){
        try {
            URL url = new URL("https://www.iplocation.net/get-ipdata");
            String formData = "ip=" + proxy.split(":")[0] + "&source=ip2location&ipv=4"; // just the address

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);  // Indicates that we intend to send data in the request body
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(formData.length()));

            try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
                writer.writeBytes(formData);
                writer.flush();
            }

            int responseCode = connection.getResponseCode();

            // Read the response
            if (responseCode == HttpURLConnection.HTTP_OK) { // 200 OK
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());

                try{
                    JSONObject result = json.getJSONObject("res");

                    return new String[] {result.getString("countryName"), result.getString("countryCode")};
                }catch (JSONException e){
                    System.out.println("Country field not found for the ip address : " + proxy);

                    return null;
                }
            } else {
                System.out.println("Failed to get country for the ip address : " + proxy + ". Request code : " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void closeExecutor(WindowEvent windowEvent){
        executorService.shutdown();
    }
}
