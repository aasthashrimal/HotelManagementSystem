package com.hotel.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.ServiceDAO;
import com.hotel.dao.StaffDAO;
import com.hotel.main.MainApp;
import com.hotel.model.Booking;
import com.hotel.model.Staff;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * WEEK 9 - JavaFX: Services module
 * Food ordering, cleaning requests, maintenance
 */
public class ServicesController {

    @FXML private ComboBox<Booking> bookingCombo;
    @FXML private ComboBox<String> serviceTypeCombo;
    @FXML private ComboBox<String> updateStatusCombo;
    @FXML private ComboBox<Staff> assignedStaffCombo;
    @FXML private TextField descriptionField;
    @FXML private TextField customServiceTypeField;
    @FXML private TextField amountField;
    @FXML private TextField serviceIdField;
    @FXML private TableView<ServiceRow> servicesTable;
    @FXML private TableColumn<ServiceRow, Number> colServiceId;
    @FXML private TableColumn<ServiceRow, String> colServiceType;
    @FXML private TableColumn<ServiceRow, String> colServiceDescription;
    @FXML private TableColumn<ServiceRow, Number> colServiceAmount;
    @FXML private TableColumn<ServiceRow, String> colServiceStatus;
    @FXML private TableColumn<ServiceRow, String> colServiceHandler;
    @FXML private Label totalChargesLabel;
    @FXML private Label staffAvailabilityHintLabel;
    @FXML private Label statusLabel;

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final StaffDAO   staffDAO   = new StaffDAO();

    @FXML
    public void initialize() {
        setupTable();

        List<Booking> active = bookingDAO.getActiveBookings();
        bookingCombo.setItems(FXCollections.observableArrayList(active));
        bookingCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Booking b) { return b == null ? "" : "#" + b.getBookingId() + " - " + b.getCustomerName() + " (Room " + b.getRoomNumber() + ")"; }
            public Booking fromString(String s) { return null; }
        });

        serviceTypeCombo.setItems(FXCollections.observableArrayList(
            "FOOD_DELIVERY", "ROOM_CLEANING", "LAUNDRY", "MAINTENANCE", "EXTRA_BEDDING", "WAKE_UP_CALL", "OTHERS"
        ));
        serviceTypeCombo.setValue("FOOD_DELIVERY");
        serviceTypeCombo.valueProperty().addListener((o, ov, nv) -> toggleCustomTypeField(nv));

        updateStatusCombo.setItems(FXCollections.observableArrayList("PENDING", "IN_PROGRESS", "DONE", "CANCELLED"));
        updateStatusCombo.setValue("PENDING");

        assignedStaffCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Staff s) { return s == null ? "" : "#" + s.getStaffId() + " - " + s.getName() + " (" + s.getRole() + ")"; }
            public Staff fromString(String s) { return null; }
        });
        loadAssignableStaff(null);

        bookingCombo.valueProperty().addListener((o, ov, nv) -> {
            loadServices(nv);
            clearUpdateForm();
        });

        servicesTable.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> populateUpdateForm(nv));
        toggleCustomTypeField(serviceTypeCombo.getValue());
    }

    private void setupTable() {
        colServiceId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().id()));
        colServiceType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type()));
        colServiceDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().description()));
        colServiceAmount.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().amount()));
        colServiceStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));
        colServiceHandler.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().handlerName()));

        // Color-coded status badges for quick scanning
        colServiceStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                String status = item.toUpperCase();
                setText(status);
                setStyle(switch (status) {
                    case "DONE" -> "-fx-text-fill:#166534;-fx-font-weight:bold;-fx-background-color:#dcfce7;";
                    case "IN_PROGRESS" -> "-fx-text-fill:#1e40af;-fx-font-weight:bold;-fx-background-color:#dbeafe;";
                    case "PENDING" -> "-fx-text-fill:#92400e;-fx-font-weight:bold;-fx-background-color:#fef3c7;";
                    case "CANCELLED" -> "-fx-text-fill:#991b1b;-fx-font-weight:bold;-fx-background-color:#fee2e2;";
                    default -> "-fx-text-fill:#475569;-fx-font-weight:bold;-fx-background-color:#e2e8f0;";
                });
            }
        });

        servicesTable.setItems(FXCollections.observableArrayList());
    }

    @FXML
    private void handleAddService() {
        Booking booking = bookingCombo.getValue();
        if (booking == null) { setStatus("Select an active booking.", true); return; }

        String type = resolveServiceType();
        if (type == null || type.isBlank()) {
            setStatus("Select a valid service type.", true);
            return;
        }

        String desc = descriptionField.getText().trim();
        Staff assigned = assignedStaffCombo.getValue();
        if (assigned != null && !isStaffAssignable(assigned.getStaffId(), null)) {
            setStatus("Selected staff is currently busy. Choose available staff or keep it unassigned.", true);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException ignored) {
            setStatus("Enter a valid service amount.", true);
            return;
        }

        int id = serviceDAO.addService(booking.getBookingId(), type, desc, amount,
            assigned == null ? null : assigned.getStaffId());
        if (id > 0) {
            if (assigned != null) {
                staffDAO.assignTask(assigned.getStaffId(), type + " - Booking #" + booking.getBookingId());
            }
            setStatus("Service added! ID: " + id, false);
            clearUpdateForm();
            loadServices(booking);
        } else {
            setStatus("Failed to add service.", true);
        }
    }

    private void loadServices(Booking booking) {
        if (booking == null) {
            servicesTable.getItems().clear();
            totalChargesLabel.setText("Total Service Charges: Rs.0.00");
            return;
        }

        List<Map<String, Object>> services = serviceDAO.getServicesByBooking(booking.getBookingId());
        var rows = FXCollections.<ServiceRow>observableArrayList();
        double total = 0;

        for (Map<String, Object> s : services) {
            double amount = ((Number) s.get("amount")).doubleValue();
            rows.add(new ServiceRow(
                (Integer) s.get("service_id"),
                String.valueOf(s.get("service_type")),
                String.valueOf(s.get("description")),
                amount,
                String.valueOf(s.get("status")),
                (Integer) s.get("staff_id"),
                s.get("handler_name") == null ? "Unassigned" : String.valueOf(s.get("handler_name"))
            ));
            total += amount;
        }

        servicesTable.setItems(rows);
        totalChargesLabel.setText(String.format("Total Service Charges: Rs.%.2f", total));
        loadAssignableStaff(null);
    }

    @FXML
    private void handleUpdateService() {
        Booking booking = bookingCombo.getValue();
        if (booking == null) { setStatus("Select an active booking first.", true); return; }

        String idText = serviceIdField.getText() == null ? "" : serviceIdField.getText().trim();
        if (idText.isEmpty()) { setStatus("Enter a Service ID to update.", true); return; }

        int serviceId;
        try {
            serviceId = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            setStatus("Invalid Service ID.", true);
            return;
        }

        // Single action button: saves service updates, including marking DONE.
        String type = resolveServiceType();
        if (type == null || type.isBlank()) { setStatus("Select a valid service type.", true); return; }

        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException e) {
            setStatus("Enter valid amount.", true);
            return;
        }

        String status = updateStatusCombo.getValue();
        if (status == null || status.isBlank()) status = "PENDING";

        Staff assigned = assignedStaffCombo.getValue();
        ServiceRow existingRow = servicesTable.getItems().stream()
            .filter(r -> r.id() == serviceId)
            .findFirst()
            .orElse(null);

        if (existingRow != null && "DONE".equals(existingRow.status())) {
            setStatus("This service is already DONE and cannot be changed.", true);
            return;
        }

        Integer existingStaffId = existingRow == null ? null : existingRow.staffId();
        Integer newStaffId = assigned == null ? null : assigned.getStaffId();
        
        if (newStaffId == null && !status.equals("PENDING") && !status.equals("CANCELLED")) {
            setStatus("Cannot change status to " + status + " without assigning Staff first.", true);
            return;
        }

        if (newStaffId != null && !isStaffAssignable(newStaffId, serviceId)) {
            setStatus("Selected staff is busy with another service. Complete that first.", true);
            return;
        }

        boolean existsInBooking = serviceDAO.getServicesByBooking(booking.getBookingId())
            .stream()
            .anyMatch(s -> ((Integer) s.get("service_id")) == serviceId);
        if (!existsInBooking) {
            setStatus("Service ID does not belong to selected booking.", true);
            return;
        }

        if (serviceDAO.updateService(serviceId, type, description, amount, status, newStaffId)) {
            // If reassigned, release old handler first
            if (existingStaffId != null && (newStaffId == null || !existingStaffId.equals(newStaffId))) {
                staffDAO.assignTask(existingStaffId, "Idle");
            }

            if (newStaffId != null) {
                if ("DONE".equals(status) || "CANCELLED".equals(status)) {
                    staffDAO.assignTask(newStaffId, "Idle");
                } else {
                    staffDAO.assignTask(newStaffId, type + " - Booking #" + booking.getBookingId());
                }
            }
            setStatus("Service #" + serviceId + " saved.", false);
            loadServices(booking);
        } else {
            setStatus("Failed to save service.", true);
        }
    }

    private String resolveServiceType() {
        String selected = serviceTypeCombo.getValue();
        if (selected == null) return null;
        if (!"OTHERS".equals(selected)) return selected;
        String custom = customServiceTypeField.getText() == null ? "" : customServiceTypeField.getText().trim();
        return custom.isEmpty() ? null : custom.toUpperCase().replace(' ', '_');
    }

    private void toggleCustomTypeField(String type) {
        boolean isOther = "OTHERS".equals(type);
        customServiceTypeField.setDisable(!isOther);
        customServiceTypeField.setVisible(isOther);
        customServiceTypeField.setManaged(isOther);
        if (!isOther) customServiceTypeField.clear();
    }

    private void populateUpdateForm(ServiceRow row) {
        if (row == null) return;
        serviceIdField.setText(String.valueOf(row.id()));

        if (serviceTypeCombo.getItems().contains(row.type())) {
            serviceTypeCombo.setValue(row.type());
        } else {
            serviceTypeCombo.setValue("OTHERS");
            customServiceTypeField.setText(row.type());
        }

        descriptionField.setText(row.description());
        amountField.setText(String.format("%.2f", row.amount()));
        if (updateStatusCombo.getItems().contains(row.status())) {
            updateStatusCombo.setValue(row.status());
        }
        loadAssignableStaff(row.staffId());
        if (row.staffId() != null) {
            assignedStaffCombo.getItems().stream()
                .filter(s -> s.getStaffId() == row.staffId())
                .findFirst()
                .ifPresent(assignedStaffCombo::setValue);
        }
    }

    private void clearUpdateForm() {
        serviceIdField.clear();
        descriptionField.clear();
        amountField.clear();
        updateStatusCombo.setValue("PENDING");
        loadAssignableStaff(null);
        assignedStaffCombo.setValue(null);
        servicesTable.getSelectionModel().clearSelection();
    }

    private void loadAssignableStaff(Integer includeStaffId) {
        List<Staff> candidates = staffDAO.getAllStaff().stream()
            .filter(Staff::isOnDuty)
            .filter(s -> "Idle".equalsIgnoreCase(s.getCurrentTask()) || (includeStaffId != null && s.getStaffId() == includeStaffId))
            .filter(s -> s.getRole() != Staff.Role.MANAGER && s.getRole() != Staff.Role.RECEPTIONIST)
            .collect(Collectors.toList());
        assignedStaffCombo.setItems(FXCollections.observableArrayList(candidates));

        if (staffAvailabilityHintLabel != null) {
            if (candidates.isEmpty()) {
                staffAvailabilityHintLabel.setText("No staff available now. Save as PENDING and assign later.");
                staffAvailabilityHintLabel.setStyle("-fx-text-fill:#b45309; -fx-font-size:11px;");
            } else {
                staffAvailabilityHintLabel.setText("Available staff: " + candidates.size() + " (busy/off-duty staff hidden)");
                staffAvailabilityHintLabel.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");
            }
        }
    }

    private boolean isStaffAssignable(int staffId, Integer serviceIdInEdit) {
        return staffDAO.getAllStaff().stream()
            .filter(s -> s.getStaffId() == staffId)
            .findFirst()
            .map(s -> {
                if (!s.isOnDuty()) return false;
                if ("Idle".equalsIgnoreCase(s.getCurrentTask())) return true;
                // Allow keeping same staff when editing their current service row
                if (serviceIdInEdit != null) {
                    ServiceRow row = servicesTable.getItems().stream()
                        .filter(r -> r.id() == serviceIdInEdit)
                        .findFirst()
                        .orElse(null);
                    return row != null && row.staffId() != null && row.staffId() == staffId;
                }
                return false;
            })
            .orElse(false);
    }

    @FXML 
    private void handleRefresh() {
        Booking b = bookingCombo.getValue();
        loadServices(b);
        setStatus("Services refreshed.", false);
    }
    
    @FXML private void handleBack() { MainApp.navigateTo("Dashboard.fxml"); }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }

    public record ServiceRow(int id, String type, String description, double amount, String status, Integer staffId, String handlerName) {}
}
