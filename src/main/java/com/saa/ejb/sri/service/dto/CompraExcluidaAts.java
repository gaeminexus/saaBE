package com.saa.ejb.sri.service.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Una factura de intermediario excluida del ATS (API-DETALLE-ATS.md §4.5, ÍTEM 13/13b de
 * {@code GeneradorAtsServiceImpl}). No va a {@code <compras>}, así que la pantalla la muestra
 * aparte para que el usuario entienda por qué el total no cuadra con la tabla de documentos.
 */
public class CompraExcluidaAts implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idDocumento;
    private String tipoComprobante;
    private String idProv;
    private String proveedor;
    private String numeroDocumento;
    private LocalDate fechaEmision;
    private double subtotal;
    private double montoIva;
    private double total;
    private String motivo;
    private String numeroRetencion;

    public Long getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Long idDocumento) { this.idDocumento = idDocumento; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getIdProv() { return idProv; }
    public void setIdProv(String idProv) { this.idProv = idProv; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getMontoIva() { return montoIva; }
    public void setMontoIva(double montoIva) { this.montoIva = montoIva; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getNumeroRetencion() { return numeroRetencion; }
    public void setNumeroRetencion(String numeroRetencion) { this.numeroRetencion = numeroRetencion; }
}
