package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de {@code EvaluacionOtorgamientoService.evaluar}: capacidad de pago (≤ 50 % de la remuneración unificada) y
 * cupo (capital pendiente + monto ≤ 100 % de la cuenta individual). Un solo cálculo para guardar, generar la tabla,
 * aprobar, el simulador de novación y la pantalla (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B1).
 */
public class EvaluacionOtorgamiento {

    private Long idEntidad;
    private Long idPrestamo;
    private Long idPrestamoOriginal;
    private Double monto;

    /** {@code false} si el préstamo no está sujeto a estas reglas (reestructuración, o ya no está en 6/1). */
    private Boolean aplicaReglas = Boolean.TRUE;
    private String motivoNoAplica;

    private Double remuneracion;
    private Double capacidadPago;
    private Double cuotasVigentes;
    private Double cuotaNueva;
    private Boolean capacidadOk;

    private Double saldoCuentaIndividual;
    /** Capital pendiente de TODOS los vigentes (informativo: panel y solicitud V1). */
    private Double saldoCapitalVigentes;
    /** Capital pendiente solo de los vigentes quirografarios y emergentes (PQ/PE): el que usa la regla del cupo. */
    private Double saldoCapitalCupo;
    private Boolean cupoOk;

    /** Uno por regla incumplida, con los números. Vacía si todo cumple. */
    private List<String> mensajes = new ArrayList<>();

    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public Long getIdPrestamoOriginal() { return idPrestamoOriginal; }
    public void setIdPrestamoOriginal(Long idPrestamoOriginal) { this.idPrestamoOriginal = idPrestamoOriginal; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public Boolean getAplicaReglas() { return aplicaReglas; }
    public void setAplicaReglas(Boolean aplicaReglas) { this.aplicaReglas = aplicaReglas; }
    public String getMotivoNoAplica() { return motivoNoAplica; }
    public void setMotivoNoAplica(String motivoNoAplica) { this.motivoNoAplica = motivoNoAplica; }
    public Double getRemuneracion() { return remuneracion; }
    public void setRemuneracion(Double remuneracion) { this.remuneracion = remuneracion; }
    public Double getCapacidadPago() { return capacidadPago; }
    public void setCapacidadPago(Double capacidadPago) { this.capacidadPago = capacidadPago; }
    public Double getCuotasVigentes() { return cuotasVigentes; }
    public void setCuotasVigentes(Double cuotasVigentes) { this.cuotasVigentes = cuotasVigentes; }
    public Double getCuotaNueva() { return cuotaNueva; }
    public void setCuotaNueva(Double cuotaNueva) { this.cuotaNueva = cuotaNueva; }
    public Boolean getCapacidadOk() { return capacidadOk; }
    public void setCapacidadOk(Boolean capacidadOk) { this.capacidadOk = capacidadOk; }
    public Double getSaldoCuentaIndividual() { return saldoCuentaIndividual; }
    public void setSaldoCuentaIndividual(Double saldoCuentaIndividual) { this.saldoCuentaIndividual = saldoCuentaIndividual; }
    public Double getSaldoCapitalVigentes() { return saldoCapitalVigentes; }
    public void setSaldoCapitalVigentes(Double saldoCapitalVigentes) { this.saldoCapitalVigentes = saldoCapitalVigentes; }
    public Double getSaldoCapitalCupo() { return saldoCapitalCupo; }
    public void setSaldoCapitalCupo(Double saldoCapitalCupo) { this.saldoCapitalCupo = saldoCapitalCupo; }
    public Boolean getCupoOk() { return cupoOk; }
    public void setCupoOk(Boolean cupoOk) { this.cupoOk = cupoOk; }
    public List<String> getMensajes() { return mensajes; }
    public void setMensajes(List<String> mensajes) { this.mensajes = mensajes; }
}
