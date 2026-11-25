package com.keycode.ilussioness_backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @NotBlank(message = "orderId is required")
    @Size(max = 64, message = "orderId must be at most 64 characters")
    @Column(name = "order_id")
    private String orderId;

    @NotBlank(message = "eventType is required")
    @Column(name = "event_type")
    private String eventType;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveryDate;

    @ManyToMany
    @JoinTable(name = "order_products", joinColumns = @JoinColumn(name = "order_id"), inverseJoinColumns = @JoinColumn(name = "product_id"))
    private List<Product> products;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Size(max = 20, message = "status must be at most 20 characters")
    @Column(name = "status", length = 20)
    private String status;

    /** A made-to-measure dress; enables the customization fields and the deposit. */
    @Column(name = "custom_order", nullable = false)
    private Boolean customOrder;

    @Size(max = 120, message = "customization must be at most 120 characters")
    @Column(name = "customization", length = 120)
    private String customization;

    @Size(max = 1000, message = "customizationDescription must be at most 1000 characters")
    @Column(name = "customization_description", length = 1000)
    private String customizationDescription;

    @Column(name = "custom_price", precision = 10, scale = 2)
    private BigDecimal customPrice;

    /** Advance payment (abono) received for a custom order. */
    @Column(name = "deposit", precision = 10, scale = 2)
    private BigDecimal deposit;

    @Embedded
    private BodyMeasurements measurements;

    /** True when this order took units from inventory (orders created before stock tracking did not). */
    @JsonIgnore
    @Column(name = "stock_reserved", nullable = false)
    private boolean stockReserved;

    public Order() {
    }

    public Order(
            String orderId,
            String eventType,
            Date deliveryDate,
            List<Product> products) {
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getCustomOrder() {
        return customOrder;
    }

    public void setCustomOrder(Boolean customOrder) {
        this.customOrder = customOrder;
    }

    @PrePersist
    @PreUpdate
    void defaultCustomOrder() {
        if (customOrder == null) {
            customOrder = false;
        }
    }

    @JsonIgnore
    public boolean isCustom() {
        return Boolean.TRUE.equals(customOrder);
    }

    public String getCustomization() {
        return customization;
    }

    public void setCustomization(String customization) {
        this.customization = customization;
    }

    public String getCustomizationDescription() {
        return customizationDescription;
    }

    public void setCustomizationDescription(String customizationDescription) {
        this.customizationDescription = customizationDescription;
    }

    public BigDecimal getCustomPrice() {
        return customPrice;
    }

    public void setCustomPrice(BigDecimal customPrice) {
        this.customPrice = customPrice;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public void setDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    public BodyMeasurements getMeasurements() {
        return measurements;
    }

    public void setMeasurements(BodyMeasurements measurements) {
        this.measurements = measurements;
    }

    /** Inventory products plus the custom dress price. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getTotal() {
        BigDecimal total = isCustom() && customPrice != null ? customPrice : BigDecimal.ZERO;
        if (products != null) {
            for (Product product : products) {
                if (product != null && product.getPrice() != null) {
                    total = total.add(product.getPrice());
                }
            }
        }
        return total;
    }

    /** Amount still owed after the deposit (saldo pendiente). */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getBalanceDue() {
        BigDecimal paid = isCustom() && deposit != null ? deposit : BigDecimal.ZERO;
        return getTotal().subtract(paid).max(BigDecimal.ZERO);
    }

    public boolean isStockReserved() {
        return stockReserved;
    }

    public void setStockReserved(boolean stockReserved) {
        this.stockReserved = stockReserved;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}