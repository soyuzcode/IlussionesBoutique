package com.keycode.ilussioness_backend.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "orders") // por si "order" es una palabra reservada en SQL
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    public Order() {}

    public Order(String eventType, LocalDate deliveryDate) {
        this.eventType = eventType;
        this.deliveryDate = deliveryDate;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
}