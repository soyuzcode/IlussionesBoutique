package com.keycode.ilussioness_backend.models;

public class Album extends Product {
    private String dimensions;
    private String coverMaterial;
    private String size;
    private String design;

    public Album() {
        super();
    }

    public Album(String productId, double price, String photo, String materials, String status,
                 String dimensions, String coverMaterial, String size, String design) {
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
        // seleccionar diseño
    }

    // Getters y Setters
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public String getCoverMaterial() { return coverMaterial; }
    public void setCoverMaterial(String coverMaterial) { this.coverMaterial = coverMaterial; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getDesign() { return design; }
    public void setDesign(String design) { this.design = design; }
}