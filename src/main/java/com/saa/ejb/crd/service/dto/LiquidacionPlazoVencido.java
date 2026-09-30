package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** El bloque {@code liquidacion} de {@link DeclaracionPlazoVencidoDTO} (§9 del contrato). */
public class LiquidacionPlazoVencido {

    private LocalDate fechaCorte;
    private Double saldoCapital;
    private Double interesVencido;
    private Double desgravamen;
    private Double seguroIncendio;
    private Double mora;
    private Double total;
    private Long cuotasImpagas;
    private String usuario;
    private LocalDateTime fecha;

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public Double getSaldoCapital() { return saldoCapital; }
    public void setSaldoCapital(Double saldoCapital) { this.saldoCapital = saldoCapital; }

    public Double getInteresVencido() { return interesVencido; }
    public void setInteresVencido(Double interesVencido) { this.interesVencido = interesVencido; }

    public Double getDesgravamen() { return desgravamen; }
    public void setDesgravamen(Double desgravamen) { this.desgravamen = desgravamen; }

    public Double getSeguroIncendio() { return seguroIncendio; }
    public void setSeguroIncendio(Double seguroIncendio) { this.seguroIncendio = seguroIncendio; }

    public Double getMora() { return mora; }
    public void setMora(Double mora) { this.mora = mora; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Long getCuotasImpagas() { return cuotasImpagas; }
    public void setCuotasImpagas(Long cuotasImpagas) { this.cuotasImpagas = cuotasImpagas; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
