package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un préstamo EN_MORA con su cuadro calculado a una fecha de corte
 * ({@code GET /rest/plvn/candidatos}, API-PASE-A-PLAZO-VENCIDO.md §3). NO es la entidad JPA: es
 * la vista previa antes de declarar. Se reusa también como {@code cuadro} dentro de
 * {@link DeclaracionPlazoVencidoDTO}, que tiene la misma forma (§9 del contrato).
 */
public class CandidatoPlazoVencido {

    private Long idPrestamo;
    private String numeroPrestamo;
    private String tipoCredito;
    private String nombreParticipe;
    private String cedula;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDate fechaUltimoCobro;
    private LocalDate fechaInicioMora;
    private Double dividendoMensual;
    private Double montoPrestamo;

    private ComponenteCuadroPlazoVencido capital;
    private ComponenteCuadroPlazoVencido interes;
    private ComponenteCuadroPlazoVencido desgravamen;
    private ComponenteCuadroPlazoVencido seguroIncendio;
    private ComponenteCuadroPlazoVencido mora;

    private Double totalCobrado;
    private Double totalPorCobrar;

    private Long cuotasPlazo;
    private Long cuotasCobradas;
    private Long cuotasPendientes;
    private Long cuotasPorVencer;

    /** Cuántas cuotas posteriores al corte tienen desgravamen o incendio > 0 — lo que {@code declarar} pondría en cero. */
    private Long cuotasConSeguroAAnular;

    private boolean valido = true;
    private List<String> inconsistencias = new ArrayList<>();

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroPrestamo() { return numeroPrestamo; }
    public void setNumeroPrestamo(String numeroPrestamo) { this.numeroPrestamo = numeroPrestamo; }

    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String tipoCredito) { this.tipoCredito = tipoCredito; }

    public String getNombreParticipe() { return nombreParticipe; }
    public void setNombreParticipe(String nombreParticipe) { this.nombreParticipe = nombreParticipe; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public LocalDate getFechaUltimoCobro() { return fechaUltimoCobro; }
    public void setFechaUltimoCobro(LocalDate fechaUltimoCobro) { this.fechaUltimoCobro = fechaUltimoCobro; }

    public LocalDate getFechaInicioMora() { return fechaInicioMora; }
    public void setFechaInicioMora(LocalDate fechaInicioMora) { this.fechaInicioMora = fechaInicioMora; }

    public Double getDividendoMensual() { return dividendoMensual; }
    public void setDividendoMensual(Double dividendoMensual) { this.dividendoMensual = dividendoMensual; }

    public Double getMontoPrestamo() { return montoPrestamo; }
    public void setMontoPrestamo(Double montoPrestamo) { this.montoPrestamo = montoPrestamo; }

    public ComponenteCuadroPlazoVencido getCapital() { return capital; }
    public void setCapital(ComponenteCuadroPlazoVencido capital) { this.capital = capital; }

    public ComponenteCuadroPlazoVencido getInteres() { return interes; }
    public void setInteres(ComponenteCuadroPlazoVencido interes) { this.interes = interes; }

    public ComponenteCuadroPlazoVencido getDesgravamen() { return desgravamen; }
    public void setDesgravamen(ComponenteCuadroPlazoVencido desgravamen) { this.desgravamen = desgravamen; }

    public ComponenteCuadroPlazoVencido getSeguroIncendio() { return seguroIncendio; }
    public void setSeguroIncendio(ComponenteCuadroPlazoVencido seguroIncendio) { this.seguroIncendio = seguroIncendio; }

    public ComponenteCuadroPlazoVencido getMora() { return mora; }
    public void setMora(ComponenteCuadroPlazoVencido mora) { this.mora = mora; }

    public Double getTotalCobrado() { return totalCobrado; }
    public void setTotalCobrado(Double totalCobrado) { this.totalCobrado = totalCobrado; }

    public Double getTotalPorCobrar() { return totalPorCobrar; }
    public void setTotalPorCobrar(Double totalPorCobrar) { this.totalPorCobrar = totalPorCobrar; }

    public Long getCuotasPlazo() { return cuotasPlazo; }
    public void setCuotasPlazo(Long cuotasPlazo) { this.cuotasPlazo = cuotasPlazo; }

    public Long getCuotasCobradas() { return cuotasCobradas; }
    public void setCuotasCobradas(Long cuotasCobradas) { this.cuotasCobradas = cuotasCobradas; }

    public Long getCuotasPendientes() { return cuotasPendientes; }
    public void setCuotasPendientes(Long cuotasPendientes) { this.cuotasPendientes = cuotasPendientes; }

    public Long getCuotasPorVencer() { return cuotasPorVencer; }
    public void setCuotasPorVencer(Long cuotasPorVencer) { this.cuotasPorVencer = cuotasPorVencer; }

    public Long getCuotasConSeguroAAnular() { return cuotasConSeguroAAnular; }
    public void setCuotasConSeguroAAnular(Long cuotasConSeguroAAnular) { this.cuotasConSeguroAAnular = cuotasConSeguroAAnular; }

    public boolean isValido() { return valido; }
    public void setValido(boolean valido) { this.valido = valido; }

    public List<String> getInconsistencias() { return inconsistencias; }
    public void setInconsistencias(List<String> inconsistencias) { this.inconsistencias = inconsistencias; }
}
