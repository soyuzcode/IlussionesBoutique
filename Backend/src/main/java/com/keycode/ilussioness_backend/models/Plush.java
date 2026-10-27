
package com.keycode.ilussioness_backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "plush")
public class Plush extends Product {

    private String size;
    private String model;

    public Plush() {
        super();
    }

    public Plush(
            UUID productId,
            BigDecimal price,
            String photo,
            String materials,
            String status,
            String size,
            String model) {

        super(productId, price, photo, materials, status);

        this.size = size;
        this.model = model;
    }

    public boolean checkAvailability() {
        return true;
    }

    public void selectDesign() {
        // Seleccionar diseño.
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
