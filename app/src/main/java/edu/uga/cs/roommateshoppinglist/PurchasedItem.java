package edu.uga.cs.roommateshoppinglist;

import java.util.List;

public class PurchasedItem {
    private String date;
    private String name;
    private List<String> items;
    private double totalPrice;
    private String key;

    public PurchasedItem() {
        // Default constructor required for calls to DataSnapshot.getValue(PurchasedItem.class)
    }

    public PurchasedItem(String date, String name, List<String> items, double totalPrice) {
        this.date = date;
        this.name = name;
        this.items = items;
        this.totalPrice = totalPrice;
    }

    public String getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public List<String> getItems() {
        return items;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
