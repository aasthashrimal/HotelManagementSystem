package com.hotel.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hotel.dao.RoomDAO;
import com.hotel.model.Room;
import com.hotel.model.RoomType;

/**
 * WEEK 8 - COLLECTIONS: sorting and filtering rooms
 * ADVANCE BOOKING: all availability checks are now date-based
 */
public class RoomService {

    private final RoomDAO roomDAO = new RoomDAO();

    public List<Room> getAllRooms() {
        // Enrich each room with active booking status for admin room table.
        List<Room> rooms = roomDAO.getAllRooms();
        for (Room r : rooms) {
            boolean hasActiveBooking = roomDAO.hasActiveBooking(r.getRoomNumber());
            r.setAvailable(!hasActiveBooking);
        }
        return rooms;
    }

    /**
     * ADVANCE BOOKING: rooms available for a specific date range.
     * Used by the booking form.
     */
    public List<Room> getAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut) {
        return roomDAO.getAvailableRoomsForDates(checkIn, checkOut);
    }

    /** Available TODAY — used for dashboard stats */
    public List<Room> getAvailableRoomsToday() {
        return roomDAO.getAvailableRoomsToday();
    }

    public List<Room> getAvailableRooms() {
        List<Room> rooms = getAllRooms();
        rooms.removeIf(r -> !r.isAvailable());
        return rooms;
    }

    public List<Room> getOccupiedRooms() {
        List<Room> rooms = getAllRooms();
        rooms.removeIf(Room::isAvailable);
        return rooms;
    }

    // WEEK 8 - Sorting by price
    public List<Room> getRoomsSortedByPrice(boolean ascending) {
        List<Room> rooms = getAllRooms();
        rooms.sort((r1, r2) -> ascending
                ? Double.compare(r1.getPricePerNight(), r2.getPricePerNight())
                : Double.compare(r2.getPricePerNight(), r1.getPricePerNight()));
        return rooms;
    }

    public List<Room> getRoomsSortedByNumber() {
        List<Room> rooms = getAllRooms();
        rooms.sort(Comparator.comparingInt(Room::getRoomNumber));
        return rooms;
    }

    // WEEK 8 - Filter by type using Iterator
    public List<Room> getRoomsByType(RoomType type) {
        List<Room> all = getAllRooms();
        List<Room> filtered = new ArrayList<>();
        Iterator<Room> it = all.iterator(); // WEEK 8 - Iterator
        while (it.hasNext()) {
            Room r = it.next();
            if (r.getRoomType() == type) filtered.add(r);
        }
        return filtered;
    }

    // WEEK 8 - HashMap: room number → Room
    public Map<Integer, Room> getRoomsMap() {
        Map<Integer, Room> map = new HashMap<>(); // WEEK 8 - HashMap
        for (Room r : getAllRooms()) map.put(r.getRoomNumber(), r);
        return map;
    }

    public boolean addRoom(Room room)       { return roomDAO.addRoom(room); }
    public boolean updateRoom(Room room)    { return roomDAO.updateRoom(room); }
    public boolean deleteRoom(int num)      { return roomDAO.deleteRoom(num); }
    public Room getRoomByNumber(int num)    { return roomDAO.getRoomByNumber(num); }

    public int getAvailableCount() { return getAvailableRoomsToday().size(); }
    public int getOccupiedCount()  { return getAllRooms().size() - getAvailableCount(); }
    public int getTotalCount()     { return roomDAO.getAllRooms().size(); }
}
