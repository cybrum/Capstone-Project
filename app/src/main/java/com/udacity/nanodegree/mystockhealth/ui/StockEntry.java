package com.udacity.nanodegree.mystockhealth.ui;

/**
 * Created by Ndey on 11/23/2016.
 */

public class StockEntry {
    private String sym;
    private int quantity;
    private double amount;

    public StockEntry() {
    }

    public StockEntry(String sym, int quantity, double amount) {
        this.sym = sym;
        this.quantity = quantity;
        this.amount = amount;
    }

    public String getSym() {
        return sym;
    }

    public void setSym(String sym) {
        this.sym = sym;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
