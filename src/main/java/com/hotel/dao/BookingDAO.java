package com.hotel.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hotel.model.Booking;
import com.hotel.model.RoomType;

/**
 * DAO for Booking operations.
 * WEEK 8 - Collections: HashMap and ArrayList
 */
public class BookingDAO {

    public int addBooking(Booking b) {
        String sql = "INSERT INTO bookings (customer_id, room_number, check_in, check_out, total_amount, status) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getCustomerId());
            ps.setInt(2, b.getRoomNumber());
            ps.setString(3, b.getCheckIn().toString());
            ps.setString(4, b.getCheckOut().toString());
            ps.setDouble(5, b.getTotalAmount());
            ps.setString(6, b.getStatus());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<>(); // WEEK 8
        String sql = """
            SELECT b.*, c.name as customer_name, r.room_type
            FROM bookings b
            JOIN customers c ON b.customer_id = c.customer_id
            JOIN rooms r ON b.room_number = r.room_number
            ORDER BY b.booking_id DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Booking> getActiveBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = """
            SELECT b.*, c.name as customer_name, r.room_type
            FROM bookings b
            JOIN customers c ON b.customer_id = c.customer_id
            JOIN rooms r ON b.room_number = r.room_number
            WHERE b.status IN ('CONFIRMED','CHECKED_IN')
            ORDER BY b.check_in
        """;
        try (Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Booking getBookingById(int id) {
        String sql = """
            SELECT b.*, c.name as customer_name, r.room_type
            FROM bookings b
            JOIN customers c ON b.customer_id = c.customer_id
            JOIN rooms r ON b.room_number = r.room_number
            WHERE b.booking_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean updateStatus(int bookingId, String status) {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Auto transition: started stays should appear as CHECKED_IN.
     * Future stays remain CONFIRMED.
     */
    public int syncCheckInStatuses() {
        String sql = """
            UPDATE bookings
            SET status = 'CHECKED_IN'
            WHERE status = 'CONFIRMED'
              AND date(check_in) <= date('now')
              AND date(check_out) >= date('now')
        """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean updateCheckoutDate(int bookingId, LocalDate checkOut) {
        String sql = "UPDATE bookings SET check_out = ? WHERE booking_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, checkOut.toString());
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateTotalAmount(int bookingId, double amount) {
        String sql = "UPDATE bookings SET total_amount = ? WHERE booking_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Updates checkout date + recalculated amount when actual checkout happens */
    public boolean updateCheckoutAndAmount(int bookingId, LocalDate checkOut, double amount) {
        String sql = "UPDATE bookings SET check_out = ?, total_amount = ? WHERE booking_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, checkOut.toString());
            ps.setDouble(2, amount);
            ps.setInt(3, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Core advance booking check:
     * Returns true if the room has any CONFIRMED/CHECKED_IN booking
     * whose dates overlap with the requested range.
     * Overlap condition: NOT (existing.checkOut <= newCheckIn OR existing.checkIn >= newCheckOut)
     */
    public boolean isRoomBooked(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT COUNT(*) FROM bookings
            WHERE room_number = ?
            AND status IN ('CONFIRMED','CHECKED_IN')
            AND NOT (check_out <= ? OR check_in >= ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setString(2, checkIn.toString());
            ps.setString(3, checkOut.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // WEEK 8 - HashMap: room → booking count
    public Map<Integer, Integer> getRoomBookingCounts() {
        Map<Integer, Integer> map = new HashMap<>();
        String sql = "SELECT room_number, COUNT(*) as cnt FROM bookings GROUP BY room_number";
        try (Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getInt("room_number"), rs.getInt("cnt"));
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    public double getTotalRevenue() {
        String sql = "SELECT SUM(total_amount) FROM bookings WHERE status != 'CANCELLED'";
        try (Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTodayBookingsCount() {
        String sql = "SELECT COUNT(*) FROM bookings WHERE date(check_in) = date('now') AND status != 'CANCELLED'";
        try (Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public List<Booking> getBookingsByCustomer(int customerId) {
        List<Booking> list = new ArrayList<>();
        String sql = """
            SELECT b.*, c.name as customer_name, r.room_type
            FROM bookings b
            JOIN customers c ON b.customer_id = c.customer_id
            JOIN rooms r ON b.room_number = r.room_number
            WHERE b.customer_id = ?
            ORDER BY b.booking_id DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Future bookings starting today or later */
    public List<Booking> getUpcomingBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = """
            SELECT b.*, c.name as customer_name, r.room_type
            FROM bookings b
            JOIN customers c ON b.customer_id = c.customer_id
            JOIN rooms r ON b.room_number = r.room_number
            WHERE b.status = 'CONFIRMED'
            AND b.check_in >= date('now')
            ORDER BY b.check_in ASC
        """;
        try (Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getInt("booking_id"));
        b.setCustomerId(rs.getInt("customer_id"));
        b.setRoomNumber(rs.getInt("room_number"));
        b.setCheckIn(LocalDate.parse(rs.getString("check_in")));
        b.setCheckOut(LocalDate.parse(rs.getString("check_out")));
        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setStatus(rs.getString("status"));
        b.setCustomerName(rs.getString("customer_name"));
        try { b.setRoomType(RoomType.valueOf(rs.getString("room_type"))); }
        catch (Exception ignored) {}
        return b;
    }
}
