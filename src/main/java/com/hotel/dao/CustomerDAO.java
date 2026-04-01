package com.hotel.dao;

import com.hotel.model.Customer;
import java.sql.*;
import java.util.*;

/**
 * DAO for Customer operations
 * WEEK 8 - Collections: ArrayList used throughout
 */
public class CustomerDAO {

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>(); // WEEK 8 - ArrayList
        String sql = "SELECT * FROM customers ORDER BY customer_id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                customers.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public Customer getCustomerById(int id) {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int addCustomer(Customer c) {
        String sql = "INSERT INTO customers (name, phone, email, id_proof, password) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getIdProof());
            ps.setString(5, c.getPassword() != null ? c.getPassword() : "1234");
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateCustomer(Customer c) {
        String sql = "UPDATE customers SET name=?, phone=?, email=?, id_proof=?, password=? WHERE customer_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getIdProof());
            ps.setString(5, c.getPassword() != null ? c.getPassword() : "1234");
            ps.setInt(6, c.getCustomerId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteCustomer(int id) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Begin transaction
            try {
                // Find and delete all services belonging to customer's bookings
                String findBookingsSql = "SELECT booking_id FROM bookings WHERE customer_id = ?";
                try (PreparedStatement fb = conn.prepareStatement(findBookingsSql)) {
                    fb.setInt(1, id);
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
                try (PreparedStatement db = conn.prepareStatement("DELETE FROM bookings WHERE customer_id = ?")) {
                    db.setInt(1, id);
                    db.executeUpdate();
                }

                // Finally delete customer
                boolean success;
                try (PreparedStatement dc = conn.prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
                    dc.setInt(1, id);
                    success = dc.executeUpdate() > 0;
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

    public List<Customer> searchCustomers(String keyword) {
        List<Customer> result = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? OR email LIKE ?";
        String kw = "%" + keyword + "%";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(mapRow(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        // Safe get for password because old schema might not have it before migration strictly ran
        String pwd = "1234";
        try {
            pwd = rs.getString("password");
        } catch (SQLException ignored) { }
        return new Customer(
            rs.getInt("customer_id"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("id_proof"),
            pwd
        );
    }

    public Customer authenticate(String phone, String password) {
        String sql = "SELECT * FROM customers WHERE phone = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, phone);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
