
package com.keycode.ilussioness_backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "card")
public class Card extends Product {

    private int quantity;
    private String design;

    public Card() {
        super();
    }

    public Card(
            UUID productId,
            BigDecimal price,
            String photo,
            String materials,
            String status,
            int quantity,
            String design) {

        super(productId, price, photo, materials, status);

        this.quantity = quantity;
        this.design = design;
    }

    public double calculatePrintingCost() {
        return 0.0;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDesign() {
        return design;
    }

    public void setDesign(String design) {
        this.design = design;
    }
}
