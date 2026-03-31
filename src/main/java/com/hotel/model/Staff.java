package com.hotel.model;

import java.io.Serializable;

/**
 * WEEK 1 - OOP: Staff class with encapsulation
 * WEEK 6 - Serializable
 */
public class Staff implements Serializable {

    private static final long serialVersionUID = 4L;

    public enum Role {
        RECEPTIONIST,
        CLEANER,
        HOUSEKEEPING,
        ROOM_SERVICE,
        MAINTENANCE,
        SECURITY,
        MANAGER
    }

    private int staffId;
    private String name;
    private Role role;
    private String phone;
    private String currentTask;
    private boolean onDuty;

    public Staff(String name, Role role, String phone) {
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.onDuty = true;
        this.currentTask = "Idle";
    }

    public Staff(int staffId, String name, Role role, String phone) {
        this.staffId = staffId;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.onDuty = true;
        this.currentTask = "Idle";
    }

    // Getters and Setters
    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCurrentTask() { return currentTask; }
    public void setCurrentTask(String currentTask) { this.currentTask = currentTask; }

    public boolean isOnDuty() { return onDuty; }
    public void setOnDuty(boolean onDuty) { this.onDuty = onDuty; }

    @Override
    public String toString() {
        return "Staff{id=" + staffId + ", name=" + name + ", role=" + role + "}";
    }
}
