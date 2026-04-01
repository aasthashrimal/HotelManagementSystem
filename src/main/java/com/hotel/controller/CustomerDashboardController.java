package com.hotel.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.CustomerDAO;
import com.hotel.dao.ServiceDAO;
import com.hotel.main.CustomerApp;
import com.hotel.main.CustomerSession;
import com.hotel.model.Booking;
import com.hotel.model.Customer;
import com.hotel.model.Room;
import com.hotel.service.BookingService;
import com.hotel.service.RoomService;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class CustomerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    
    // Bookings Table
    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, Number> colRoom;
    @FXML private TableColumn<Booking, String> colCheckIn;
    @FXML private TableColumn<Booking, String> colCheckOut;
    @FXML private TableColumn<Booking, Number> colAmount;
    @FXML private TableColumn<Booking, String> colStatus;
    
    // New Booking Form
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private ComboBox<Room> roomCombo;
    @FXML private Label pricePreviewLabel;
    
    // Services / Food
    @FXML private ComboBox<Booking> activeBookingCombo;
    @FXML private ComboBox<String> foodMenuCombo;
    @FXML private ComboBox<String> serviceCombo;
    
    @FXML private TableView<Map<String, Object>> servicesTable;
    @FXML private TableColumn<Map<String, Object>, String> colServiceType;
    @FXML private TableColumn<Map<String, Object>, String> colServiceDesc;
    @FXML private TableColumn<Map<String, Object>, String> colServiceStatus;

    private final BookingService bookingService = new BookingService();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final RoomService roomService = new RoomService();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    
    private final ObservableList<Booking> myBookings = FXCollections.observableArrayList();
    private final ObservableList<Map<String, Object>> myServices = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Customer c = CustomerSession.getLoggedInCustomer();
        if (c == null) return;
        welcomeLabel.setText("👋 Welcome, " + c.getName());

        setupTables();
        setupCombos();
        loadData();

        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));

        checkInPicker.valueProperty().addListener((o, ov, nv) -> { refreshRooms(); updatePricePreview(); });
        checkOutPicker.valueProperty().addListener((o, ov, nv) -> { refreshRooms(); updatePricePreview(); });
        roomCombo.valueProperty().addListener((o, ov, nv) -> updatePricePreview());
        refreshRooms();
    }

    private void setupTables() {
        // Bookings Table
        colRoom.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getRoomNumber()));
        colCheckIn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCheckIn() == null ? "" : d.getValue().getCheckIn().toString()));
        colCheckOut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCheckOut() == null ? "" : d.getValue().getCheckOut().toString()));
        colAmount.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getTotalAmount()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        bookingsTable.setItems(myBookings);

        // Services Table
        colServiceType.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue().get("service_type")));
        colServiceDesc.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue().get("description")));
        colServiceStatus.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue().get("status")));
        
        servicesTable.setItems(myServices);
    }

    private void setupCombos() {
        roomCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Room r) { return r == null ? "" : "#" + r.getRoomNumber() + " " + r.getRoomType() + " (Rs." + r.getPricePerNight() + ")"; }
            public Room fromString(String s) { return null; }
        });
        
        activeBookingCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Booking b) { return b == null ? "" : "Booking #" + b.getBookingId() + " (Room " + b.getRoomNumber() + ")"; }
            public Booking fromString(String s) { return null; }
        });

        foodMenuCombo.setItems(FXCollections.observableArrayList(
            "Margherita Pizza - Rs. 450", 
            "Pasta Alfredo - Rs. 380", 
            "Veg Biryani - Rs. 320",
            "Grilled Sandwich - Rs. 200",
            "Cold Coffee - Rs. 150"
        ));
        
        serviceCombo.setItems(FXCollections.observableArrayList(
            "Extra Towels",
            "Room Cleaning",
            "Make dummy phone call",
            "Wake Up Call"
        ));
    }

    private void loadData() {
        Customer c = CustomerSession.getLoggedInCustomer();
        if (c == null) return;
        
        // Load bookings
        bookingDAO.syncCheckInStatuses();
        List<Booking> all = bookingDAO.getAllBookings();
        List<Booking> filtered = all.stream().filter(b -> b.getCustomerId() == c.getCustomerId()).toList();
        myBookings.setAll(filtered);
        
        // Active bookings for combo
        List<Booking> active = filtered.stream().filter(b -> "CHECKED_IN".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus())).toList();
        activeBookingCombo.setItems(FXCollections.observableArrayList(active));
        if (!active.isEmpty()) activeBookingCombo.setValue(active.get(0));

        // Load services
        myServices.clear();
        for (Booking b : filtered) {
            myServices.addAll(serviceDAO.getServicesByBooking(b.getBookingId()));
        }
    }

    private void refreshRooms() {
        LocalDate cIn = checkInPicker.getValue();
        LocalDate cOut = checkOutPicker.getValue();
        if (cIn == null || cOut == null || !cOut.isAfter(cIn)) {
            roomCombo.setItems(FXCollections.observableArrayList());
            return;
        }
        roomCombo.setItems(FXCollections.observableArrayList(roomService.getAvailableRoomsForDates(cIn, cOut)));
    }

    private void updatePricePreview() {
        Room r = roomCombo.getValue();
        LocalDate in = checkInPicker.getValue(), out = checkOutPicker.getValue();
        if (r == null || in == null || out == null || !out.isAfter(in)) { pricePreviewLabel.setText(""); return; }
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(in, out);
        double charge = r.calculateTariff(nights);
        pricePreviewLabel.setText("Est: Rs." + charge);
    }

    @FXML
    private void handleBook() {
        Customer c = CustomerSession.getLoggedInCustomer();
        Room r = roomCombo.getValue();
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (r == null || in == null || out == null || !out.isAfter(in)) { 
            setStatus("Please select valid dates and room.", true); return; 
        }

        BookingService.BookingResult result = bookingService.bookRoom(c.getCustomerId(), r.getRoomNumber(), in, out);
        if (result.success) {
            setStatus("Room booked successfully!", false);
            loadData();
            refreshRooms();
        } else {
            setStatus(result.message, true);
        }
    }

    @FXML
    private void handleOrderFood() {
        Booking b = activeBookingCombo.getValue();
        String food = foodMenuCombo.getValue();
        if (b == null) { setStatus("No active booking selected.", true); return; }
        if (food == null || food.isBlank()) { setStatus("Select or type a food item.", true); return; }
        
        double price = 0;
        try {
            if (food.contains("Rs. ")) {
                price = Double.parseDouble(food.split("Rs\\. ")[1]);
            } else if (food.contains("- Rs.")) {
                price = Double.parseDouble(food.split("- Rs\\.")[1].trim());
            }
        } catch (Exception ignored) {}
        
        int sid = serviceDAO.addService(b.getBookingId(), "FOOD_ORDER", food, price, null);
        if (sid > 0) {
            setStatus("Food ordered! It is PENDING approval.", false);
            loadData();
        }
    }

    @FXML
    private void handleRequestService() {
        Booking b = activeBookingCombo.getValue();
        String req = serviceCombo.getValue();
        if (b == null || req == null || req.isBlank()) { setStatus("Select booking and request.", true); return; }
        
        int sid = serviceDAO.addService(b.getBookingId(), "GUEST_REQUEST", req, 0, null);
        if (sid > 0) {
            setStatus("Request sent successfully.", false);
            loadData();
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
        refreshRooms();
        setStatus("Dashboard refreshed securely from server.", false);
    }

    @FXML
    private void handleDeleteAccount() {
        Customer c = CustomerSession.getLoggedInCustomer();
        Alert confirm = new Alert(Alert.AlertType.WARNING, 
            "Are you sure you want to completely delete your account and all associated bookings? This cannot be undone.", 
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (customerDAO.deleteCustomer(c.getCustomerId())) {
                    CustomerSession.clearSession();
                    CustomerApp.navigateTo("CustomerLogin.fxml");
                } else {
                    setStatus("Failed to delete account.", true);
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        CustomerSession.clearSession();
        CustomerApp.navigateTo("CustomerLogin.fxml");
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }
}
