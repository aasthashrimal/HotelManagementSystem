package com.hotel.main;

import com.hotel.dao.DatabaseManager;
import com.hotel.util.FileLogger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * WEEK 9 - JavaFX: Main Application entry point
 */
public class MainApp extends Application {

    private static Stage primaryStage;
    private static UserRole currentUserRole;

    public enum UserRole {
        ADMIN,
        STAFF
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Initialize database on startup
        DatabaseManager.initializeDatabase();
        FileLogger.logInfo("Application started.");

        // Load login screen
        Parent root = FXMLLoader.load(getClass().getResource("/com/hotel/view/Login.fxml"));
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("Aurelia Suites - Management System");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static UserRole getCurrentUserRole() {
        return currentUserRole;
    }

    public static void setCurrentUserRole(UserRole role) {
        currentUserRole = role;
    }

    public static boolean canAccess(String fxmlFile) {
        if ("Login.fxml".equals(fxmlFile)) return true;
        if (currentUserRole == null) return false;

        if (currentUserRole == UserRole.ADMIN) return true;

        return switch (fxmlFile) {
            case "Dashboard.fxml", "Rooms.fxml", "Customers.fxml", "Bookings.fxml", "Services.fxml" -> true;
            default -> false;
        };
    }

    public static void navigateTo(String fxmlFile) {
        try {
            if (!canAccess(fxmlFile)) {
                FileLogger.logError("Access denied to " + fxmlFile + " for role " + currentUserRole);
                javafx.scene.control.Alert denied = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                denied.setTitle("Access Denied");
                denied.setHeaderText(null);
                denied.setContentText("You do not have permission to open this module.");
                denied.showAndWait();

                if (!"Dashboard.fxml".equals(fxmlFile) && canAccess("Dashboard.fxml")) {
                    navigateTo("Dashboard.fxml");
                }
                return;
            }

            Parent root = FXMLLoader.load(MainApp.class.getResource("/com/hotel/view/" + fxmlFile));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.sizeToScene();
        } catch (Exception e) {
            e.printStackTrace();
            FileLogger.logError("Navigation error to " + fxmlFile + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
