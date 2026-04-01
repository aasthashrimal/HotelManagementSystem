package com.hotel.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDAO {

    public int addService(int bookingId, String type, String desc, double amount, Integer staffId) {
        String sql = "INSERT INTO services (booking_id, service_type, description, amount, status, staff_id) VALUES (?,?,?,?,'PENDING',?)";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, bookingId);
            ps.setString(2, type);
            ps.setString(3, desc);
            ps.setDouble(4, amount);
            if (staffId == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, staffId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<Map<String, Object>> getServicesByBooking(int bookingId) {
        List<Map<String, Object>> services = new ArrayList<>();
        String sql = """
            SELECT s.*, st.name AS handler_name
            FROM services s
            LEFT JOIN staff st ON s.staff_id = st.staff_id
            WHERE s.booking_id = ?
            ORDER BY s.service_id DESC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("service_id", rs.getInt("service_id"));
                row.put("service_type", rs.getString("service_type"));
                row.put("description", rs.getString("description"));
                row.put("amount", rs.getDouble("amount"));
                row.put("status", rs.getString("status"));
                int staffId = rs.getInt("staff_id");
                row.put("staff_id", rs.wasNull() ? null : staffId);
                row.put("handler_name", rs.getString("handler_name"));
                services.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return services;
    }

    public double getTotalServiceCharges(int bookingId) {
        String sql = "SELECT SUM(amount) FROM services WHERE booking_id = ? AND status != 'CANCELLED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean updateServiceStatus(int serviceId, String status) {
        String sql = "UPDATE services SET status=? WHERE service_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, serviceId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateService(int serviceId, String type, String desc, double amount, String status, Integer staffId) {
        String sql = "UPDATE services SET service_type=?, description=?, amount=?, status=?, staff_id=? WHERE service_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type);
            ps.setString(2, desc);
            ps.setDouble(3, amount);
            ps.setString(4, status);
            if (staffId == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, staffId);
            ps.setInt(6, serviceId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
