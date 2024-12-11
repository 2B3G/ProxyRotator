package com.example.proxyrotator.Controllers;

import com.example.proxyrotator.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class MainController {
    @FXML
    private Button refreshBtn;
    @FXML
    private Button addBtn;
    @FXML
    private TextField proxySearch;
    @FXML
    VBox proxyContainer;
    @FXML
    ComboBox<String> countryChoice;
    @FXML
    WebView mapContainer;
    @FXML
    StackPane webviewContainer;

    private ObservableList<String> Countries = FXCollections.observableArrayList();
    private List<ProxyElement> Proxies = new ArrayList<>();

    private String addressFilter = "", countryFilter = "";

    private class Logger{
        public void log(String message){
            System.out.println(message);
        }
    }

    Logger jsLogger = new Logger();

    @FXML
    public void initialize() {
        Countries.add("Any");
        countryChoice.setItems(Countries);

        mapContainer.setContextMenuEnabled(false);
        WebEngine engine = mapContainer.getEngine();

        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((observableValue, oldValue, newValue) ->
        {
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("java", jsLogger);
        });

        String htmlPath = "/com/example/proxyrotator/assets/svg/index.html";
        engine.load(MainController.class.getResource(htmlPath).toExternalForm());

        setAddContextMenu();

        proxySearch.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String character = event.getCharacter();

            if (character.equals("\b")) {
                // If the backspace key is pressed, remove the last character from addressFilter
                if (!addressFilter.isEmpty()) {
                    addressFilter = addressFilter.substring(0, addressFilter.length() - 1);
                }
            } else {
                // If it's not backspace, add the character to the addressFilter
                addressFilter += character;
            }

            filterProxies();
        });

        countryChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            countryFilter = newValue;
            System.out.println(newValue);

            filterProxies();
        });

        // TODO : finish this connection status text. it will be green if connected, red if disconnected, it will show a big nice text like
        // CONNECTED or DISCONNECTED in the middle. then on the left will be the ip address with the port and on the right there will be the country
        Label bottomLabel = new Label("This is at the bottom");
        bottomLabel.getStyleClass().add("connect-status");

        StackPane.setAlignment(bottomLabel, javafx.geometry.Pos.BOTTOM_CENTER);

        webviewContainer.getChildren().add(bottomLabel);

        reloadProxies();
    }

    private void filterProxies(){
        List<ProxyElement> remainingProxies = Proxies;

        System.out.println(addressFilter);
        System.out.println(countryFilter);

        if(!addressFilter.isEmpty() && !countryFilter.isEmpty()){
            remainingProxies = ProxyFilter.filterByBoth(Proxies, addressFilter, countryFilter);
        }
        else if(addressFilter.isEmpty() && !countryFilter.isEmpty()){
            if(!countryFilter.equals("Any")) remainingProxies = ProxyFilter.filterByCountryName(Proxies, countryFilter);
        }
        else if(!addressFilter.isEmpty()){
            remainingProxies = ProxyFilter.filterByAddress(Proxies, addressFilter);
        }

        proxyContainer.getChildren().clear();

        for(int i = 0; i < Proxies.toArray().length ; i++){
            for(int j = 0; j < remainingProxies.toArray().length; j++){
                if(Proxies.get(i).equals(remainingProxies.get(j))) addProxyElement(remainingProxies.get(j), String.valueOf(i));
            }
        }
    }

    private void chooseProxyFile(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose proxy file");

        File lastDir = new File(UserPrefs.getLastProxyFolder());
        if(lastDir.exists()) fileChooser.setInitialDirectory(lastDir);

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt")
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        File f = fileChooser.showOpenDialog(MainApplication.mainStage);
        if(f == null) return;

        UserPrefs.setLastProxyFolder(f.getParent());
        try{
            ProxyManager.readFromFile(f);
        }catch (SQLException e){
            System.out.println("SQL Error adding proxies to user : " + e.getMessage());
            MainApplication.showDialog("SQL error", "Error saving proxies to users account. Restart the app and try again", Alert.AlertType.ERROR);
        }catch (IOException e){
            System.out.println("Error reading proxy file : " + e.getMessage());
            MainApplication.showDialog("File read error", "Error reading proxy file in path "+ f.getAbsolutePath() +". Try again", Alert.AlertType.ERROR);
        }
    }

    public void reloadProxies(){
        try{
            List<ProxyElement> proxies = ProxyManager.getProxies();

            proxyContainer.getChildren().clear();

            proxies.forEach(proxy -> {
                Proxies.add(Proxies.toArray().length, proxy);

                addProxyElement(proxy, String.valueOf(Proxies.toArray().length - 1));
            });
        } catch (SQLException e) {
            System.out.println("SQL Error while getting users proxies : " + e.getMessage());
            MainApplication.showDialog("SQL Error", "SQL Error while getting users proxies. Restart the app and try again", Alert.AlertType.ERROR);
        }
    }

    private void addProxyElement(ProxyElement proxy, String id){
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.getStyleClass().add("list-proxy-container");

        Label addressLabel = new Label(proxy.getAddress());
        addressLabel.getStyleClass().add("list-proxy");

        Label countryLabel = new Label(proxy.getCountryName());
        countryLabel.getStyleClass().add("list-proxy-country");

        VBox vbox = new VBox(addressLabel, countryLabel);
        HBox.setHgrow(vbox, javafx.scene.layout.Priority.ALWAYS);

        CheckBox checkBox = new CheckBox("");
        checkBox.setFocusTraversable(false);
        HBox.setMargin(checkBox, new Insets(0, 5, 0, 5)); // Right margin of 6.0

        hBox.getStyleClass().add("list-proxy-container");
        hBox.getChildren().addAll(vbox, checkBox);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setId(id);


        hBox.setOnMouseClicked(e -> {
            if(e.getButton() == MouseButton.PRIMARY){
                System.out.println(String.format("Proxy address : %s , country code : %s", proxy.getAddress(), proxy.getCountryCode()));

                // TODO : show on the map and make some cool animations or smth
            }
        });

        proxyContainer.getChildren().add(hBox);
        if(!Countries.contains(proxy.getCountryName())) Countries.add(proxy.getCountryName());
    }

    @FXML
    private void logout(){
        UserPrefs.removeUserId();
        MainApplication.switchScene("LoginLayout.fxml");
    }

    private void setAddContextMenu(){
        ContextMenu cm = new ContextMenu();

        MenuItem addOne = new MenuItem("One proxy");
        addOne.setOnAction(event -> getOneProxy());

        MenuItem fromFile = new MenuItem("Read from file");
        fromFile.setOnAction(event -> chooseProxyFile());

        cm.getItems().addAll(addOne, fromFile);

        addBtn.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                cm.show(addBtn, event.getScreenX(), event.getScreenY());
            }
        });

        addBtn.setContextMenu(cm);
    }

    private void getOneProxy(){
        Dialog<Object> dialog = new Dialog<>();

        dialog.setTitle("Proxy Address Input");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        VBox vbox = new VBox(5);
        vbox.setStyle("-fx-padding: 10;");
        vbox.setStyle("-fx-alignment: CENTER;");

        TextField input = new TextField();

        input.setPromptText("Type the proxy address");
        input.setMaxWidth(Double.MAX_VALUE);

        input.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            String character = e.getCharacter();
            if (!character.matches("[0-9.:]") || proxySearch.getText().length() > 40) {
                e.consume();
            }
        });

        vbox.getChildren().addAll(input);

        dialog.getDialogPane().setContent(vbox);
        Optional<Object> result = dialog.showAndWait();

        result.ifPresent(buttonType -> {
            if(buttonType == ButtonType.OK){
                String regex = "\\b(?:(?:https?:\\/\\/)?((?:[0-9]{1,3}\\.){3}[0-9]{1,3}):([1-9][0-9]{0,4}|[1-5][0-9]{4}|[0-5][0-9]{0,3}|[0-9]{1,4})\\b)";
                Pattern pattern = Pattern.compile(regex);

                if(!pattern.matcher(input.getText()).find()){
                    MainApplication.showDialog("Invalid proxy", "The proxy you tried to add is invalid, try again", Alert.AlertType.ERROR);

                    return;
                }

                try{
                    ProxyManager.addProxies(new HashSet<>(Collections.singletonList(input.getText())));
                }
                catch(InterruptedException e){
                    System.out.println("Main thread interrupted while getting proxies countries !");
                    MainApplication.showDialog("Thread Error", "Thread interrupt error while trying to add proxy!. Try again", Alert.AlertType.ERROR);
                }
                catch (SQLException e){
                    System.out.println("SQL Error while adding the proxy to users account : " + e.getMessage());
                    MainApplication.showDialog("SQL error", "Error adding proxy to users account. Restart the app and try again", Alert.AlertType.ERROR);
                }
            }
        });
    }
}
