package com.hotel.model;

/**
 * WEEK 1 - INTERFACE:
 * Interface for room amenities
 */
public interface Amenities {
    void listAmenities();
    boolean hasWifi();
    boolean hasAC();
    boolean hasTV();
    String getAmenitiesSummary();
}
