
package com.keycode.ilussioness_backend.models;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Product {

    @Id
    @Column(name = "id", nullable = false)
    private UUID productId;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    protected BigDecimal price;

    @Column(name = "photo_url")
    protected String photo;

    @Column(name = "materials")
    protected String materials;

    // Temporalmente String: abajo explico cómo mapear el enum.
    @Column(name = "status")
    protected String status;

    public Product() {
    }

    public Product(
            UUID productId,
            BigDecimal price,
            String photo,
            String materials,
            String status) {
        this.productId = productId;
        this.price = price;
        this.photo = photo;
        this.materials = materials;
        this.status = status;
    }

    protected void updateStock() {
        // Actualizar stock.
    }

    protected void getDetails() {
        // Obtener detalles.
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getMaterials() {
        return materials;
    }

    public void setMaterials(String materials) {
        this.materials = materials;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
