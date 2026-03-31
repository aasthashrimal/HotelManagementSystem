package com.hotel.controller;

import com.hotel.main.MainApp;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * WEEK 9 - JavaFX: Login screen controller
 * Simple admin/staff login
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> roleCombo;

    @FXML
    public void initialize() {
        // WEEK 9 - ComboBox
        roleCombo.getItems().addAll("Admin", "Receptionist");
        roleCombo.setValue("Admin");
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        String role = roleCombo.getValue();

        // Role-aware credentials for this project
        boolean valid = false;
        com.hotel.main.MainApp.UserRole userRole = null;

        if ("Admin".equals(role)
                && "admin".equalsIgnoreCase(user)
                && "admin123".equals(pass)) {
            valid = true;
            userRole = com.hotel.main.MainApp.UserRole.ADMIN;
        }

        if ("Receptionist".equals(role)
                && "staff".equalsIgnoreCase(user)
                && "staff123".equals(pass)) {
            valid = true;
            userRole = com.hotel.main.MainApp.UserRole.STAFF;
        }

        if (valid) {
            MainApp.setCurrentUserRole(userRole);
            MainApp.navigateTo("Dashboard.fxml");
        } else {
            errorLabel.setText("Invalid username or password!");
            errorLabel.setVisible(true);
            passwordField.clear();
        }
    }

    @FXML
    private void handleClear() {
        usernameField.clear();
        passwordField.clear();
        errorLabel.setVisible(false);
    }
}
