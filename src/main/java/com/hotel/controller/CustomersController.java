package com.hotel.controller;

import com.hotel.dao.CustomerDAO;
import com.hotel.main.MainApp;
import com.hotel.model.Customer;
import com.hotel.util.ValidationUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * WEEK 9 - JavaFX: Customers screen
 */
public class CustomersController {

    @FXML private TableView<Customer>          customersTable;
    @FXML private TableColumn<Customer, Number> colId;
    @FXML private TableColumn<Customer, String> colName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, String> colIdProof;

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField idProofField;
    @FXML private TextField searchField;
    @FXML private Label     statusLabel;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // FIX: lambda cell value factories
        colId.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getCustomerId()));
        colName.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getName()));
        colPhone.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPhone()));
        colEmail.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getEmail() == null ? "" : data.getValue().getEmail()));
        colIdProof.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIdProof() == null ? "" : data.getValue().getIdProof()));

        customersTable.setItems(customerList);

        customersTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, o, n) -> populateForm(n)
        );

        loadCustomers();
    }

    private void loadCustomers() {
        customerList.setAll(customerDAO.getAllCustomers());
        setStatus(customerList.size() + " customers loaded.", false);
    }

    @FXML
    private void handleAdd() {
        if (!validateForm()) return;

        Customer c = new Customer(
            nameField.getText().trim(),
            phoneField.getText().trim(),
            emailField.getText().trim()
        );
        c.setIdProof(idProofField.getText().trim());

        int newId = customerDAO.addCustomer(c);
        if (newId > 0) {
            loadCustomers();
            clearForm();
            setStatus("Customer added! ID: " + newId, false);
        } else {
            setStatus("Failed to add customer.", true);
        }
    }

    @FXML
    private void handleUpdate() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a customer to update.", true); return; }
        if (!validateForm()) return;

        selected.setName(nameField.getText().trim());
        selected.setPhone(phoneField.getText().trim());
        selected.setEmail(emailField.getText().trim());
        selected.setIdProof(idProofField.getText().trim());

        if (customerDAO.updateCustomer(selected)) {
            loadCustomers(); clearForm(); setStatus("Customer updated!", false);
        } else {
            setStatus("Update failed.", true);
        }
    }

    @FXML
    private void handleDelete() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a customer to delete.", true); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete customer: " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (customerDAO.deleteCustomer(selected.getCustomerId())) {
                    loadCustomers(); clearForm(); setStatus("Customer deleted.", false);
                } else {
                    setStatus("Cannot delete (may have bookings).", true);
                }
            }
        });
    }

    @FXML
    private void handleSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { loadCustomers(); return; }
        customerList.setAll(customerDAO.searchCustomers(kw));
        setStatus("Found " + customerList.size() + " result(s).", false);
    }

    @FXML private void handleClear() { clearForm(); searchField.clear(); loadCustomers(); }
    @FXML private void handleRefresh() { loadCustomers(); setStatus("Customers list refreshed.", false); }
    @FXML private void handleBack()  { MainApp.navigateTo("Dashboard.fxml"); }

    private void populateForm(Customer c) {
        if (c == null) return;
        nameField.setText(c.getName());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail() == null ? "" : c.getEmail());
        idProofField.setText(c.getIdProof() == null ? "" : c.getIdProof());
    }

    private void clearForm() {
        nameField.clear(); phoneField.clear(); emailField.clear(); idProofField.clear();
        customersTable.getSelectionModel().clearSelection();
    }

    private boolean validateForm() {
        String name  = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (!ValidationUtil.isNotEmpty(name)) {
            setStatus("Name cannot be empty.", true); return false;
        }
        if (!ValidationUtil.isValidPhone(phone)) {
            setStatus("Enter valid 10-digit Indian mobile number.", true); return false;
        }
        if (!email.isEmpty() && !ValidationUtil.isValidEmail(email)) {
            setStatus("Enter a valid email address.", true); return false;
        }
        return true;
    }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }
}
