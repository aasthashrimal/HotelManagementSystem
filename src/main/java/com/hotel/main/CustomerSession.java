package com.hotel.main;

import com.hotel.model.Customer;

public class CustomerSession {
    private static Customer loggedInCustomer;

    public static void setLoggedInCustomer(Customer customer) {
        loggedInCustomer = customer;
    }

    public static Customer getLoggedInCustomer() {
        return loggedInCustomer;
    }
    
    public static void clearSession() {
        loggedInCustomer = null;
    }
}
