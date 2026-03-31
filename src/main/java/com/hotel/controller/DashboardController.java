package com.hotel.controller;

import com.hotel.dao.BookingDAO;
import com.hotel.main.MainApp;
import com.hotel.service.RoomService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * WEEK 9 - JavaFX: Dashboard controller
 * Shows stats and navigation to all modules
 */
public class DashboardController {

    @FXML private Label totalRoomsLabel;
    @FXML private Label availableRoomsLabel;
    @FXML private Label occupiedRoomsLabel;
    @FXML private Label todayBookingsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Button staffModuleButton;
    @FXML private Button reportsModuleButton;

    private final RoomService roomService = new RoomService();
    private final BookingDAO bookingDAO = new BookingDAO();

    @FXML
    public void initialize() {
        loadStats();
        applyRoleVisibility();
    }

    private void applyRoleVisibility() {
        boolean isAdmin = MainApp.getCurrentUserRole() == MainApp.UserRole.ADMIN;
        if (staffModuleButton != null) {
            staffModuleButton.setVisible(isAdmin);
            staffModuleButton.setManaged(isAdmin);
        }
        if (reportsModuleButton != null) {
            reportsModuleButton.setVisible(isAdmin);
            reportsModuleButton.setManaged(isAdmin);
        }
    }

    private void loadStats() {
        int total = roomService.getAllRooms().size();
        int available = roomService.getAvailableCount();
        int occupied = roomService.getOccupiedCount();
        int todayBookings = bookingDAO.getTodayBookingsCount();
        double revenue = bookingDAO.getTotalRevenue();

        totalRoomsLabel.setText(String.valueOf(total));
        availableRoomsLabel.setText(String.valueOf(available));
        occupiedRoomsLabel.setText(String.valueOf(occupied));
        todayBookingsLabel.setText(String.valueOf(todayBookings));
        totalRevenueLabel.setText(String.format("₹%,.0f", revenue));
    }

    // Navigation handlers - WEEK 9 Button events
    @FXML private void goToRooms()     { MainApp.navigateTo("Rooms.fxml"); }
    @FXML private void goToCustomers() { MainApp.navigateTo("Customers.fxml"); }
    @FXML private void goToBookings()  { MainApp.navigateTo("Bookings.fxml"); }
    @FXML private void goToStaff()     { MainApp.navigateTo("Staff.fxml"); }
    @FXML private void goToReports()   { MainApp.navigateTo("Reports.fxml"); }
    @FXML private void goToServices()  { MainApp.navigateTo("Services.fxml"); }

    @FXML
    private void handleLogout() {
        MainApp.setCurrentUserRole(null);
        MainApp.navigateTo("Login.fxml");
    }

    @FXML
    private void handleRefresh() {
        loadStats();
    }
}
