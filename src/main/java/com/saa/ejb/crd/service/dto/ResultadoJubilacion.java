package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de procesar la jubilación de un partícipe: el traslado de sus saldos de cesantía
 * y jubilación a pensión complementaria (tipo de aporte 23) y el cambio de estado a JUBILADO
 * COMPLEMENTARIO.
 *
 * No incluye el cruce contra préstamos ni la devolución en efectivo — esos son decisiones
 * previas y opcionales que la pantalla ya ejecutó llamando a
 * {@code ProcesoPagoPrestamoService#pagarConAportes}/{@code DevolucionAporteService} antes de
 * este paso; este resultado es solo el traslado del remanente.
 *
 * Ver LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md §4.b (J1-J7) y el flujo completo de esa sección.
 */
public class ResultadoJubilacion {

    private Long idEntidad;

    /** Fecha de negocio con la que se registró el traslado */
    private LocalDate fecha;

    /** Saldo de cesantía trasladado (0 si no tenía) */
    private Double valorCesantiaTrasladado;

    /** Saldo de jubilación trasladado (0 si no tenía) */
    private Double valorJubilacionTrasladado;

    /** Suma de los dos anteriores — el valor que ingresó a pensión complementaria */
    private Double valorTotalTrasladado;

    /**
     * Los movimientos de CRD.APRT generados: hasta dos NEGATIVOS (cesantía/jubilación, solo
     * los que tenían saldo &gt; 0) y uno POSITIVO (pensión complementaria), todos con
     * {@code tipoMovimiento = JUBILACION}.
     */
    private List<MovimientoAporte> movimientos = new ArrayList<>();

    /** Estado del partícipe después del cambio (JUBILADO_COMPLEMENTARIO) */
    private Long estadoNuevo;

    /**
     * Código del asiento de reclasificación (§3.1 del levantamiento + plantilla alterno 29),
     * o {@code null} si la contabilidad de CRD está apagada.
     */
    private Long numeroAsiento;

    public ResultadoJubilacion() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getValorCesantiaTrasladado() {
        return valorCesantiaTrasladado;
    }

    public void setValorCesantiaTrasladado(Double valorCesantiaTrasladado) {
        this.valorCesantiaTrasladado = valorCesantiaTrasladado;
    }

    public Double getValorJubilacionTrasladado() {
        return valorJubilacionTrasladado;
    }

    public void setValorJubilacionTrasladado(Double valorJubilacionTrasladado) {
        this.valorJubilacionTrasladado = valorJubilacionTrasladado;
    }

    public Double getValorTotalTrasladado() {
        return valorTotalTrasladado;
    }

    public void setValorTotalTrasladado(Double valorTotalTrasladado) {
        this.valorTotalTrasladado = valorTotalTrasladado;
    }

    public List<MovimientoAporte> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(List<MovimientoAporte> movimientos) {
        this.movimientos = movimientos;
    }

    public Long getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(Long estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public Long getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(Long numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }
}
