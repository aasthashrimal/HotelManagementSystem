package com.hotel.model;

import java.io.Serializable; // Week 6 - Serialization

/**
 * WEEK 1 - OOP CONCEPTS:
 * Abstract class demonstrating Abstraction, Encapsulation, Inheritance base
 * WEEK 6 - Serializable for saving/loading room data
 */
public abstract class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    // WEEK 1 - Encapsulation: private fields with getters/setters
    private int roomNumber;
    private RoomType roomType;     // WEEK 2 - Enum
    private boolean available;
    private String description;
    private double pricePerNight;  // WEEK 2 - will use Double wrapper in billing

    // WEEK 1 - Constructor Overloading
    public Room(int roomNumber, RoomType roomType) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.available = true;
        this.description = roomType.name() + " Room";
        this.pricePerNight = roomType.getBasePrice();
    }

    public Room(int roomNumber, RoomType roomType, String description) {
        this(roomNumber, roomType); // calls above constructor
        this.description = description;
    }

    // WEEK 1 - Abstract method (Abstraction) - each subclass calculates tariff differently
    public abstract double calculateTariff(int nights);

    // WEEK 1 - Polymorphism method to get room info
    public String getRoomInfo() {
        return "Room #" + roomNumber + " [" + roomType + "] - Available: " + available;
    }

    // WEEK 1 - Encapsulation: Getters and Setters
    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    @Override
    public String toString() {
        return "Room{#" + roomNumber + ", type=" + roomType + ", price=" + pricePerNight + ", available=" + available + "}";
    }
}
