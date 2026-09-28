package com.saa.ejb.sri.service.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Una línea de {@code <detalleVentas>} del detalle del ATS (API-DETALLE-ATS.md §4.4). El ATS de
 * ventas va agrupado por (cliente, tipo de comprobante), no por documento -- {@link #idsDocumento}
 * trae los ids de todas las facturas/notas agrupadas en esta línea.
 */
public class VentaAts implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tpIdCliente;
    private String idCliente;
    private String cliente;
    private String tipoComprobante;
    private int numeroComprobantes;
    private List<Long> idsDocumento;
    private double baseNoGraIva;
    private double baseImponible;
    private double baseImpGrav;
    private double montoIva;
    private double montoIce;
    private double valorRetIva;
    private double valorRetRenta;

    public String getTpIdCliente() { return tpIdCliente; }
    public void setTpIdCliente(String tpIdCliente) { this.tpIdCliente = tpIdCliente; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public int getNumeroComprobantes() { return numeroComprobantes; }
    public void setNumeroComprobantes(int numeroComprobantes) { this.numeroComprobantes = numeroComprobantes; }

    public List<Long> getIdsDocumento() { return idsDocumento; }
    public void setIdsDocumento(List<Long> idsDocumento) { this.idsDocumento = idsDocumento; }

    public double getBaseNoGraIva() { return baseNoGraIva; }
    public void setBaseNoGraIva(double baseNoGraIva) { this.baseNoGraIva = baseNoGraIva; }

    public double getBaseImponible() { return baseImponible; }
    public void setBaseImponible(double baseImponible) { this.baseImponible = baseImponible; }

    public double getBaseImpGrav() { return baseImpGrav; }
    public void setBaseImpGrav(double baseImpGrav) { this.baseImpGrav = baseImpGrav; }

    public double getMontoIva() { return montoIva; }
    public void setMontoIva(double montoIva) { this.montoIva = montoIva; }

    public double getMontoIce() { return montoIce; }
    public void setMontoIce(double montoIce) { this.montoIce = montoIce; }

    public double getValorRetIva() { return valorRetIva; }
    public void setValorRetIva(double valorRetIva) { this.valorRetIva = valorRetIva; }

    public double getValorRetRenta() { return valorRetRenta; }
    public void setValorRetRenta(double valorRetRenta) { this.valorRetRenta = valorRetRenta; }
}
