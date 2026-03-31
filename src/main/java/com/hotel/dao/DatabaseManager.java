package com.hotel.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Manager - handles SQLite connection and schema creation
 * JDBC integration with DAO layer
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:hotel.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    /**
     * Creates all tables on first run
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Rooms table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    room_number INTEGER PRIMARY KEY,
                    room_type TEXT NOT NULL,
                    description TEXT,
                    price_per_night REAL NOT NULL,
                    available INTEGER DEFAULT 1
                )
            """);

            // Customers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    email TEXT,
                    id_proof TEXT
                )
            """);

            // Bookings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bookings (
                    booking_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    room_number INTEGER NOT NULL,
                    check_in TEXT NOT NULL,
                    check_out TEXT NOT NULL,
                    total_amount REAL,
                    status TEXT DEFAULT 'CONFIRMED',
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                    FOREIGN KEY (room_number) REFERENCES rooms(room_number)
                )
            """);

            // Staff table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS staff (
                    staff_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    phone TEXT,
                    current_task TEXT DEFAULT 'Idle',
                    on_duty INTEGER DEFAULT 1
                )
            """);

            // Services table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS services (
                    service_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id INTEGER,
                    service_type TEXT NOT NULL,
                    description TEXT,
                    amount REAL DEFAULT 0,
                    status TEXT DEFAULT 'PENDING',
                    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
                )
            """);

            // Backward-compatible schema migration: link services to assigned staff
            try {
                stmt.execute("ALTER TABLE services ADD COLUMN staff_id INTEGER");
            } catch (SQLException ignored) {
                // Column already exists on existing databases
            }

            insertSampleData(conn);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("DB Init Error: " + e.getMessage());
        }
    }

    private static void insertSampleData(Connection conn) throws SQLException {
        // Check if rooms already exist
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM rooms")) {
            if (rs.getInt(1) > 0) return; // Already has data
        }

        // Insert sample rooms
        String insertRoom = "INSERT INTO rooms (room_number, room_type, description, price_per_night, available) VALUES (?,?,?,?,1)";
        try (PreparedStatement ps = conn.prepareStatement(insertRoom)) {
            Object[][] rooms = {
                {101, "STANDARD", "Standard room with garden view", 1500.0},
                {102, "STANDARD", "Standard room with pool view", 1500.0},
                {103, "STANDARD", "Standard room, 1st floor", 1500.0},
                {201, "DELUXE", "Deluxe room with balcony", 2700.0},
                {202, "DELUXE", "Deluxe room, city view", 2500.0},
                {301, "SUITE", "Presidential suite, luxury", 5900.0},
                {302, "SUITE", "Honeymoon suite", 5000.0}
            };
            for (Object[] room : rooms) {
                ps.setInt(1, (int) room[0]);
                ps.setString(2, (String) room[1]);
                ps.setString(3, (String) room[2]);
                ps.setDouble(4, (double) room[3]);
                ps.executeUpdate();
            }
        }

        // Insert sample staff
        String insertStaff = "INSERT INTO staff (name, role, phone) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(insertStaff)) {
            Object[][] staff = {
                {"Rahul Sharma", "MANAGER", "9876543210"},
                {"Priya Singh", "RECEPTIONIST", "9876543211"},
                {"Amit Kumar", "CLEANER", "9876543212"},
                {"Sunita Devi", "CLEANER", "9876543213"}
            };
            for (Object[] s : staff) {
                ps.setString(1, (String) s[0]);
                ps.setString(2, (String) s[1]);
                ps.setString(3, (String) s[2]);
                ps.executeUpdate();
            }
        }

        System.out.println("Sample data inserted.");
    }
}
