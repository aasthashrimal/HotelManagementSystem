package com.hotel.util;

import java.io.*;
import java.util.List;

/**
 * WEEK 6 - SERIALIZATION:
 * Utility to save/load objects using Java serialization
 */
public class SerializationUtil {

    private static final String BACKUP_DIR = "backup/";

    static {
        new File(BACKUP_DIR).mkdirs();
    }

    // WEEK 6 - Serialize (save) object to file
    public static <T> boolean save(T object, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(BACKUP_DIR + filename))) {
            oos.writeObject(object);
            FileLogger.logInfo("Serialized: " + filename);
            return true;
        } catch (IOException e) {
            FileLogger.logError("Serialization failed: " + e.getMessage());
            return false;
        }
    }

    // WEEK 6 - Deserialize (load) object from file
    @SuppressWarnings("unchecked")
    public static <T> T load(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(BACKUP_DIR + filename))) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            FileLogger.logError("Deserialization failed: " + e.getMessage());
            return null;
        }
    }

    // WEEK 6 - Backup all bookings
    public static boolean backupBookings(List<?> bookings) {
        return save(bookings, "bookings_backup.ser");
    }

    // WEEK 6 - Load bookings from backup
    public static List<?> loadBookingsBackup() {
        return load("bookings_backup.ser");
    }
}
