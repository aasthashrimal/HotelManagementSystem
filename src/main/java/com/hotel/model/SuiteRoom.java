package com.hotel.model;

/**
 * WEEK 1 - INHERITANCE: SuiteRoom extends Room
 * Most expensive room with luxury tariff
 */
public class SuiteRoom extends Room implements Amenities {

    private int numberOfRooms;

    public SuiteRoom(int roomNumber) {
        super(roomNumber, RoomType.SUITE);
        this.numberOfRooms = 2;
    }

    public SuiteRoom(int roomNumber, int numberOfRooms) {
        super(roomNumber, RoomType.SUITE);
        this.numberOfRooms = numberOfRooms;
    }

    // WEEK 1 - Polymorphism: Suite tariff includes luxury tax
    @Override
    public double calculateTariff(int nights) {
        Double base = getPricePerNight(); // WEEK 2 - wrapper class
        double total = base * nights;
        double luxuryTax = total * 0.18; // 18% luxury tax
        return total + luxuryTax;
    }

    @Override
    public String getRoomInfo() {
        return "[SUITE] " + super.getRoomInfo() + " (" + numberOfRooms + " rooms)";
    }

    @Override public void listAmenities() { System.out.println("WiFi, AC, TV, Jacuzzi, Butler, Mini-bar"); }
    @Override public boolean hasWifi() { return true; }
    @Override public boolean hasAC() { return true; }
    @Override public boolean hasTV() { return true; }
    @Override public String getAmenitiesSummary() { return "WiFi, AC, TV, Jacuzzi, Butler, Mini-bar"; }

    public int getNumberOfRooms() { return numberOfRooms; }
    public void setNumberOfRooms(int numberOfRooms) { this.numberOfRooms = numberOfRooms; }
}
