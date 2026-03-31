package com.hotel.util;

/**
 * WEEK 7 - GENERICS:
 * Generic Pair class - used to pair Room with Customer
 * Demonstrates generic class with bounded type
 */
public class Pair<A, B> {

    // WEEK 7 - Generic fields
    private A first;
    private B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() { return first; }
    public B getSecond() { return second; }

    @Override
    public String toString() {
        return "Pair{" + first + " ↔ " + second + "}";
    }

    // WEEK 7 - Generic static method
    public static <T> void display(T item) {
        System.out.println("Item: " + item.toString());
    }

    // WEEK 7 - Bounded generic method: only works with Number subclasses
    public static <T extends Number> double sumPrices(T price1, T price2) {
        return price1.doubleValue() + price2.doubleValue();
    }
}
