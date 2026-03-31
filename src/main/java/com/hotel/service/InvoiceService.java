package com.hotel.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.CustomerDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.dao.ServiceDAO;
import com.hotel.model.Amenities;
import com.hotel.model.Booking;
import com.hotel.model.Customer;
import com.hotel.model.Room;
import com.hotel.util.FileLogger;
import com.hotel.util.PdfInvoiceGenerator;

/**
 * WEEK 5 - FILE I/O: Invoice generation + PDF saving + CSV export
 * WEEK 8 - Collections: Iterator used for services list
 */
public class InvoiceService {

    private final BookingDAO  bookingDAO  = new BookingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final RoomDAO     roomDAO     = new RoomDAO();
    private final ServiceDAO  serviceDAO  = new ServiceDAO();

    /**
     * Generates PDF invoice + text preview for the UI.
     * @return InvoiceResult containing both the preview string and PDF path
     */
    public InvoiceResult generateInvoice(int bookingId) {
        try {
            Booking  booking  = bookingDAO.getBookingById(bookingId);
            Customer customer = booking != null ? customerDAO.getCustomerById(booking.getCustomerId()) : null;
            Room     room     = booking != null ? roomDAO.getRoomByNumber(booking.getRoomNumber()) : null;

            if (booking == null || customer == null || room == null) {
                return new InvoiceResult("Error: booking / customer / room data not found.", null);
            }

            // WEEK 8 - Collections: list of services
            List<Map<String, Object>> services = serviceDAO.getServicesByBooking(bookingId);

            // WEEK 5 - File I/O: generate PDF
            String pdfPath = PdfInvoiceGenerator.generatePdf(booking, customer, room, services);

            // Build text preview for the UI TextArea
            String preview = buildTextPreview(booking, customer, room, services);

            return new InvoiceResult(preview, pdfPath);
        } catch (Exception e) {
            FileLogger.logError("Invoice generation failed for booking " + bookingId + ": " + e.getMessage());
            return new InvoiceResult("Unable to generate invoice right now. Please verify booking data and try again.", null);
        }
    }

    // ── Text preview for the app's TextArea ──────────────────────────────────

    private String buildTextPreview(Booking b, Customer c, Room room,
                                     List<Map<String, Object>> services) {
        int    nights     = b.getNumberOfNights();
        if (nights < 1 && b.getCheckIn() != null) {
            LocalDate end = b.getCheckOut() != null ? b.getCheckOut() : LocalDate.now();
            nights = (int) ChronoUnit.DAYS.between(b.getCheckIn(), end);
        }
        if (nights < 1) nights = 1;

        // WEEK 1 - Polymorphism: correct subclass calculateTariff
        double roomCharge = room.calculateTariff(nights);
        double svcCharge  = services.stream().mapToDouble(s -> toAmount(s.get("amount"))).sum();
        double subtotal   = roomCharge + svcCharge;
        double gst        = subtotal * 0.12;
        double total      = subtotal + gst;

        DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String line  = "=".repeat(62);
        String thin  = "-".repeat(62);

        StringBuilder sb = new StringBuilder();
        sb.append(line).append("\n");
        sb.append("                  AURELIA SUITES - INVOICE\n");
        sb.append(line).append("\n");
        sb.append(String.format("Invoice #  : INV-%04d%n", b.getBookingId()));
        sb.append(String.format("Date       : %s%n", LocalDate.now().format(fmt)));
        sb.append(String.format("Status     : %s%n", b.getStatus()));
        sb.append(thin).append("\n");
        sb.append("GUEST\n");
        sb.append(String.format("  Name     : %s%n", c.getName()));
        sb.append(String.format("  Phone    : %s%n", c.getPhone()));
        sb.append(String.format("  Email    : %s%n", c.getEmail() == null ? "-" : c.getEmail()));
        sb.append(thin).append("\n");
        sb.append("STAY\n");
        sb.append(String.format("  Room     : #%d  (%s)%n", b.getRoomNumber(), room.getRoomType()));

        // Safe amenities display
        if (room instanceof Amenities a) {
            sb.append(String.format("  Amenities: %s%n", a.getAmenitiesSummary()));
        }
        sb.append(String.format("  Check-In : %s%n", formatDateOrDash(b.getCheckIn(), fmt)));
        sb.append(String.format("  Check-Out: %s%n", formatDateOrDash(b.getCheckOut(), fmt)));
        sb.append(String.format("  Nights   : %d%n", nights));
        sb.append(thin).append("\n");
        sb.append("CHARGES\n");
        sb.append(String.format("  %-26s Rs.%,10.2f%n",
                "Room (" + nights + " x Rs." + String.format("%.0f", room.getPricePerNight()) + ")",
                roomCharge));

        if (!services.isEmpty()) {
            // WEEK 8 - Iterator
            Iterator<Map<String, Object>> it = services.iterator();
            while (it.hasNext()) {
                Map<String, Object> svc = it.next();
                sb.append(String.format("  %-26s Rs.%,10.2f%n",
                        String.valueOf(svc.get("service_type")), toAmount(svc.get("amount"))));
            }
        }

        sb.append(thin).append("\n");
        sb.append(String.format("  %-26s Rs.%,10.2f%n", "Subtotal", subtotal));
        sb.append(String.format("  %-26s Rs.%,10.2f%n", "GST (12%)", gst));
        sb.append(line).append("\n");
        sb.append(String.format("  %-26s Rs.%,10.2f%n", "GRAND TOTAL", total));
        sb.append(line).append("\n");
        sb.append("  Thank you for choosing Aurelia Suites!\n");
        if (total > 0) {
            sb.append("  PDF invoice saved to: invoices/invoice_")
              .append(b.getBookingId()).append(".pdf\n");
        }
        sb.append(line).append("\n");

        return sb.toString();
    }

    private double toAmount(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private String formatDateOrDash(LocalDate date, DateTimeFormatter fmt) {
        return date == null ? "-" : date.format(fmt);
    }

    // ── CSV revenue report (WEEK 5 - File I/O) ───────────────────────────────

    public void exportRevenueReport() {
        List<Booking> bookings = bookingDAO.getAllBookings();
        StringBuilder csv = new StringBuilder();
        csv.append("Booking ID,Customer,Room,Check-In,Check-Out,Amount,Status\n");

        Iterator<Booking> it = bookings.iterator(); // WEEK 8 - Iterator
        while (it.hasNext()) {
            Booking b = it.next();
            csv.append(b.getBookingId()).append(",")
               .append(b.getCustomerName()).append(",")
               .append(b.getRoomNumber()).append(",")
               .append(b.getCheckIn()).append(",")
               .append(b.getCheckOut()).append(",")
               .append(b.getTotalAmount()).append(",")
               .append(b.getStatus()).append("\n");
        }

        FileLogger.exportCSV("revenue_report_" + LocalDate.now(), csv.toString());
    }

    // ── Result wrapper ────────────────────────────────────────────────────────

    public static class InvoiceResult {
        public final String preview;   // text for UI TextArea
        public final String pdfPath;   // path to generated PDF (null if failed)

        public InvoiceResult(String preview, String pdfPath) {
            this.preview = preview;
            this.pdfPath = pdfPath;
        }
    }
}
