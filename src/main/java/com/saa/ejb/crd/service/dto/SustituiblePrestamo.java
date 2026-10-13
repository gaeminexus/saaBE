package com.saa.ejb.crd.service.dto;

/**
 * Un préstamo del partícipe que puede (o no) ser el ORIGINAL de una reestructuración o novación,
 * con su valor de sustitución (VS). Respuesta de {@code GET /prst/sustituibles/{idEntidad}}
 * (API-REESTRUCTURACION-NOVACION.md §2).
 */
public class SustituiblePrestamo {

    private Long idPrestamo;
    private Long idAsoprep;
    private String producto;
    private String familia;
    private Long estado;
    private boolean puede;
    private String motivo;
    private Double valorSustitucion;
    private Desglose desglose;
    private ProductoSugerido productoSugerido;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public Long getIdAsoprep() { return idAsoprep; }
    public void setIdAsoprep(Long idAsoprep) { this.idAsoprep = idAsoprep; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public String getFamilia() { return familia; }
    public void setFamilia(String familia) { this.familia = familia; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public boolean isPuede() { return puede; }
    public void setPuede(boolean puede) { this.puede = puede; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Double getValorSustitucion() { return valorSustitucion; }
    public void setValorSustitucion(Double valorSustitucion) { this.valorSustitucion = valorSustitucion; }

    public Desglose getDesglose() { return desglose; }
    public void setDesglose(Desglose desglose) { this.desglose = desglose; }

    public ProductoSugerido getProductoSugerido() { return productoSugerido; }
    public void setProductoSugerido(ProductoSugerido productoSugerido) { this.productoSugerido = productoSugerido; }

    /** Desglose del VS: seguros = desgravamen + seguro de incendio. */
    public static class Desglose {
        private double capital;
        private double interes;
        private double mora;
        private double seguros;

        public double getCapital() { return capital; }
        public void setCapital(double capital) { this.capital = capital; }

        public double getInteres() { return interes; }
        public void setInteres(double interes) { this.interes = interes; }

        public double getMora() { return mora; }
        public void setMora(double mora) { this.mora = mora; }

        public double getSeguros() { return seguros; }
        public void setSeguros(double seguros) { this.seguros = seguros; }
    }

    /** Producto RESTRUCTURADO o NOVACION de la misma familia que el original (RN9). */
    public static class ProductoSugerido {
        private Long idProducto;
        private String nombre;

        public Long getIdProducto() { return idProducto; }
        public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }
}
