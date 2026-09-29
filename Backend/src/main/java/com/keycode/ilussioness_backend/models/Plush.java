package com.keycode.ilussioness_backend.models;

public class Plush extends Product {
    private String size;
    private String model;

    public Plush() {
        super();
    }

    public Plush(String productId, double price, String photo, String materials, String status,
                 String size, String model) {
        super(productId, price, photo, materials, status);
        this.size = size;
        this.model = model;
    }

    public boolean checkAvailability() {
        return true;
    }

    public void selectDesign() {
        // seleccionar diseño
    }

    // Getters y Setters
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}