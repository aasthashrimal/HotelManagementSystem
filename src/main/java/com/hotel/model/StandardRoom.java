package com.hotel.model;

/**
 * WEEK 1 - INHERITANCE & POLYMORPHISM:
 * StandardRoom extends Room, implements Amenities interface
 */
public class StandardRoom extends Room implements Amenities {

    public StandardRoom(int roomNumber) {
        super(roomNumber, RoomType.STANDARD);
    }

    public StandardRoom(int roomNumber, String description) {
        super(roomNumber, RoomType.STANDARD, description);
    }

    // WEEK 1 - Polymorphism: overriding abstract method
    @Override
    public double calculateTariff(int nights) {
        // WEEK 2 - Autoboxing/Unboxing with wrapper classes
        Double base = getPricePerNight(); // autoboxing
        Integer n = nights;               // autoboxing
        double total = base * n;          // unboxing
        return total;
    }

    @Override
    public String getRoomInfo() {
        return "[STANDARD] " + super.getRoomInfo();
    }

    // WEEK 1 - Interface implementation
    @Override public void listAmenities() { System.out.println("WiFi, AC, TV, Bathroom"); }
    @Override public boolean hasWifi() { return true; }
    @Override public boolean hasAC() { return true; }
    @Override public boolean hasTV() { return true; }
    @Override public String getAmenitiesSummary() { return "WiFi, AC, TV"; }
}
