package com.hotel.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Optional;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.CustomerDAO;
import com.hotel.main.MainApp;
import com.hotel.model.Booking;
import com.hotel.model.Customer;
import com.hotel.model.Room;
import com.hotel.service.BookingService;
import com.hotel.service.InvoiceService;
import com.hotel.service.RoomService;
import com.hotel.util.FileLogger;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;

/**
 * WEEK 9 - JavaFX: Bookings screen
 * ADVANCE BOOKING: room combo shows only rooms available for selected dates
 */
public class BookingsController {

    @FXML private TableView<Booking>           bookingsTable;
    @FXML private TableColumn<Booking, Number> colId;
    @FXML private TableColumn<Booking, String> colCustomer;
    @FXML private TableColumn<Booking, Number> colRoom;
    @FXML private TableColumn<Booking, String> colCheckIn;
    @FXML private TableColumn<Booking, String> colCheckOut;
    @FXML private TableColumn<Booking, Number> colAmount;
    @FXML private TableColumn<Booking, String> colStatus;

    @FXML private ComboBox<Customer>  customerCombo;
    @FXML private ComboBox<Room>      roomCombo;
    @FXML private DatePicker          checkInPicker;
    @FXML private DatePicker          checkOutPicker;
    @FXML private Label               statusLabel;
    @FXML private Label               pricePreviewLabel;
    @FXML private ComboBox<String>    filterCombo;

    private final BookingService bookingService = new BookingService();
    private final BookingDAO     bookingDAO     = new BookingDAO();
    private final CustomerDAO    customerDAO    = new CustomerDAO();
    private final RoomService    roomService    = new RoomService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupCombos();
        loadBookings();

        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));

        // ADVANCE BOOKING: refresh available rooms whenever dates change
        checkInPicker.valueProperty().addListener((o, ov, nv) -> { refreshRoomComboForDates(); updatePricePreview(); });
        checkOutPicker.valueProperty().addListener((o, ov, nv) -> { refreshRoomComboForDates(); updatePricePreview(); });
        roomCombo.valueProperty().addListener((o, ov, nv) -> updatePricePreview());

        refreshRoomComboForDates();
    }

    private void setupTable() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getBookingId()));
        colCustomer.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getCustomerName())));
        colRoom.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getRoomNumber()));
        colCheckIn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCheckIn()  == null ? "" : d.getValue().getCheckIn().toString()));
        colCheckOut.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCheckOut() == null ? "" : d.getValue().getCheckOut().toString()));
        colAmount.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getTotalAmount()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getStatus())));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "CONFIRMED"   -> "-fx-text-fill: #2563eb; -fx-font-weight: bold;";
                    case "CHECKED_IN"  -> "-fx-text-fill: #0891b2; -fx-font-weight: bold;";
                    case "CHECKED_OUT" -> "-fx-text-fill: #64748b;";
                    case "CANCELLED"   -> "-fx-text-fill: #e74c3c;";
                    default -> "";
                });
            }
        });

        bookingsTable.setItems(bookingList);
    }

    private void setupCombos() {
        customerCombo.setItems(FXCollections.observableArrayList(customerDAO.getAllCustomers()));
        customerCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Customer c) { return c == null ? "" : c.getName() + " (" + c.getPhone() + ")"; }
            public Customer fromString(String s) { return null; }
        });

        roomCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Room r) {
                return r == null ? "" : "#" + r.getRoomNumber() + "  " + r.getRoomType()
                        + "  Rs." + (int) r.getPricePerNight() + "/night";
            }
            public Room fromString(String s) { return null; }
        });
        roomCombo.setOnShowing(e -> refreshRoomComboForDates());

        filterCombo.setItems(FXCollections.observableArrayList(
                "All", "Active", "Upcoming", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"));
        filterCombo.setValue("All");
    }

    /**
     * ADVANCE BOOKING: Refreshes room combo with rooms available for the selected dates.
     */
    private void refreshRoomComboForDates() {
        LocalDate checkIn  = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            roomCombo.setItems(FXCollections.observableArrayList());
            roomCombo.setValue(null);
            roomCombo.setPromptText("Fix dates first");
            return;
        }
        Room selected = roomCombo.getValue();
        var rooms = roomService.getAvailableRoomsForDates(checkIn, checkOut);
        roomCombo.setItems(FXCollections.observableArrayList(rooms));

        if (selected != null) {
            boolean stillAvailable = rooms.stream()
                    .anyMatch(r -> r.getRoomNumber() == selected.getRoomNumber());
            roomCombo.setValue(stillAvailable ? selected : null);
        } else {
            roomCombo.setValue(null);
        }

        roomCombo.setPromptText(rooms.isEmpty()
                ? "No rooms free for these dates"
                : rooms.size() + " room(s) available");
    }

    private void loadBookings() {
        bookingDAO.syncCheckInStatuses();
        bookingList.setAll(bookingDAO.getAllBookings());
    }

    @FXML
    private void handleBook() {
        Customer  customer = customerCombo.getValue();
        Room      room     = roomCombo.getValue();
        LocalDate checkIn  = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        if (customer == null) { setStatus("Please select a customer.", true); return; }
        if (room     == null) { setStatus("Please select an available room.", true); return; }
        if (checkIn  == null || checkOut == null) { setStatus("Please select dates.", true); return; }
        if (!checkOut.isAfter(checkIn)) { setStatus("Check-out must be after check-in.", true); return; }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Booking");
        confirm.setHeaderText("Confirm Booking?");
        confirm.setContentText(
                "Customer : " + customer.getName() + "\n" +
                "Room     : #" + room.getRoomNumber() + " (" + room.getRoomType() + ")\n" +
                "Check-In : " + checkIn + "\n" +
                "Check-Out: " + checkOut + "\n" +
                "Estimate : " + pricePreviewLabel.getText());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            // Final recheck to handle stale dropdown values.
            var latestAvailable = roomService.getAvailableRoomsForDates(checkIn, checkOut);
            boolean stillAvailable = latestAvailable.stream()
                    .anyMatch(r -> r.getRoomNumber() == room.getRoomNumber());
            if (!stillAvailable) {
                roomCombo.setItems(FXCollections.observableArrayList(latestAvailable));
                roomCombo.setValue(null);
                setStatus("Selected room is no longer available. Please select another room.", true);
                return;
            }

            BookingService.BookingResult result = bookingService.bookRoom(
                    customer.getCustomerId(), room.getRoomNumber(), checkIn, checkOut);
            if (result.success) {
                setStatus(result.message + " | Confirmation email sent!", false);
                loadBookings();
                refreshRoomComboForDates();
            } else {
                setStatus(result.message, true);
            }
        });
    }

    @FXML
    private void handleCheckOut() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a booking to check out.", true); return; }
        if (!"CONFIRMED".equals(selected.getStatus()) && !"CHECKED_IN".equals(selected.getStatus())) {
            setStatus("Only CONFIRMED or CHECKED_IN bookings can be checked out.", true); return;
        }

        // Dialog: ask actual checkout date
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Checkout");
        dialog.setHeaderText("Guest: " + selected.getCustomerName()
                + "  |  Room #" + selected.getRoomNumber()
                + "\nPlanned checkout: " + selected.getCheckOut());

        ButtonType checkoutBtn = new ButtonType("Checkout & Generate Invoice", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkoutBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        DatePicker actualOut = new DatePicker(LocalDate.now());
        Label      nightsLbl = new Label();

        actualOut.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null && selected.getCheckIn() != null) {
                long n = java.time.temporal.ChronoUnit.DAYS.between(selected.getCheckIn(), nv);
                nightsLbl.setText(n + " night(s) stayed");
                nightsLbl.setStyle(n > 0 ? "-fx-text-fill:#2563eb;-fx-font-weight:bold;"
                                         : "-fx-text-fill:#e74c3c;");
            }
        });
        actualOut.setValue(LocalDate.now()); // trigger listener

        grid.add(new Label("Check-In Date:"),     0, 0);
        grid.add(new Label(selected.getCheckIn().toString()), 1, 0);
        grid.add(new Label("Actual Checkout:"),   0, 1);
        grid.add(actualOut,                       1, 1);
        grid.add(nightsLbl,                       1, 2);
        grid.add(new Label("PDF invoice will be generated\nand emailed to the guest."), 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> btn == checkoutBtn ? actualOut.getValue() : null);

        Optional<LocalDate> result = dialog.showAndWait();
        result.ifPresent(checkoutDate -> {
            if (!checkoutDate.isAfter(selected.getCheckIn())) {
                setStatus("Checkout date must be after check-in!", true); return;
            }
            BookingService.CheckoutResult cr = bookingService.checkOut(selected.getBookingId(), checkoutDate);
            if (cr.success) {
                loadBookings();
                refreshRoomComboForDates();
                if (cr.invoice != null) {
                    setStatus(cr.message + (cr.invoice.pdfPath != null ? " | Saved: " + cr.invoice.pdfPath : ""), false);
                } else {
                    setStatus(cr.message, false);
                }
            } else {
                setStatus("Checkout failed: " + cr.message, true);
            }
        });
    }

    @FXML
    private void handleCancel() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a booking to cancel.", true); return; }
        if ("CHECKED_OUT".equals(selected.getStatus()) || "CANCELLED".equals(selected.getStatus())) {
            setStatus("Cannot cancel a " + selected.getStatus() + " booking.", true); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel Booking #" + selected.getBookingId() + " for " + selected.getCustomerName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES && bookingService.cancelBooking(selected.getBookingId())) {
                setStatus("Booking cancelled.", false);
                loadBookings();
                refreshRoomComboForDates();
            }
        });
    }

    @FXML
    private void handleGenerateInvoice() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a booking.", true); return; }
        InvoiceService.InvoiceResult invoiceResult = showInvoicePreview(selected);
        if (invoiceResult != null) {
            showInvoicePopup(selected, invoiceResult);
        }
    }

    @FXML
    private void handleFilter() {
        String f = filterCombo.getValue();
        switch (f) {
            case "Active"   -> bookingList.setAll(bookingDAO.getActiveBookings());
            case "Upcoming" -> bookingList.setAll(bookingDAO.getUpcomingBookings());
            case "All"      -> bookingList.setAll(bookingDAO.getAllBookings());
            default         -> bookingList.setAll(
                    bookingDAO.getAllBookings().stream().filter(b -> b.getStatus().equals(f)).toList());
        }
        setStatus(bookingList.size() + " bookings shown.", false);
    }
    
    @FXML
    private void handleRefresh() {
        loadBookings();
        refreshRoomComboForDates();
        setStatus("Bookings synced successfully from DB.", false);
    }

    private InvoiceService.InvoiceResult showInvoicePreview(Booking b) {
        try {
            InvoiceService.InvoiceResult r = invoiceService.generateInvoice(b.getBookingId());
            if (r == null || r.preview == null || r.preview.isBlank()) {
                setStatus("Invoice preview unavailable.", true);
                return null;
            }
            setStatus("Invoice preview generated for Booking #" + b.getBookingId() + ".", false);
            return r;
        } catch (Exception ex) {
            FileLogger.logError("Generate invoice failed for booking " + b.getBookingId() + ": " + ex.getMessage());
            setStatus("Invoice generation failed.", true);
            return null;
        }
    }

    private void showInvoicePopup(Booking booking, InvoiceService.InvoiceResult invoiceResult) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(MainApp.getPrimaryStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Invoice PDF");
        dialog.setHeaderText("Booking #" + booking.getBookingId() + " - " + nvl(booking.getCustomerName()));

        ButtonType previewBtn = new ButtonType("Preview PDF", ButtonBar.ButtonData.LEFT);
        ButtonType downloadBtn = new ButtonType("Save Invoice", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(previewBtn, downloadBtn, ButtonType.CLOSE);

        Label titleLabel = new Label("Invoice PDF Ready");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label pathLabel = new Label(
                invoiceResult.pdfPath == null || invoiceResult.pdfPath.isBlank()
                        ? "PDF location: invoices folder"
                        : "Saved: " + invoiceResult.pdfPath);
        pathLabel.getStyleClass().add("hint-text");

        Label helperLabel = new Label("Click Preview to view the PDF, or Save Invoice to confirm and store it.");
        helperLabel.getStyleClass().add("hint-text");
        helperLabel.setWrapText(true);

        VBox content = new VBox(12, titleLabel, pathLabel, helperLabel);
        content.setPadding(new Insets(16));
        content.setAlignment(Pos.TOP_LEFT);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(220);

        Button previewButton = (Button) dialog.getDialogPane().lookupButton(previewBtn);
        previewButton.setOnAction(e -> {
            e.consume();
            openPdf(invoiceResult.pdfPath);
        });

        Button downloadButton = (Button) dialog.getDialogPane().lookupButton(downloadBtn);
        downloadButton.setOnAction(e -> {
            e.consume();
            if (saveInvoiceAutomatic(booking)) {
                dialog.close();
            }
        });

        dialog.showAndWait();
    }

    private void openPdf(String pdfPath) {
        File pdfFile = getInvoiceFile(pdfPath);
        if (pdfFile == null) {
            setStatus("Invoice PDF file not found.", true);
            return;
        }

        try {
            if (!Desktop.isDesktopSupported()) {
                setStatus("PDF preview is not supported on this system.", true);
                return;
            }
            Desktop.getDesktop().open(pdfFile);
            setStatus("Opened invoice PDF preview.", false);
        } catch (IOException ex) {
            setStatus("Could not open PDF: " + ex.getMessage(), true);
        }
    }

    private boolean saveInvoiceAutomatic(Booking booking) {
        try {
            InvoiceService.InvoiceResult freshInvoice = invoiceService.generateInvoice(booking.getBookingId());
            if (freshInvoice == null || freshInvoice.pdfPath == null || freshInvoice.pdfPath.isBlank()) {
                setStatus("Failed to generate invoice for saving.", true);
                return false;
            }

            File pdfFile = Path.of(freshInvoice.pdfPath).toFile();
            if (!pdfFile.exists()) {
                setStatus("Invoice file not found after generation.", true);
                return false;
            }

            setStatus("Invoice saved to: " + freshInvoice.pdfPath, false);
            return true;
        } catch (Exception ex) {
            setStatus("Save failed: " + ex.getMessage(), true);
            return false;
        }
    }

    private void savePdfCopy(Booking booking, String pdfPath) {
        File pdfFile = getInvoiceFile(pdfPath);
        if (pdfFile == null) {
            setStatus("Invoice PDF file not found.", true);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Download Invoice PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("invoice_" + booking.getBookingId() + ".pdf");

        File downloadsDir = new File(System.getProperty("user.home"), "Downloads");
        if (downloadsDir.exists() && downloadsDir.isDirectory()) {
            chooser.setInitialDirectory(downloadsDir);
        }

        File target = chooser.showSaveDialog(MainApp.getPrimaryStage());
        if (target == null) {
            return;
        }

        try {
            Files.copy(pdfFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            setStatus("Invoice downloaded: " + target.getAbsolutePath(), false);
        } catch (IOException ex) {
            setStatus("Download failed: " + ex.getMessage(), true);
        }
    }

    private File getInvoiceFile(String pdfPath) {
        if (pdfPath == null || pdfPath.isBlank()) {
            return null;
        }
        File file = Path.of(pdfPath).toFile();
        return file.exists() ? file : null;
    }

    private void updatePricePreview() {
        Room r = roomCombo.getValue();
        LocalDate in = checkInPicker.getValue(), out = checkOutPicker.getValue();
        if (r == null || in == null || out == null || !out.isAfter(in)) { pricePreviewLabel.setText(""); return; }
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(in, out);
        double charge = r.calculateTariff(nights);
        double total  = charge + charge * 0.12;
        pricePreviewLabel.setText(String.format("%d night(s)  ×  Rs.%.0f  +  12%% GST  =  Rs.%.2f",
                nights, r.getPricePerNight(), total));
    }

    @FXML private void handleBack() { MainApp.navigateTo("Dashboard.fxml"); }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill:#e74c3c;" : "-fx-text-fill:#2563eb;");
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
