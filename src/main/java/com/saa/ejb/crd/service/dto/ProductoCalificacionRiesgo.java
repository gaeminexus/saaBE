package com.saa.ejb.crd.service.dto;

/**
 * Una fila de {@code GET /cfcr/listado} — P22. Un producto con su configuración de calificación de
 * riesgo vigente, o {@code configuracion == null} si todavía no tiene ninguna (el hueco que la
 * pantalla debe mostrar). Mismo espíritu que {@code ProductoBandas}, sin la dimensión
 * por-vencer/vencido (acá no existe).
 */
public class ProductoCalificacionRiesgo {

    private Long idProducto;
    private String nombreProducto;
    private Long estadoProducto;
    private DetalleConfiguracionCalificacionRiesgo configuracion;

    public ProductoCalificacionRiesgo() {
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Long getEstadoProducto() {
        return estadoProducto;
    }

    public void setEstadoProducto(Long estadoProducto) {
        this.estadoProducto = estadoProducto;
    }

    public DetalleConfiguracionCalificacionRiesgo getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(DetalleConfiguracionCalificacionRiesgo configuracion) {
        this.configuracion = configuracion;
    }
}
