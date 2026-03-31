package com.hotel.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WEEK 5 - FILE I/O:
 * Handles logging to files (booking logs, error logs)
 */
public class FileLogger {

    private static final String LOG_DIR = "logs/";
    private static final String BOOKING_LOG = LOG_DIR + "booking.log";
    private static final String ERROR_LOG = LOG_DIR + "error.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        // Create logs directory if not exists
        new File(LOG_DIR).mkdirs();
    }

    // WEEK 5 - Writing to file using FileWriter (File I/O)
    public static void logBooking(String message) {
        writeToFile(BOOKING_LOG, "[BOOKING] " + message);
    }

    public static void logError(String message) {
        writeToFile(ERROR_LOG, "[ERROR] " + message);
    }

    public static void logInfo(String message) {
        writeToFile(BOOKING_LOG, "[INFO] " + message);
    }

    private static void writeToFile(String filePath, String message) {
        // WEEK 5 - File I/O: try-with-resources
        try (FileWriter fw = new FileWriter(filePath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {

            String timestamp = LocalDateTime.now().format(FORMATTER);
            pw.println("[" + timestamp + "] " + message);

        } catch (IOException e) {
            System.err.println("Could not write to log: " + e.getMessage());
        }
    }

    // WEEK 5 - Reading from file
    public static String readLogs(String logType) {
        String filePath = logType.equals("error") ? ERROR_LOG : BOOKING_LOG;
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            return "No logs found.";
        }
        return sb.toString();
    }

    // WEEK 5 - Export report as CSV
    public static void exportCSV(String filename, String content) {
        String path = "reports/" + filename + ".csv";
        new File("reports/").mkdirs();

        try (FileWriter fw = new FileWriter(path)) {
            fw.write(content);
            System.out.println("CSV exported: " + path);
        } catch (IOException e) {
            logError("CSV export failed: " + e.getMessage());
        }
    }

    // WEEK 5 - Save invoice as text file
    public static String saveInvoice(String bookingId, String invoiceContent) {
        String path = "invoices/invoice_" + bookingId + ".txt";
        new File("invoices/").mkdirs();

        try (FileWriter fw = new FileWriter(path)) {
            fw.write(invoiceContent);
            return path;
        } catch (IOException e) {
            logError("Invoice save failed: " + e.getMessage());
            return null;
        }
    }
}
