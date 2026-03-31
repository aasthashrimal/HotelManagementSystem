package com.hotel.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * WEEK 6 - Serializable Booking class
 */
public class Booking implements Serializable {

    private static final long serialVersionUID = 3L;

    private int bookingId;
    private int customerId;
    private int roomNumber;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private double totalAmount;
    private String status; // CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    private String customerName;
    private RoomType roomType;

    public Booking() {}

    public Booking(int customerId, int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        this.customerId = customerId;
        this.roomNumber = roomNumber;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = "CONFIRMED";
    }

    public int getNumberOfNights() {
        if (checkIn != null && checkOut != null) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        }
        return 0;
    }

    // Getters and Setters
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    @Override
    public String toString() {
        return "Booking{id=" + bookingId + ", room=" + roomNumber + ", customer=" + customerId
                + ", checkIn=" + checkIn + ", checkOut=" + checkOut + ", status=" + status + "}";
    }
}
