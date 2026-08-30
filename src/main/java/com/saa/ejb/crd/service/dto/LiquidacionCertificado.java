package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Pago de cuenta individual (CRD.HPCS) ofrecido a la pantalla de certificados (tipos 2 y 5).
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class LiquidacionCertificado {

    /** HPCS.HPCSCDGO — es lo que se manda en idLiquidacion. */
    private Long idLiquidacion;
    private LocalDate fechaPago;
    /** J, C, JP, CP, JRV, CRV — ya en mayúsculas. */
    private String tipo;
    private String tipoTexto;
    private Double valor;
    private String observacion;

    public LiquidacionCertificado() {
    }

    public Long getIdLiquidacion() { return idLiquidacion; }
    public void setIdLiquidacion(Long idLiquidacion) { this.idLiquidacion = idLiquidacion; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTipoTexto() { return tipoTexto; }
    public void setTipoTexto(String tipoTexto) { this.tipoTexto = tipoTexto; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
