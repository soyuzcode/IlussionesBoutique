
package com.keycode.ilussioness_backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "album")
public class Album extends Product {

    private String dimensions;
    private String coverMaterial;
    private String size;
    private String design;

    public Album() {
        super();
    }

    public Album(
            UUID productId,
            BigDecimal price,
            String photo,
            String materials,
            String status,
            String dimensions,
            String coverMaterial,
            String size,
            String design) {

        super(productId, price, photo, materials, status);

        this.dimensions = dimensions;
        this.coverMaterial = coverMaterial;
        this.size = size;
        this.design = design;
    }

    public boolean checkAvailability() {
        return true;
    }

    public void selectDesign() {
        // Seleccionar diseño.
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public String getCoverMaterial() {
        return coverMaterial;
    }

    public void setCoverMaterial(String coverMaterial) {
        this.coverMaterial = coverMaterial;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDesign() {
        return design;
    }

    public void setDesign(String design) {
        this.design = design;
    }
}
