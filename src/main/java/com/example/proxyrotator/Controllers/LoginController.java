package com.example.proxyrotator.Controllers;

import com.example.proxyrotator.MainApplication;
import com.example.proxyrotator.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.sql.SQLException;

public class LoginController {
    @FXML
    TextField email;

    @FXML
    PasswordField password;

    @FXML
    Label errorLabel;

    @FXML
    private Button toggleButton;

    @FXML
    private HBox passwordContainer;

    private boolean isPasswordVisible = false;

    @FXML
    private void initialize(){
        setToggleButtonIcon("show");
    }

    @FXML
    private void login(){
        if(email.getText().isEmpty() || password.getText().isEmpty()) return;

        try{
            boolean success = UserService.login(email.getText(), password.getText());

            if(!success){
                errorLabel.setText("Account with this email or password doesn't exists");
                password.setText("");
                email.setText("");

                return;
            }

            MainApplication.switchScene("MainLayout.fxml");
        }
        catch (SQLException e){
            errorLabel.setText("Database error while logging in. Restart the app and try again");
        }
    }

    @FXML
    private void goRegister(){
        MainApplication.switchScene("RegisterLayout.fxml");
    }

    @FXML
    public void togglePasswordVisibility() {
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

    private void setToggleButtonIcon(String action) {
        String iconPath = "/com/example/proxyrotator/assets/icons/" + (action.equals("show") ? "show.png" : "hide.png");
        Image image = new Image(getClass().getResourceAsStream(iconPath));
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(17); // Set the desired height
        imageView.setFitWidth(17); // Set the desired width
        toggleButton.setGraphic(imageView); // Set the graphic for the button
    }
}
