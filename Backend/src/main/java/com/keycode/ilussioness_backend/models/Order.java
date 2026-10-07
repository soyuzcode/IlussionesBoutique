package com.keycode.ilussioness_backend.models;

import java.util.Date;
import java.util.List;

public class Order {
    private String orderId;
    private String eventType;
    private Date deliveryDate;
    private List<Product> products; // Agregación de Producto (0..* a 1..*) del UML

    public Order() {}

    public Order(String orderId, String eventType, Date deliveryDate, List<Product> products) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.deliveryDate = deliveryDate;
        this.products = products;
    }

    public void processPayment() {
        // registrar pago
    }

    public void removeProduct(Product product) {
        // eliminar producto de la lista
        if (this.products != null) {
            this.products.remove(product);
        }
    }

    // Getters y Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}