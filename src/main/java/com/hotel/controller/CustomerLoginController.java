package com.hotel.controller;

import com.hotel.dao.CustomerDAO;
import com.hotel.main.CustomerApp;
import com.hotel.main.CustomerSession;
import com.hotel.model.Customer;
import com.hotel.util.ValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CustomerLoginController {

    @FXML private VBox          loginBox;
    @FXML private VBox          registerBox;
    @FXML private Label         statusLabel;
    
    // Login
    @FXML private TextField     phoneField;
    @FXML private PasswordField passwordField;
    
    // Registration
    @FXML private TextField     regNameField;
    @FXML private TextField     regPhoneField;
    @FXML private TextField     regEmailField;
    @FXML private PasswordField regPasswordField;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private boolean isRegistering = false;

    @FXML
    private void handleLogin() {
        String phone = phoneField.getText().trim();
        String pass  = passwordField.getText().trim();
        
        if (phone.isEmpty() || pass.isEmpty()) {
            setStatus("Please enter phone and password.", true);
            return;
        }
        
        Customer c = customerDAO.authenticate(phone, pass);
        if (c != null) {
            CustomerSession.setLoggedInCustomer(c);
            setStatus("Login successful!", false);
            CustomerApp.navigateTo("CustomerDashboard.fxml");
        } else {
            setStatus("Invalid credentials.", true);
        }
    }

    @FXML
    private void toggleRegister() {
        isRegistering = !isRegistering;
        loginBox.setManaged(!isRegistering);
        loginBox.setVisible(!isRegistering);
        registerBox.setManaged(isRegistering);
        registerBox.setVisible(isRegistering);
        statusLabel.setText("");
    }

    @FXML
    private void handleRegister() {
        String name  = regNameField.getText().trim();
        String phone = regPhoneField.getText().trim();
        String email = regEmailField.getText().trim();
        String pwd   = regPasswordField.getText().trim();

        if (!ValidationUtil.isNotEmpty(name)) { setStatus("Enter your full name.", true); return; }
        if (!ValidationUtil.isValidPhone(phone)) { setStatus("Enter a valid 10-digit mobile number.", true); return; }
        if (pwd.length() < 4) { setStatus("Password too short (min 4 chars).", true); return; }

        Customer c = new Customer(0, name, phone, email, "", pwd);
        int newId = customerDAO.addCustomer(c);
        if (newId > 0) {
            c.setCustomerId(newId);
            CustomerSession.setLoggedInCustomer(c);
            CustomerApp.navigateTo("CustomerDashboard.fxml");
        } else {
            setStatus("Registration failed (phone might exist).", true);
        }
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }
}
