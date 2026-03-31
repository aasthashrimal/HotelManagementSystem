package com.hotel.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hotel.dao.BookingDAO;
import com.hotel.main.MainApp;
import com.hotel.model.Booking;
import com.hotel.service.InvoiceService;
import com.hotel.service.RoomService;
import com.hotel.util.FileLogger;
import com.hotel.util.SerializationUtil;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

/**
 * WEEK 9 - JavaFX: Reports & Analytics
 * WEEK 5 - File export
 * WEEK 6 - Serialization backup
 */
public class ReportsController {

    @FXML private Label totalRevenueLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label activeBookingsLabel;
    @FXML private Label availableRoomsLabel;
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> roomOccupancyChart;
    @FXML private LineChart<String, Number> revenueTrendChart;

    @FXML private TextArea logArea;

    private final BookingDAO     bookingDAO     = new BookingDAO();
    private final RoomService    roomService    = new RoomService();
    private final InvoiceService invoiceService = new InvoiceService();

    @FXML
    public void initialize() {
        loadStats();
        loadStatusChart();
        loadRoomOccupancyChart();
        loadRevenueTrendChart();
    }

    private void loadStats() {
        List<Booking> all    = bookingDAO.getAllBookings();
        long   active        = all.stream().filter(b ->
            "CONFIRMED".equals(b.getStatus()) || "CHECKED_IN".equals(b.getStatus())).count();
        double revenue       = bookingDAO.getTotalRevenue();
        int    totalRooms    = roomService.getAllRooms().size();
        int    available     = roomService.getAvailableCount();

        totalRevenueLabel.setText(String.format("Rs.%,.2f", revenue));
        totalBookingsLabel.setText(String.valueOf(all.size()));
        activeBookingsLabel.setText(String.valueOf(active));
        availableRoomsLabel.setText(available + " / " + totalRooms);
    }

    @FXML
    private void handleExportCSV() {
        invoiceService.exportRevenueReport();
        showAlert("Success", "Revenue report exported to reports/ folder as CSV.");
        FileLogger.logInfo("Revenue report exported.");
    }

    @FXML
    private void handleViewLogs() {
        String logs = FileLogger.readLogs("booking");
        logArea.setText(logs.isEmpty() ? "No logs yet." : logs);
    }

    @FXML
    private void handleViewErrorLogs() {
        String logs = FileLogger.readLogs("error");
        logArea.setText(logs.isEmpty() ? "No error logs." : logs);
    }

    @FXML
    private void handleBackup() {
        List<Booking> bookings = bookingDAO.getAllBookings();
        boolean ok = SerializationUtil.backupBookings(bookings);
        showAlert(ok ? "Backup Successful" : "Backup Failed",
            ok ? "Bookings backed up to backup/bookings_backup.ser" : "Backup failed!");
    }

    @FXML
    private void handleRestoreInfo() {
        List<?> loaded = SerializationUtil.loadBookingsBackup();
        if (loaded != null)
            showAlert("Backup Info", "Backup contains " + loaded.size() + " booking records.");
        else
            showAlert("No Backup", "No backup file found.");
    }

    @FXML
    private void handleRefresh() {
        loadStats();
        loadStatusChart();
        loadRoomOccupancyChart();
        loadRevenueTrendChart();
    }

    @FXML private void handleBack() { MainApp.navigateTo("Dashboard.fxml"); }

    private void loadStatusChart() {
        List<Booking> all = bookingDAO.getAllBookings();
        Map<String, Integer> statusCount = new HashMap<>();
        statusCount.put("Confirmed", 0);
        statusCount.put("Checked In", 0);
        statusCount.put("Checked Out", 0);
        statusCount.put("Cancelled", 0);

        for (Booking b : all) {
            switch (b.getStatus()) {
                case "CONFIRMED" -> statusCount.put("Confirmed", statusCount.get("Confirmed") + 1);
                case "CHECKED_IN" -> statusCount.put("Checked In", statusCount.get("Checked In") + 1);
                case "CHECKED_OUT" -> statusCount.put("Checked Out", statusCount.get("Checked Out") + 1);
                case "CANCELLED" -> statusCount.put("Cancelled", statusCount.get("Cancelled") + 1);
            }
        }

        javafx.collections.ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        statusCount.forEach((k, v) -> {
            if (v > 0) data.add(new PieChart.Data(k + " (" + v + ")", v));
        });
        statusPieChart.setData(data);
        statusPieChart.setTitle("Booking Status Distribution");

        // Add hover tooltips + click highlight for interactivity
        Platform.runLater(() -> {
            for (PieChart.Data d : statusPieChart.getData()) {
                if (d.getNode() == null) continue;
                Tooltip.install(d.getNode(), new Tooltip(d.getName() + ": " + (int) d.getPieValue()));
                d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-opacity: 0.86;"));
                d.getNode().setOnMouseExited(e -> d.getNode().setStyle(""));
                d.getNode().setOnMouseClicked(e -> {
                    String style = d.getNode().getStyle();
                    if (style != null && style.contains("-fx-translate-x")) {
                        d.getNode().setStyle("-fx-opacity: 0.86;");
                    } else {
                        d.getNode().setStyle("-fx-opacity: 0.86; -fx-translate-x: 6; -fx-translate-y: -4;");
                    }
                });
            }
        });
    }

    private void loadRoomOccupancyChart() {
        var roomTypes = new HashMap<String, Integer>();
        var roomByNumber = new HashMap<Integer, String>();
        roomService.getAllRooms().forEach(room -> roomByNumber.put(room.getRoomNumber(), room.getRoomType().name()));

        // Aggregate booking count by room type (exclude cancelled)
        bookingDAO.getAllBookings().stream()
            .filter(b -> !"CANCELLED".equals(b.getStatus()))
            .forEach(b -> {
                String roomType = roomByNumber.getOrDefault(b.getRoomNumber(), "UNKNOWN");
                roomTypes.put(roomType, roomTypes.getOrDefault(roomType, 0) + 1);
            });

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Occupancy");
        roomTypes.forEach((roomType, count) -> 
            series.getData().add(new XYChart.Data<>(roomType, count)));
        
        roomOccupancyChart.getData().clear();
        roomOccupancyChart.getData().add(series);

        // Hover tooltip + subtle lift on bars
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() == null) continue;
                Tooltip.install(d.getNode(), new Tooltip(d.getXValue() + ": " + d.getYValue()));
                d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-scale-x: 1.04; -fx-scale-y: 1.04;"));
                d.getNode().setOnMouseExited(e -> d.getNode().setStyle(""));
            }
        });
    }

    private void loadRevenueTrendChart() {
        Map<String, Double> revenueByDate = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        // Initialize last 7 days with 0 revenue
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            revenueByDate.put(date.toString(), 0.0);
        }
        
        // Sum revenue by date
        bookingDAO.getAllBookings().stream()
            .filter(b -> !b.getStatus().equals("CANCELLED") && b.getCheckOut() != null)
            .forEach(b -> {
                String dateKey = b.getCheckOut().toString();
                if (revenueByDate.containsKey(dateKey)) {
                    revenueByDate.put(dateKey, revenueByDate.get(dateKey) + b.getTotalAmount());
                }
            });

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Revenue");
        revenueByDate.forEach((date, amount) -> 
            series.getData().add(new XYChart.Data<>(date, amount)));
        
        revenueTrendChart.setAnimated(false);
        revenueTrendChart.getData().clear();
        revenueTrendChart.getData().add(series);

        applyRevenueSeriesStyle(series);

        // Tooltip on revenue points
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() == null) continue;
                Tooltip.install(d.getNode(), new Tooltip(d.getXValue() + "\nRevenue: Rs." + String.format("%,.0f", d.getYValue().doubleValue())));
                d.getNode().setOnMouseEntered(e -> {
                    d.getNode().setScaleX(1.15);
                    d.getNode().setScaleY(1.15);
                });
                d.getNode().setOnMouseExited(e -> {
                    d.getNode().setScaleX(1.0);
                    d.getNode().setScaleY(1.0);
                });
            }
        });
    }

    private void applyRevenueSeriesStyle(XYChart.Series<String, Number> series) {
        Platform.runLater(() -> {
            Node line = series.getNode();
            if (line != null) {
                line.setStyle("-fx-stroke: #2f5fb3; -fx-stroke-width: 2.2px;");
            }

            for (XYChart.Data<String, Number> data : series.getData()) {
                Node symbol = data.getNode();
                if (symbol != null) {
                    symbol.setStyle("-fx-background-color: #ffffff, #2f5fb3; -fx-background-insets: 0, 2; -fx-background-radius: 8px; -fx-padding: 4px;");
                }
            }
        });
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
