package com.keycode.ilussioness_backend.models;

public class Customer {
    private String customerId;
    private String name;
    private String phone;

    public Customer() {}

    public Customer(String customerId, String name, String phone) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
    }

    public void registerCustomer() {
        // registrar cliente
    }

    public void updateData() {
        // actualizar datos
    }

    public void viewHistory() {
        // consultar historial
    }

    // Getters y Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}