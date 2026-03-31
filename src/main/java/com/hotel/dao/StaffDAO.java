package com.hotel.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.hotel.model.Staff;

public class StaffDAO {

    public List<Staff> getAllStaff() {
        List<Staff> list = new ArrayList<>();
        String sql = "SELECT * FROM staff ORDER BY staff_id";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int addStaff(Staff s) {
        String sql = "INSERT INTO staff (name, role, phone, current_task, on_duty) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, s.getName());
            ps.setString(2, s.getRole().name());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getCurrentTask());
            ps.setInt(5, s.isOnDuty() ? 1 : 0);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateStaff(Staff s) {
        String sql = "UPDATE staff SET name=?, role=?, phone=?, current_task=?, on_duty=? WHERE staff_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getName());
            ps.setString(2, s.getRole().name());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getCurrentTask());
            ps.setInt(5, s.isOnDuty() ? 1 : 0);
            ps.setInt(6, s.getStaffId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteStaff(int id) {
        String sql = "DELETE FROM staff WHERE staff_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean assignTask(int staffId, String task) {
        String sql = "UPDATE staff SET current_task=? WHERE staff_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, task);
            ps.setInt(2, staffId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDutyStatus(int staffId, boolean onDuty) {
        String sql = "UPDATE staff SET on_duty=? WHERE staff_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, onDuty ? 1 : 0);
            ps.setInt(2, staffId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Staff mapRow(ResultSet rs) throws SQLException {
        Staff s = new Staff(
            rs.getInt("staff_id"),
            rs.getString("name"),
            Staff.Role.valueOf(rs.getString("role")),
            rs.getString("phone")
        );
        s.setCurrentTask(rs.getString("current_task"));
        s.setOnDuty(rs.getInt("on_duty") == 1);
        return s;
    }
}
