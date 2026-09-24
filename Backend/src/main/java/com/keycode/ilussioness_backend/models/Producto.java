package com.keycode.ilussioness_backend.models;

public abstract class Producto {
    protected String idProducto;
    protected double precio;
    protected String foto;
    protected String materiales;
    protected String estado;

    public Producto() {}

    public Producto(String idProducto, double precio, String foto, String materiales, String estado) {
        this.idProducto = idProducto;
        this.precio = precio;
        this.foto = foto;
        this.materiales = materiales;
        this.estado = estado;
    }

    public void actualizarStock() {
        // actualizar stock
    }

    public void obtenerDetalles() {
        // obtener detalles
    }

    public double obtenerPrecio() {
        return this.precio;
    }

    // Getters y Setters
    public String getIdProducto() { return idProducto; }
    public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public String getMateriales() { return materiales; }
    public void setMateriales(String materiales) { this.materiales = materiales; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}