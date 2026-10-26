package com.keycode.ilussioness_backend.models;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String orderId;

    private String eventType;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveryDate;

    @ManyToMany
    private List<Product> products;

    public Order() {}

    public Order(
        String orderId,
        String eventType,
        Date deliveryDate,
        List<Product> products
    ) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.deliveryDate = deliveryDate;
        this.products = products;
    }

    public void processPayment() {
        // Registrar pago
    }

    public void removeProduct(Product product) {
        if (this.products != null) {
            this.products.remove(product);
        }
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}