package com.hotel.model;

/**
 * WEEK 2 - ENUM:
 * Enum with fields and methods for room types
 * Demonstrates enum with constructor, fields, and calculateCost() method
 */
public enum RoomType {

    STANDARD(1500.0, "Standard Room - Basic amenities"),
    DELUXE(2500.0, "Deluxe Room - Enhanced amenities with city view"),
    SUITE(5000.0, "Suite - Luxury experience with premium amenities");

    // WEEK 2 - Enum fields
    private final double basePrice;
    private final String description;

    // Enum constructor
    RoomType(double basePrice, String description) {
        this.basePrice = basePrice;
        this.description = description;
    }

    // WEEK 2 - Enum method: calculateCost
    public double calculateCost(int nights) {
        // WEEK 2 - Autoboxing: double primitive → Double wrapper used in return
        Double cost = basePrice * nights; // autoboxing
        return cost; // unboxing back to double
    }

    public double getBasePrice() { return basePrice; }
    public String getDescription() { return description; }
}
