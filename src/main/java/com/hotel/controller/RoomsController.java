package com.hotel.controller;

import com.hotel.main.MainApp;
import com.hotel.model.*;
import com.hotel.service.RoomService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.*;

/**
 * WEEK 9 - JavaFX: Rooms screen
 * ADVANCE BOOKING: availability shown reflects actual bookings for today
 */
public class RoomsController {

    @FXML private TableView<Room>              roomsTable;
    @FXML private TableColumn<Room, Integer>   colRoomNumber;
    @FXML private TableColumn<Room, String>    colType;
    @FXML private TableColumn<Room, String>    colDescription;
    @FXML private TableColumn<Room, Double>    colPrice;
    @FXML private TableColumn<Room, String>    colAvailable;

    @FXML private TextField          roomNumberField;
    @FXML private ComboBox<RoomType> roomTypeCombo;
    @FXML private TextField          descriptionField;
    @FXML private TextField          priceField;
    @FXML private ComboBox<String>   filterCombo;
    @FXML private Label              statusLabel;

    private final RoomService roomService = new RoomService();
    private final ObservableList<Room> roomList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupComboBoxes();
        loadRooms();
        roomsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, ov, nv) -> populateForm(nv));
    }

    private void setupTable() {
        colRoomNumber.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getRoomNumber()));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRoomType().name()));
        colDescription.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
        colPrice.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getPricePerNight()));

        // ADVANCE BOOKING: available flag is now set from bookings table for TODAY
        colAvailable.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().isAvailable() ? "✔ Available Today" : "✘ Occupied Today"));

        colAvailable.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.startsWith("✔")
                        ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                        : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });

        roomsTable.setItems(roomList);
    }

    private void setupComboBoxes() {
        roomTypeCombo.setItems(FXCollections.observableArrayList(RoomType.values()));
        roomTypeCombo.setValue(RoomType.STANDARD);
        filterCombo.setItems(FXCollections.observableArrayList(
                "All Rooms", "Available Today", "Occupied Today", "By Price ↑", "By Price ↓",
                "STANDARD", "DELUXE", "SUITE"));
        filterCombo.setValue("All Rooms");
    }

    private void loadRooms() {
        // getAllRooms() enriches each room with today's date-based availability
        roomList.setAll(roomService.getAllRooms());
        long avail = roomList.stream().filter(Room::isAvailable).count();
        setStatus("Loaded " + roomList.size() + " rooms  |  " + avail + " available today.", false);
    }

    @FXML
    private void handleAdd() {
        if (!validateForm()) return;
        Room room = createRoomFromForm();
        if (roomService.addRoom(room)) {
            loadRooms(); clearForm();
            setStatus("Room " + room.getRoomNumber() + " added.", false);
        } else {
            setStatus("Failed — room number may already exist.", true);
        }
    }

    @FXML
    private void handleUpdate() {
        Room selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a room to update.", true); return; }
        if (!validateForm()) return;
        Room room = createRoomFromForm();
        if (roomService.updateRoom(room)) {
            loadRooms(); clearForm(); setStatus("Room updated.", false);
        } else {
            setStatus("Update failed.", true);
        }
    }

    @FXML
    private void handleDelete() {
        Room selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a room to delete.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete Room #" + selected.getRoomNumber() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (roomService.deleteRoom(selected.getRoomNumber())) {
                    loadRooms(); clearForm(); setStatus("Room deleted.", false);
                } else {
                    setStatus("Cannot delete (may have bookings).", true);
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadRooms();
        setStatus("Rooms list refreshed.", false);
    }

    @FXML
    private void handleFilter() {
        switch (filterCombo.getValue()) {
            case "Available Today"  -> roomList.setAll(roomService.getAvailableRoomsToday());
            case "Occupied Today"   -> {
                var all = roomService.getAllRooms();
                all.removeIf(Room::isAvailable);
                roomList.setAll(all);
            }
            case "By Price ↑"       -> roomList.setAll(roomService.getRoomsSortedByPrice(true));
            case "By Price ↓"       -> roomList.setAll(roomService.getRoomsSortedByPrice(false));
            case "STANDARD"         -> roomList.setAll(roomService.getRoomsByType(RoomType.STANDARD));
            case "DELUXE"           -> roomList.setAll(roomService.getRoomsByType(RoomType.DELUXE));
            case "SUITE"            -> roomList.setAll(roomService.getRoomsByType(RoomType.SUITE));
            default                 -> loadRooms();
        }
        setStatus(roomList.size() + " rooms shown.", false);
    }

    @FXML private void handleClear() { clearForm(); }
    @FXML private void handleBack()  { MainApp.navigateTo("Dashboard.fxml"); }

    private Room createRoomFromForm() {
        int    num   = Integer.parseInt(roomNumberField.getText().trim());
        String desc  = descriptionField.getText().trim();
        double price = Double.parseDouble(priceField.getText().trim());
        Room room = switch (roomTypeCombo.getValue()) {
            case DELUXE -> new DeluxeRoom(num);
            case SUITE  -> new SuiteRoom(num);
            default     -> new StandardRoom(num);
        };
        room.setDescription(desc);
        room.setPricePerNight(price);
        return room;
    }

    private void populateForm(Room room) {
        if (room == null) return;
        roomNumberField.setText(String.valueOf(room.getRoomNumber()));
        roomTypeCombo.setValue(room.getRoomType());
        descriptionField.setText(room.getDescription());
        priceField.setText(String.valueOf(room.getPricePerNight()));
    }

    private void clearForm() {
        roomNumberField.clear(); descriptionField.clear(); priceField.clear();
        roomTypeCombo.setValue(RoomType.STANDARD);
        roomsTable.getSelectionModel().clearSelection();
    }

    private boolean validateForm() {
        try {
            int num = Integer.parseInt(roomNumberField.getText().trim());
            if (num <= 0) throw new NumberFormatException();
            double price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) throw new NumberFormatException();
            if (descriptionField.getText().isBlank()) throw new IllegalArgumentException();
        } catch (Exception e) {
            setStatus("Fill all fields correctly (Room# and Price must be > 0).", true);
            return false;
        }
        return true;
    }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill:#e74c3c;" : "-fx-text-fill:#27ae60;");
    }
}
