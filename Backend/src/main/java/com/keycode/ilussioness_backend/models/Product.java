package com.keycode.ilussioness_backend.models;

public abstract class Product {
    protected String productId;
    protected double price;
    protected String photo;
    protected String materials;
    protected String status;

    public Product() {}

    public Product(String productId, double price, String photo, String materials, String status) {
        this.productId = productId;
        this.price = price;
        this.photo = photo;
        this.materials = materials;
        this.status = status;
    }

    protected void updateStock() {
        // actualizar stock
    }

    protected void getDetails() {
        // obtener detalles
    }

    public double getPrice() {
        return this.price;
    }

    // Getters y Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public void setPrice(double price) { this.price = price; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getMaterials() { return materials; }
    public void setMaterials(String materials) { this.materials = materials; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}