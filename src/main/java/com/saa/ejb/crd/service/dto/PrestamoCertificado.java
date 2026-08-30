package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Préstamo del partícipe, como lo ve la pantalla de certificados (tipos 3 y 4).
 * {@code numeroCredito} es PRSTIDAS (número de operación ASOPREP) o PRSTCDGO si no tiene.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class PrestamoCertificado {

    /** PRST.PRSTCDGO — es lo que se manda en idPrestamo. */
    private Long idPrestamo;
    private Long numeroCredito;
    /** PRDC.PRDCNMBR ("EMERGENTE"). */
    private String producto;
    /** Como se imprime: "Crédito Emergente". */
    private String productoTexto;
    private LocalDate fecha;
    /** PRSTIDST. */
    private Long estado;
    private String estadoTexto;
    /** PRSTIDST IN (3, 4, 5). */
    private boolean cancelado;

    public PrestamoCertificado() {
    }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public Long getNumeroCredito() { return numeroCredito; }
    public void setNumeroCredito(Long numeroCredito) { this.numeroCredito = numeroCredito; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public String getProductoTexto() { return productoTexto; }
    public void setProductoTexto(String productoTexto) { this.productoTexto = productoTexto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getEstadoTexto() { return estadoTexto; }
    public void setEstadoTexto(String estadoTexto) { this.estadoTexto = estadoTexto; }

    public boolean isCancelado() { return cancelado; }
    public void setCancelado(boolean cancelado) { this.cancelado = cancelado; }
}
