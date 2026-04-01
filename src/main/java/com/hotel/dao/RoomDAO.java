package com.hotel.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.hotel.model.DeluxeRoom;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.model.StandardRoom;
import com.hotel.model.SuiteRoom;

/**
 * DAO for Room operations.
 * ADVANCE BOOKING: availability is now date-based, not a permanent flag.
 * WEEK 8 - Collections: ArrayList used throughout
 */
public class RoomDAO {

    // ── All rooms ─────────────────────────────────────────────────────────────

    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>(); // WEEK 8 - ArrayList
        String sql = "SELECT * FROM rooms ORDER BY room_number";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) rooms.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return rooms;
    }

    /**
     * ADVANCE BOOKING FIX:
     * Returns rooms that have NO confirmed/checked-in booking overlapping [checkIn, checkOut).
     * This allows the same room to be booked for non-overlapping future dates.
     */
    public List<Room> getAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut) {
        List<Room> rooms = new ArrayList<>();
        String sql = """
            SELECT * FROM rooms
            WHERE room_number NOT IN (
                SELECT room_number FROM bookings
                WHERE status IN ('CONFIRMED', 'CHECKED_IN')
                AND check_in  < ?
                AND check_out > ?
            )
            ORDER BY room_number
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, checkOut.toString());
            ps.setString(2, checkIn.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) rooms.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return rooms;
    }

    /**
     * Returns rooms available TODAY (for dashboard/rooms screen display).
     */
    public List<Room> getAvailableRoomsToday() {
        return getAvailableRoomsForDates(LocalDate.now(), LocalDate.now().plusDays(1));
    }

    public Room getRoomByNumber(int roomNumber) {
        String sql = "SELECT * FROM rooms WHERE room_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean addRoom(Room room) {
        String sql = "INSERT INTO rooms (room_number, room_type, description, price_per_night, available) VALUES (?,?,?,?,1)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType().name());
            ps.setString(3, room.getDescription());
            ps.setDouble(4, room.getPricePerNight());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateRoom(Room room) {
        String sql = "UPDATE rooms SET room_type=?, description=?, price_per_night=? WHERE room_number=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomType().name());
            ps.setString(2, room.getDescription());
            ps.setDouble(3, room.getPricePerNight());
            ps.setInt(4, room.getRoomNumber());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteRoom(int roomNumber) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete services belonging to bookings of this room
                String findBookingsSql = "SELECT booking_id FROM bookings WHERE room_number = ?";
                try (PreparedStatement fb = conn.prepareStatement(findBookingsSql)) {
                    fb.setInt(1, roomNumber);
                    ResultSet rs = fb.executeQuery();
                    while (rs.next()) {
                        int bId = rs.getInt(1);
                        try (PreparedStatement ds = conn.prepareStatement("DELETE FROM services WHERE booking_id = ?")) {
                            ds.setInt(1, bId);
                            ds.executeUpdate();
                        }
                    }
                }

                // Delete bookings
                try (PreparedStatement db = conn.prepareStatement("DELETE FROM bookings WHERE room_number = ?")) {
                    db.setInt(1, roomNumber);
                    db.executeUpdate();
                }

                // Delete room
                boolean success;
                try (PreparedStatement dr = conn.prepareStatement("DELETE FROM rooms WHERE room_number = ?")) {
                    dr.setInt(1, roomNumber);
                    success = dr.executeUpdate() > 0;
                }

                conn.commit();
                return success;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ADVANCE BOOKING FIX:
     * Checks if a room is occupied RIGHT NOW (for display purposes only).
     * Does NOT use the available flag — uses bookings table.
     */
    public boolean isRoomOccupiedToday(int roomNumber) {
        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        String sql = """
            SELECT COUNT(*) FROM bookings
            WHERE room_number = ?
            AND status IN ('CONFIRMED','CHECKED_IN')
            AND check_in  < ?
            AND check_out > ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setString(2, tomorrow.toString());
            ps.setString(3, today.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Returns true if the room has any active booking.
     * Active booking = CONFIRMED or CHECKED_IN.
     */
    public boolean hasActiveBooking(int roomNumber) {
        String sql = """
            SELECT COUNT(*) FROM bookings
            WHERE room_number = ?
            AND status IN ('CONFIRMED','CHECKED_IN')
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ── WEEK 1 - Polymorphism: creates correct Room subclass ─────────────────
    private Room mapRow(ResultSet rs) throws SQLException {
        int    number = rs.getInt("room_number");
        String type   = rs.getString("room_type");
        String desc   = rs.getString("description");
        double price  = rs.getDouble("price_per_night");

        Room room = switch (RoomType.valueOf(type)) {
            case DELUXE -> new DeluxeRoom(number);
            case SUITE  -> new SuiteRoom(number);
            default     -> new StandardRoom(number);
        };
        room.setDescription(desc);
        room.setPricePerNight(price);
        // ADVANCE BOOKING: available flag set dynamically from bookings, not DB column
        room.setAvailable(true); // will be overridden by date-based check where needed
        return room;
    }
}
