package com.example.proxyrotator.Controllers;

import com.example.proxyrotator.MainApplication;
import com.example.proxyrotator.UserService;
import javafx.fxml.FXML;

import java.sql.SQLException;

public class LoginController extends AccountBaseController {

    @FXML
    private void login() {
        if (email.getText().isEmpty() || password.getText().isEmpty()) return;

        try {
            boolean success = UserService.login(email.getText(), password.getText());

            if (!success) {
                errorLabel.setText("Account with this email or password doesn't exist");
                clearFields();
                return;
            }

            switchToMainLayout();
        } catch (SQLException e) {
            errorLabel.setText("Database error while logging in. Restart the app and try again");
        }
    }

    @FXML
    private void goRegister() {
        MainApplication.switchScene("RegisterLayout.fxml");
    }
}