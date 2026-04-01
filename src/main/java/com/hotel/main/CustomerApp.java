package com.hotel.main;

import com.hotel.dao.DatabaseManager;
import com.hotel.util.FileLogger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Customer Portal Application entry point
 */
public class CustomerApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Initialize database on startup
        DatabaseManager.initializeDatabase();
        FileLogger.logInfo("Customer Portal started.");

        // Load login screen
        Parent root = FXMLLoader.load(getClass().getResource("/com/hotel/view/CustomerLogin.fxml"));
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("Aurelia Suites - Customer Guest Portal");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void navigateTo(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(CustomerApp.class.getResource("/com/hotel/view/" + fxmlFile));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(CustomerApp.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.sizeToScene();
        } catch (Exception e) {
            e.printStackTrace();
            FileLogger.logError("Navigation error in Customer Portal to " + fxmlFile + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
