package com.keycode.ilussioness_backend.models;

public class Cup extends Product {
    private int quantity;
    private double capacity;
    private String design;

    public Cup() {
        super();
    }

    public Cup(String productId, double price, String photo, String materials, String status,
               int quantity, double capacity, String design) {
        super(productId, price, photo, materials, status);
        this.quantity = quantity;
        this.capacity = capacity;
        this.design = design;
    }

    public boolean checkStock() {
        return true;
    }

    public void selectDesign() {
        // seleccionar diseño
    }

    // Getters y Setters
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getCapacity() { return capacity; }
    public void setCapacity(double capacity) { this.capacity = capacity; }

    public String getDesign() { return design; }
    public void setDesign(String design) { this.design = design; }
}