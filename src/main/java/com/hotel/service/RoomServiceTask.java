package com.hotel.service;

import com.hotel.dao.StaffDAO;
import com.hotel.util.FileLogger;

/**
 * WEEK 3 - MULTITHREADING: Runnable for room service tasks
 * WEEK 4 - SYNCHRONIZATION: wait() and notify() for room availability
 */
public class RoomServiceTask implements Runnable {

    public enum TaskType {
        CLEANING,
        FOOD_DELIVERY,
        MAINTENANCE,
        LAUNDRY,
        EXTRA_BEDDING,
        ROOM_INSPECTION,
        WAKE_UP_CALL
    }

    private final int roomNumber;
    private final TaskType taskType;
    private final int staffId;
    private final StaffDAO staffDAO = new StaffDAO();

    // WEEK 4 - Shared lock object for room availability signaling
    private static final Object roomLock = new Object();
    private static boolean roomReady = false;

    public RoomServiceTask(int roomNumber, TaskType taskType, int staffId) {
        this.roomNumber = roomNumber;
        this.taskType = taskType;
        this.staffId = staffId;
    }

    @Override
    public void run() {
        String taskName = taskType.name();
        System.out.println(Thread.currentThread().getName() + " → Starting " + taskName + " for room " + roomNumber);
        FileLogger.logInfo(taskName + " started for Room " + roomNumber);

        staffDAO.assignTask(staffId, taskName + " - Room " + roomNumber);

        try {
            // WEEK 3 - sleep() to simulate task duration
            switch (taskType) {
                case CLEANING -> Thread.sleep(3000);
                case FOOD_DELIVERY -> Thread.sleep(1500);
                case MAINTENANCE -> Thread.sleep(5000);
                case LAUNDRY -> Thread.sleep(2200);
                case EXTRA_BEDDING -> Thread.sleep(1800);
                case ROOM_INSPECTION -> Thread.sleep(2500);
                case WAKE_UP_CALL -> Thread.sleep(800);
            }

            // WEEK 3 - yield() to give other threads a chance
            Thread.yield();

            System.out.println(Thread.currentThread().getName() + " → Completed " + taskName + " for room " + roomNumber);
            FileLogger.logInfo(taskName + " completed for Room " + roomNumber);

            // WEEK 4 - notify() to signal room is ready
            synchronized (roomLock) {
                roomReady = true;
                roomLock.notifyAll(); // WEEK 4 - notifyAll()
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            FileLogger.logError("Task interrupted: " + taskName + " for room " + roomNumber);
        } finally {
            staffDAO.assignTask(staffId, "Idle");
        }
    }

    // WEEK 4 - wait() for room to be ready
    public static void waitForRoomReady() throws InterruptedException {
        synchronized (roomLock) {
            while (!roomReady) {
                roomLock.wait(); // WEEK 4 - wait()
            }
            roomReady = false; // reset
        }
    }

    /**
     * WEEK 3 - Dispatch a service task on a new thread
     */
    public static Thread dispatchTask(int roomNumber, TaskType taskType, int staffId) {
        RoomServiceTask task = new RoomServiceTask(roomNumber, taskType, staffId);
        Thread t = new Thread(task);
        t.setName(taskType.name() + "-Room" + roomNumber);
        t.setDaemon(true);
        t.start(); // WEEK 3 - start()
        return t;
    }
}
