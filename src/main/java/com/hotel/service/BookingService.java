package com.hotel.service;

import java.time.LocalDate;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.CustomerDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.Customer;
import com.hotel.model.Room;
import com.hotel.util.EmailUtil;
import com.hotel.util.FileLogger;

/**
 * WEEK 3 - MULTITHREADING: Email threads, cleaning thread
 * WEEK 4 - SYNCHRONIZATION: synchronized booking + checkout
 * ADVANCE BOOKING: room availability is purely date-based — no permanent flag
 */
public class BookingService {

    private final BookingDAO     bookingDAO     = new BookingDAO();
    private final RoomDAO        roomDAO        = new RoomDAO();
    private final CustomerDAO    customerDAO    = new CustomerDAO();
    private final InvoiceService invoiceService = new InvoiceService();

    /**
     * ADVANCE BOOKING: Books a room for any dates (present or future).
     * Does NOT permanently mark room unavailable.
     * WEEK 4 - synchronized: only one thread can book at a time
     */
    public synchronized BookingResult bookRoom(int customerId, int roomNumber,
                                                LocalDate checkIn, LocalDate checkOut) {
        try {
            // WEEK 4 - synchronized overlap check
            if (bookingDAO.isRoomBooked(roomNumber, checkIn, checkOut)) {
                return new BookingResult(false,
                        "Room " + roomNumber + " is already booked for "
                        + checkIn + " to " + checkOut + "!", -1);
            }

            Room     room     = roomDAO.getRoomByNumber(roomNumber);
            Customer customer = customerDAO.getCustomerById(customerId);

            if (room     == null) return new BookingResult(false, "Room not found!", -1);
            if (customer == null) return new BookingResult(false, "Customer not found!", -1);

            int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights <= 0)
                return new BookingResult(false, "Check-out must be after check-in.", -1);

            // WEEK 1 - Polymorphism: correct subclass calculateTariff
            double roomCharge  = room.calculateTariff(nights);
            double tax         = roomCharge * 0.12;
            double totalAmount = roomCharge + tax;

            Booking booking = new Booking(customerId, roomNumber, checkIn, checkOut);
            booking.setTotalAmount(totalAmount);
            booking.setStatus("CONFIRMED");

            int bookingId = bookingDAO.addBooking(booking);
            if (bookingId < 0)
                return new BookingResult(false, "Database error saving booking.", -1);

            // WEEK 5 - File I/O: log booking
            FileLogger.logBooking("BOOKED Room=" + roomNumber + " Customer=" + customerId
                    + " Dates=" + checkIn + "/" + checkOut + " ID=" + bookingId);

            // WEEK 3 - Confirmation email on new thread (no invoice yet)
            if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                String subject = "Booking Confirmed - Aurelia Suites (#" + bookingId + ")";
                String body    = EmailUtil.buildBookingConfirmationBody(
                        customer.getName(), bookingId, roomNumber,
                        room.getRoomType().name(),
                        checkIn.toString());
                EmailUtil.sendEmailAsync(customer.getEmail(), subject, body); // WEEK 3
            }

            return new BookingResult(true,
                    "Booking confirmed! ID: " + bookingId + "  (" + checkIn + " to " + checkOut + ")",
                    bookingId);

        } catch (Exception e) {
            FileLogger.logError("Booking error: " + e.getMessage());
            return new BookingResult(false, "Unexpected error: " + e.getMessage(), -1);
        }
    }

    /**
     * Checkout with actual date (may differ from planned checkout).
     * Recalculates amount, generates PDF, emails guest.
     * WEEK 4 - synchronized
     */
    public synchronized CheckoutResult checkOut(int bookingId, LocalDate actualCheckOut) {
        Booking booking = bookingDAO.getBookingById(bookingId);
        if (booking == null)
            return new CheckoutResult(false, "Booking not found.", null);

        bookingDAO.updateCheckoutDate(bookingId, actualCheckOut);
        booking.setCheckOut(actualCheckOut);

        int nights = booking.getNumberOfNights();
        if (nights <= 0) nights = 1;

        Room   room        = roomDAO.getRoomByNumber(booking.getRoomNumber());
        double finalAmount = 0;
        if (room != null) {
            double roomCharge = room.calculateTariff(nights); // WEEK 1 - Polymorphism
            finalAmount = roomCharge + roomCharge * 0.12;
        }
        booking.setTotalAmount(finalAmount);
        bookingDAO.updateTotalAmount(bookingId, finalAmount);
        bookingDAO.updateStatus(bookingId, "CHECKED_OUT");

        FileLogger.logBooking("CHECKOUT ID=" + bookingId
                + " Room=" + booking.getRoomNumber()
                + " ActualOut=" + actualCheckOut); // WEEK 5

        // Generate PDF invoice
        InvoiceService invoiceService = new InvoiceService();
        InvoiceService.InvoiceResult invoiceResult = invoiceService.generateInvoice(bookingId);

        // WEEK 3 - Email PDF invoice on new thread
        Customer customer = customerDAO.getCustomerById(booking.getCustomerId());
        if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
            String subject = "Your Invoice - Aurelia Suites (Booking #" + bookingId + ")";
                String body    = EmailUtil.buildCheckoutBody(customer.getName());
            EmailUtil.sendEmailWithPdfAsync(
                    customer.getEmail(), subject, body, invoiceResult.pdfPath); // WEEK 3
        }

        // WEEK 3 - Room cleaning on new thread
        Thread cleanThread = new Thread(() -> {
            try {
                Thread.sleep(2000); // WEEK 3 - sleep()
                FileLogger.logInfo("Room " + booking.getRoomNumber() + " cleaned post-checkout.");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        cleanThread.setDaemon(true);
        cleanThread.setName("Cleaner-Room" + booking.getRoomNumber());
        cleanThread.start(); // WEEK 3 - start()

        return new CheckoutResult(true, "Checkout complete! Invoice emailed.", invoiceResult);
    }

    public boolean cancelBooking(int bookingId) {
        Booking booking = bookingDAO.getBookingById(bookingId);
        if (booking == null) return false;
        boolean ok = bookingDAO.updateStatus(bookingId, "CANCELLED");
        if (ok) {
            FileLogger.logBooking("CANCELLED BookingID=" + bookingId);
            // Send cancellation email with invoice PDF
            Customer customer = customerDAO.getCustomerById(booking.getCustomerId());
            if (customer != null && customer.getEmail() != null) {
                String subject = "Booking Cancellation Confirmation - Aurelia Suites";
                String body = EmailUtil.buildCancellationBody(
                    customer.getName(),
                    booking.getBookingId(),
                    booking.getRoomNumber(),
                    booking.getCheckIn().toString()
                );
                // Generate cancellation invoice PDF
                InvoiceService.InvoiceResult invoiceResult = invoiceService.generateInvoice(bookingId);
                String pdfPath = invoiceResult != null ? invoiceResult.pdfPath : null;
                // Send email with PDF attachment
                if (pdfPath != null) {
                    EmailUtil.sendEmailWithPdfAsync(customer.getEmail(), subject, body, pdfPath);
                } else {
                    EmailUtil.sendEmailAsync(customer.getEmail(), subject, body);
                }
            }
        }
        return ok;
    }

    // Result wrappers
    public static class BookingResult {
        public final boolean success;
        public final String  message;
        public final int     bookingId;
        public BookingResult(boolean s, String m, int id) { success=s; message=m; bookingId=id; }
    }

    public static class CheckoutResult {
        public final boolean                      success;
        public final String                       message;
        public final InvoiceService.InvoiceResult invoice;
        public CheckoutResult(boolean s, String m, InvoiceService.InvoiceResult i) { success=s; message=m; invoice=i; }
    }
}
