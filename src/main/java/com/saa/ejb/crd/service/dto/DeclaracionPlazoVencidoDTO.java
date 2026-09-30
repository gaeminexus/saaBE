package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La declaración completa, para {@code GET /rest/plvn/listar} y {@code GET /rest/plvn/getId/{id}}
 * (§9 del contrato). NUNCA la entidad JPA cruda: {@code DeclaracionPlazoVencido.prestamo} es
 * {@code @ManyToOne} EAGER y arrastra todo el grafo del préstamo al serializar.
 */
public class DeclaracionPlazoVencidoDTO {

    private Long idDeclaracion;
    private Long idPrestamo;
    private String numeroPrestamo;
    private Long estado;
    private String numeroMemorando;
    private LocalDate fechaCorte;
    private String paraNombre;
    private String paraCargo;
    private String ccNombre;
    private String ccCargo;
    private String nombreParticipe;
    private String cedula;
    private String tipoCredito;

    /** Misma forma que {@code GET /rest/plvn/candidatos} (§3). */
    private CandidatoPlazoVencido cuadro;

    /** Null mientras la declaración está en estado 1 DECLARADA. */
    private LiquidacionPlazoVencido liquidacion;

    private String usuarioDeclaracion;
    private LocalDateTime fechaDeclaracion;
    private String usuarioReverso;
    private LocalDateTime fechaReverso;
    private String motivoReverso;

    public Long getIdDeclaracion() { return idDeclaracion; }
    public void setIdDeclaracion(Long idDeclaracion) { this.idDeclaracion = idDeclaracion; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroPrestamo() { return numeroPrestamo; }
    public void setNumeroPrestamo(String numeroPrestamo) { this.numeroPrestamo = numeroPrestamo; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getNumeroMemorando() { return numeroMemorando; }
    public void setNumeroMemorando(String numeroMemorando) { this.numeroMemorando = numeroMemorando; }

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public String getParaNombre() { return paraNombre; }
    public void setParaNombre(String paraNombre) { this.paraNombre = paraNombre; }

    public String getParaCargo() { return paraCargo; }
    public void setParaCargo(String paraCargo) { this.paraCargo = paraCargo; }

    public String getCcNombre() { return ccNombre; }
    public void setCcNombre(String ccNombre) { this.ccNombre = ccNombre; }

    public String getCcCargo() { return ccCargo; }
    public void setCcCargo(String ccCargo) { this.ccCargo = ccCargo; }

    public String getNombreParticipe() { return nombreParticipe; }
    public void setNombreParticipe(String nombreParticipe) { this.nombreParticipe = nombreParticipe; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String tipoCredito) { this.tipoCredito = tipoCredito; }

    public CandidatoPlazoVencido getCuadro() { return cuadro; }
    public void setCuadro(CandidatoPlazoVencido cuadro) { this.cuadro = cuadro; }

    public LiquidacionPlazoVencido getLiquidacion() { return liquidacion; }
    public void setLiquidacion(LiquidacionPlazoVencido liquidacion) { this.liquidacion = liquidacion; }

    public String getUsuarioDeclaracion() { return usuarioDeclaracion; }
    public void setUsuarioDeclaracion(String usuarioDeclaracion) { this.usuarioDeclaracion = usuarioDeclaracion; }

    public LocalDateTime getFechaDeclaracion() { return fechaDeclaracion; }
    public void setFechaDeclaracion(LocalDateTime fechaDeclaracion) { this.fechaDeclaracion = fechaDeclaracion; }

    public String getUsuarioReverso() { return usuarioReverso; }
    public void setUsuarioReverso(String usuarioReverso) { this.usuarioReverso = usuarioReverso; }

    public LocalDateTime getFechaReverso() { return fechaReverso; }
    public void setFechaReverso(LocalDateTime fechaReverso) { this.fechaReverso = fechaReverso; }

    public String getMotivoReverso() { return motivoReverso; }
    public void setMotivoReverso(String motivoReverso) { this.motivoReverso = motivoReverso; }
}
