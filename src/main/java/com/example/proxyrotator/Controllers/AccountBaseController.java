package com.example.proxyrotator.Controllers;

import com.example.proxyrotator.MainApplication;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public abstract class AccountBaseController {
    @FXML
    protected TextField email;
    @FXML
    protected PasswordField password;
    @FXML
    protected Label errorLabel;
    @FXML
    protected Button toggleButton;
    @FXML
    protected HBox passwordContainer;

    protected boolean isPasswordVisible = false;

    @FXML
    protected void initialize() {
        toggleButton.setOnMouseClicked((e) -> {
            if(e.getButton() == MouseButton.PRIMARY)
                togglePasswordVisibility();
        });

        setToggleButtonIcon("show");
    }

    protected void togglePasswordVisibility() {
        if (isPasswordVisible) {
            TextField shownPassword = (TextField) passwordContainer.getChildren().getFirst();
            password.setText(shownPassword.getText());
            passwordContainer.getChildren().remove(shownPassword);
            passwordContainer.getChildren().addFirst(password);
            setToggleButtonIcon("show");
            isPasswordVisible = false;
        } else {
            TextField shownPassword = new TextField(password.getText());
            shownPassword.setPromptText("Enter Password");
            HBox.setHgrow(shownPassword, Priority.ALWAYS);
            passwordContainer.getChildren().remove(password);
            passwordContainer.getChildren().addFirst(shownPassword);
            setToggleButtonIcon("hide");
            isPasswordVisible = true;
        }
    }

    protected void setToggleButtonIcon(String action) {
        String iconPath = "/com/example/proxyrotator/assets/icons/" + (action.equals("show") ? "show.png" : "hide.png");
        Image image = new Image(getClass().getResourceAsStream(iconPath));
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(17); // Set the desired height
        imageView.setFitWidth(17); // Set the desired width
        toggleButton.setGraphic(imageView); // Set the graphic for the button
    }

    protected void switchToMainLayout() {
        MainApplication.switchScene("MainLayout.fxml");
    }

    protected void clearFields() {
        email.setText("");
        password.setText("");
    }
}