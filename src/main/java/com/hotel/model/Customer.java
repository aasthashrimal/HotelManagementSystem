package com.hotel.model;

import java.io.Serializable; // WEEK 6

/**
 * WEEK 1 - OOP: Customer class with encapsulation
 * WEEK 6 - Serializable
 */
public class Customer implements Serializable {

    private static final long serialVersionUID = 2L;

    private int customerId;
    private String name;
    private String phone;
    private String email;
    private String idProof;   // Optional: Aadhar/PAN/Passport
    private String password;  // WEEK 11 - Customer Portal

    // WEEK 1 - Constructor overloading
    public Customer(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.idProof = "";
        this.password = "1234"; // default
    }

    public Customer(int customerId, String name, String phone, String email, String idProof) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.idProof = idProof;
        this.password = "1234"; // Default for existing rows without it
    }

    public Customer(int customerId, String name, String phone, String email, String idProof, String password) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.idProof = idProof;
        this.password = password;
    }

    // WEEK 1 - Encapsulation: Getters and Setters
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIdProof() { return idProof; }
    public void setIdProof(String idProof) { this.idProof = idProof; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return "Customer{id=" + customerId + ", name=" + name + ", phone=" + phone + "}";
    }
}
