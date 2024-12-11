package com.example.proxyrotator.Controllers;

import com.example.proxyrotator.MainApplication;
import com.example.proxyrotator.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class RegisterController {
    @FXML
    TextField name;
    @FXML
    TextField last_name;
    @FXML
    TextField email;
    @FXML
    PasswordField password;
    @FXML
    Text errorLabel;

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
    private void register(){
        String em = email.getText(), pass, nam = name.getText(), lastnam = last_name.getText();
        Pattern passwordPattern = Pattern.compile("^.*(?=.{8,})(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!_#$%&?]).*$");

        // TODO : do better?
        if(passwordContainer.getChildren().getFirst() instanceof PasswordField) pass = ((PasswordField) passwordContainer.getChildren().getFirst()).getText();
        else pass = ((TextField) passwordContainer.getChildren().getFirst()).getText();

        String message = "";

        if (nam.length() < 3 || nam.length() >= 30) message = "Name must be between 3 and 30 characters long";
        else if (lastnam.length() < 4 || lastnam.length() >= 40) message = "Last name must be between 4 and 40 characters long";
        else if(em.length() < 6 || em.length() >= 30) message = "Email must be between 6 and 50 characters long";
        else if(!em.contains("@")) message = "Invalid email format";
        else if(!passwordPattern.matcher(pass).matches()) message = "Password must be at least 8 characters long and include at least one: letter, number and a special character (!, _, #, $, %, &, ?)";

        errorLabel.setText(message);
        if(!message.isEmpty()) return;

        try{
            boolean success = UserService.register(email.getText(), password.getText(), name.getText(), last_name.getText());

            if(!success){
                errorLabel.setText("Account with this email or password already exists");
                password.setText("");
                email.setText("");

                return;
            }

            MainApplication.switchScene("MainLayout.fxml");
        }
        catch (SQLException e){
            errorLabel.setText("Database error while registering. Restart the app and try again");
            System.out.println(e.getMessage());
            System.out.println(e.getClass());
        }
    }

    @FXML
    private void goLogin(){
        MainApplication.switchScene("LoginLayout.fxml");
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
