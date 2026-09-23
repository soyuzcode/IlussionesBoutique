package models;

public class Vestido extends Producto {
    private String medidas;
    private String color;
    private String tipoCorte;
    private boolean requiereCambios;

    public Vestido() {
        super();
    }

    public Vestido(String idProducto, double precio, String foto, String materiales, String estado,
                   String medidas, String color, String tipoCorte, boolean requiereCambios) {
        super(idProducto, precio, foto, materiales, estado);
        this.medidas = medidas;
        this.color = color;
        this.tipoCorte = tipoCorte;
        this.requiereCambios = requiereCambios;
    }

    public void registrarTalla() {
        // registrar talla
    }

    public void solicitarModificacion() {
        // solicitar modificación
    }

    public String getMedidas() { return medidas; }
    public void setMedidas(String medidas) { this.medidas = medidas; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getTipoCorte() { return tipoCorte; }
    public void setTipoCorte(String tipoCorte) { this.tipoCorte = tipoCorte; }

    public boolean isRequiereCambios() { return requiereCambios; }
    public void setRequiereCambios(boolean requiereCambios) { this.requiereCambios = requiereCambios; }
}