package com.example.proxyrotator;

import com.example.proxyrotator.Controllers.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class MainApplication extends Application {
    public static int WINDOW_WIDTH = 800;
    public static int WINDOW_HEIGHT = 600;
    public static Stage mainStage;
    public static MainController mainController;

    @Override
    public void start(Stage stage) throws IOException {
        try{
            DatabaseManager.connect();
        }catch (SQLException e){
            System.out.println("Failed to connect to database. Error :" + e.getMessage());

            showDialog("Launch error", "Couldn't connect to the database. Check internet connection and try again", Alert.AlertType.ERROR);
            Platform.exit();
            return;
        }

        mainStage = stage;
        FXMLLoader fxmlLoader;

        if(UserPrefs.getUserId() != -1){
            fxmlLoader = new FXMLLoader(MainApplication.class.getResource("MainLayout.fxml"));
            mainController = fxmlLoader.getController();
        }
        else{
            fxmlLoader = new FXMLLoader(MainApplication.class.getResource("LoginLayout.fxml"));
        }

        Scene scene = new Scene(fxmlLoader.load(), MainApplication.WINDOW_WIDTH,  MainApplication.WINDOW_HEIGHT);
        stage.setTitle("Proxy Rotator");

        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        // process doesn't close without this
        stage.setOnCloseRequest(ProxyManager::closeExecutor);

        stage.setMinHeight(400);
        stage.setMinWidth(500);
        stage.setScene(scene);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void switchScene(String sceneName){
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(sceneName));
            Scene newScene = new Scene(loader.load(), MainApplication.WINDOW_WIDTH,  MainApplication.WINDOW_HEIGHT);

            newScene.getStylesheets().add(MainApplication.class.getResource("style.css").toExternalForm());

            if(sceneName == "MainLayout.fxml")
                mainController = loader.getController();

            mainStage.setScene(newScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showDialog(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    public static void connectToProxy(String proxy){
        System.out.println("Connecting to proxy " + proxy);
    }
}