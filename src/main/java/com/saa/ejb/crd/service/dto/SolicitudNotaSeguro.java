package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/** Cuerpo de {@code POST /posg/{idFactura}/nota} (contrato §7). */
public class SolicitudNotaSeguro {

    private Long clase;
    private List<Long> prestamos;
    private String usuario;
    private String aseguradora;
    private String ruc;
    private String numeroPoliza;
    private String numeroDocumento;
    private String claveAcceso;
    private LocalDate fechaEmision;
    private Double valorTotal;

    public Long getClase() { return clase; }
    public void setClase(Long clase) { this.clase = clase; }

    public List<Long> getPrestamos() { return prestamos; }
    public void setPrestamos(List<Long> prestamos) { this.prestamos = prestamos; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getAseguradora() { return aseguradora; }
    public void setAseguradora(String aseguradora) { this.aseguradora = aseguradora; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getNumeroPoliza() { return numeroPoliza; }
    public void setNumeroPoliza(String numeroPoliza) { this.numeroPoliza = numeroPoliza; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }
}
