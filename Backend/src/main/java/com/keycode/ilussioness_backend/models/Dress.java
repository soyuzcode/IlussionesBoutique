
package com.keycode.ilussioness_backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dress")
public class Dress extends Product {

    private String measurements;
    private String color;
    private String cutType;
    private boolean requiresChanges;

    public Dress() {
        super();
    }

    public Dress(
            UUID productId,
            BigDecimal price,
            String photo,
            String materials,
            String status,
            String measurements,
            String color,
            String cutType,
            boolean requiresChanges) {

        super(productId, price, photo, materials, status);

        this.measurements = measurements;
        this.color = color;
        this.cutType = cutType;
        this.requiresChanges = requiresChanges;
    }

    public void registerSize() {
        // Registrar talla.
    }

    public void requestModification() {
        // Solicitar modificación.
    }

    public String getMeasurements() {
        return measurements;
    }

    public void setMeasurements(String measurements) {
        this.measurements = measurements;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCutType() {
        return cutType;
    }

    public void setCutType(String cutType) {
        this.cutType = cutType;
    }

    public boolean isRequiresChanges() {
        return requiresChanges;
    }

    public void setRequiresChanges(boolean requiresChanges) {
        this.requiresChanges = requiresChanges;
    }
}
