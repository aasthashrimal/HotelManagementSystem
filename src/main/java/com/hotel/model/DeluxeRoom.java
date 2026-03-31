package com.hotel.model;

/**
 * WEEK 1 - INHERITANCE: DeluxeRoom extends Room
 * WEEK 1 - POLYMORPHISM: Different tariff calculation
 */
public class DeluxeRoom extends Room implements Amenities {

    private boolean hasBalcony;

    public DeluxeRoom(int roomNumber) {
        super(roomNumber, RoomType.DELUXE);
        this.hasBalcony = true;
    }

    public DeluxeRoom(int roomNumber, boolean hasBalcony) {
        super(roomNumber, RoomType.DELUXE);
        this.hasBalcony = hasBalcony;
    }

    // WEEK 1 - Polymorphism: Deluxe adds 10% weekend surcharge simulation
    @Override
    public double calculateTariff(int nights) {
        Double base = getPricePerNight(); // WEEK 2 - autoboxing
        double total = base * nights;
        // Add balcony surcharge if applicable
        if (hasBalcony) total += 200 * nights;
        return total;
    }

    @Override
    public String getRoomInfo() {
        return "[DELUXE] " + super.getRoomInfo() + (hasBalcony ? " + Balcony" : "");
    }

    @Override public void listAmenities() { System.out.println("WiFi, AC, TV, Mini-bar, Balcony"); }
    @Override public boolean hasWifi() { return true; }
    @Override public boolean hasAC() { return true; }
    @Override public boolean hasTV() { return true; }
    @Override public String getAmenitiesSummary() { return "WiFi, AC, TV, Mini-bar, Balcony"; }

    public boolean isHasBalcony() { return hasBalcony; }
    public void setHasBalcony(boolean hasBalcony) { this.hasBalcony = hasBalcony; }
}
