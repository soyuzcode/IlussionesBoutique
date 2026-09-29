package com.keycode.ilussioness_backend.models;

public class Dress extends Product {
    private String measurements;
    private String color;
    private String cutType;
    private boolean requiresChanges;

    public Dress() {
        super();
    }

    public Dress(String productId, double price, String photo, String materials, String status,
                 String measurements, String color, String cutType, boolean requiresChanges) {
        super(productId, price, photo, materials, status);
        this.measurements = measurements;
        this.color = color;
        this.cutType = cutType;
        this.requiresChanges = requiresChanges;
    }

    public void registerSize() {
        // registrar talla
    }

    public void requestModification() {
        // solicitar modificación
    }

    // Getters y Setters
    public String getMeasurements() { return measurements; }
    public void setMeasurements(String measurements) { this.measurements = measurements; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getCutType() { return cutType; }
    public void setCutType(String cutType) { this.cutType = cutType; }

    public boolean isRequiresChanges() { return requiresChanges; }
    public void setRequiresChanges(boolean requiresChanges) { this.requiresChanges = requiresChanges; }
}