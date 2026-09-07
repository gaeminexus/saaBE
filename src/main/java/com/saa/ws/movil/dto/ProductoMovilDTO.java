package com.saa.ws.movil.dto;

/**
 * Producto de crédito recortado para {@code GET /movil/simulador/productos} (§5.3 del
 * contrato). Nunca la entidad {@code Producto} cruda: tiene {@code @ManyToOne} a {@code Filial}
 * y a {@code TipoPrestamo} que la arrastrarían enteras.
 */
public class ProductoMovilDTO {

    private Long codigo;
    private String nombre;
    private String codigoSBS;
    private Long idTipoPrestamo;
    private String nombreTipoPrestamo;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoSBS() {
        return codigoSBS;
    }

    public void setCodigoSBS(String codigoSBS) {
        this.codigoSBS = codigoSBS;
    }

    public Long getIdTipoPrestamo() {
        return idTipoPrestamo;
    }

    public void setIdTipoPrestamo(Long idTipoPrestamo) {
        this.idTipoPrestamo = idTipoPrestamo;
    }

    public String getNombreTipoPrestamo() {
        return nombreTipoPrestamo;
    }

    public void setNombreTipoPrestamo(String nombreTipoPrestamo) {
        this.nombreTipoPrestamo = nombreTipoPrestamo;
    }
}
