package com.hotel.controller;

import com.hotel.dao.StaffDAO;
import com.hotel.main.MainApp;
import com.hotel.model.Staff;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * WEEK 9 - JavaFX: Staff management screen
 * WEEK 3 - Multithreading: dispatching service tasks to threads
 */
public class StaffController {

    @FXML private TableView<Staff>              staffTable;
    @FXML private TableColumn<Staff, Number>    colId;
    @FXML private TableColumn<Staff, String>    colName;
    @FXML private TableColumn<Staff, String>    colRole;
    @FXML private TableColumn<Staff, String>    colPhone;
    @FXML private TableColumn<Staff, String>    colTask;
    @FXML private TableColumn<Staff, String>    colDuty;

    @FXML private TextField             nameField;
    @FXML private ComboBox<Staff.Role>  roleCombo;
    @FXML private TextField             phoneField;
    @FXML private Label                 statusLabel;

    private final StaffDAO staffDAO = new StaffDAO();
    private final ObservableList<Staff> staffList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // FIX: lambda cell value factories
        colId.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getStaffId()));
        colName.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getName()));
        colRole.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getRole().name()));
        colPhone.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPhone() == null ? "" : data.getValue().getPhone()));
        colTask.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCurrentTask() == null ? "Idle" : data.getValue().getCurrentTask()));
        colDuty.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isOnDuty() ? "On Duty" : "Off Duty"));

        colDuty.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("On Duty".equals(item)
                    ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                    : "-fx-text-fill: #7f8c8d;");
            }
        });

        staffTable.setItems(staffList);
        staffTable.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> populateForm(nv));

        roleCombo.setItems(FXCollections.observableArrayList(Staff.Role.values()));
        roleCombo.setValue(Staff.Role.RECEPTIONIST);

        loadStaff();
    }

    private void loadStaff() {
        staffList.setAll(staffDAO.getAllStaff());
    }

    @FXML
    private void handleAdd() {
        String name  = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        if (name.isEmpty() || phone.isEmpty()) { setStatus("Fill all fields.", true); return; }

        Staff s  = new Staff(name, roleCombo.getValue(), phone);
        int   id = staffDAO.addStaff(s);
        if (id > 0) { loadStaff(); clearForm(); setStatus("Staff added! ID: " + id, false); }
        else setStatus("Failed to add staff.", true);
    }

    @FXML
    private void handleUpdate() {
        Staff sel = staffTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select a staff to update.", true); return; }

        sel.setName(nameField.getText().trim());
        sel.setRole(roleCombo.getValue());
        sel.setPhone(phoneField.getText().trim());

        if (staffDAO.updateStaff(sel)) { loadStaff(); clearForm(); setStatus("Staff updated!", false); }
        else setStatus("Update failed.", true);
    }

    @FXML
    private void handleDelete() {
        Staff sel = staffTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select a staff to delete.", true); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete staff: " + sel.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES && staffDAO.deleteStaff(sel.getStaffId())) {
                loadStaff(); clearForm(); setStatus("Staff deleted.", false);
            }
        });
    }

    @FXML
    private void handleToggleDuty() {
        Staff sel = staffTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select a staff member.", true); return; }

        boolean nextDuty = !sel.isOnDuty();
        if (staffDAO.updateDutyStatus(sel.getStaffId(), nextDuty)) {
            // If going off-duty, clear active task
            if (!nextDuty) {
                staffDAO.assignTask(sel.getStaffId(), "Idle");
            }
            loadStaff();
            setStatus(sel.getName() + " is now " + (nextDuty ? "On Duty" : "Off Duty") + ".", false);
        } else {
            setStatus("Unable to update duty status.", true);
        }
    }

    private void populateForm(Staff s) {
        if (s == null) return;
        nameField.setText(s.getName());
        roleCombo.setValue(s.getRole());
        phoneField.setText(s.getPhone() == null ? "" : s.getPhone());
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        roleCombo.setValue(Staff.Role.RECEPTIONIST);
        staffTable.getSelectionModel().clearSelection();
    }

    @FXML private void handleBack() { MainApp.navigateTo("Dashboard.fxml"); }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }
}
