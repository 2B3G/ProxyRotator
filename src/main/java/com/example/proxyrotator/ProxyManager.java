package com.example.proxyrotator;

import javafx.scene.control.Alert;
import javafx.stage.WindowEvent;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

        if(batches.isEmpty()) MainApplication.showDialog("Fail", "No valid proxies found", Alert.AlertType.INFORMATION);
        else MainApplication.showDialog("Success", "Added " + batches.size() + " proxies!", Alert.AlertType.INFORMATION);

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

    public static void deleteProxies(List<ProxyElement> proxies) throws SQLException {
        int uid = UserPrefs.getUserId();

        String placeholders = String.join(",", proxies.stream().map(proxy -> "?").toArray(String[]::new));
        String sql = "DELETE FROM proxies WHERE user_id = ? AND proxy_address IN (" + placeholders + ")";

        ArrayList<String> parameters = proxies.stream().map(ProxyElement::getAddress).collect(Collectors.toCollection(ArrayList::new));
        parameters.addFirst(String.valueOf(uid));

        DatabaseManager.executeUpdate(sql, parameters.toArray());
    }

    // TODO : implement rate limit of 60 requests per minute
    public static String[] getProxyCountry(String proxy){
        try {
            URL url = new URL("https://freeipapi.com/api/json/" + proxy.split(":")[0]);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
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
                    return new String[] {json.getString("countryName"), json.getString("countryCode")};
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

    public static void setProxy(ProxyElement proxy){
        String command = "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 1 /f";
        String proxyCommand = String.format("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer /t REG_SZ /d %s:%s /f", proxy.getAddress(), proxy.getAddress().split(":")[1]);

        try {
            Runtime.getRuntime().exec(command);
            Runtime.getRuntime().exec(proxyCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * @return the proxy currently set or null if no proxy set
    ***/
    public static String getProxySet(){
        String proxyServerCommand = "reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer";

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", proxyServerCommand);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("ProxyServer")) {
                    // Get the proxy server address
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 2) {
                        return parts[2]; // Return the proxy server address
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // Proxy server not found
    }

    /**
        @return The proxies which are not usable
    */
    public static List<ProxyElement> checkProxies(List<ProxyElement> proxies) throws RuntimeException{
        List<Future<ProxyElement>> futures = new ArrayList<>();
        List<ProxyElement> bad = new ArrayList<>();

        proxies.forEach(proxy -> futures.add(executorService.submit(() -> isProxyWorking(proxy))));

        for (Future<ProxyElement> future : futures) {
            try{
                ProxyElement result = future.get();

                if (result != null) bad.add(result);
            }
            catch (ExecutionException | InterruptedException e){
                throw new RuntimeException(e);
            }
        }

        return bad;
    }

    private static ProxyElement isProxyWorking(ProxyElement proxyElement){
        String[] addrPort = proxyElement.getAddress().split(":");
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(addrPort[0], Integer.parseInt(addrPort[1])));

        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if(responseCode != 200 && responseCode != 403) return proxyElement;
        }catch (SocketTimeoutException | SocketException e){
            // this means a timeout
            return proxyElement;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public static void disableProxy() {
        try {
            String disableProxyCommand = "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 0 /f";
            String removeProxyCommand = "reg delete \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer /f";

            // Execute commands
            Runtime.getRuntime().exec(disableProxyCommand);
            Runtime.getRuntime().exec(removeProxyCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
