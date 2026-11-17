package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Resultado por jubilado de {@code POST /pgpc/reprocesarPeriodo} (C20). */
public class DetalleReprocesoPension {

    private Long idEntidad;
    /** {@code CRD.PGPC.PGPCCDGO} del período. */
    private Long idPago;
    /** GENERADO, YA_EXISTIA o ERROR. */
    private String estado;
    private double pension;
    private double seguro;
    private double cruzado;
    /** Neto devengado a pagar al jubilado (lo que sale al banco). 0 si quedó retenido (C32). */
    private double neto;
    /**
     * C32 (D-113): pensión neta retenida (PGPCVLRT) por no tener cuenta bancaria activa única con certificado ni fecha
     * de pago del banco. No se debita, no se devenga ni se ordena; la paga pagarRetenidos al registrar la cuenta.
     */
    private double retenido;
    /** Descuento del rol que no se pudo cruzar (sin préstamo, excede la cuota o la pensión). Queda en su aporte 23. */
    private double excedenteNoCruzado;
    private Long idAsientoDevengo;
    private Long idPagoProgramado;
    /**
     * CONFIRMADO (egreso con la fecha del banco), PENDIENTE (orden por aprobar), SIN_PAGO (neto 0), RETENIDO (C32: sin
     * cuenta certificada, sin orden), SIN_CUENTA_CERTIFICADA (C32: PGPC anterior ya devengado, sin salida: no se ordena)
     * o ERROR.
     */
    private String estadoPago;
    private LocalDate fechaPago;
    private Long idAsientoPago;
    private String mensaje;
    private List<String> novedades = new ArrayList<>();
    private List<String> reaperturaAlCorte = new ArrayList<>();

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Long getIdPago() {
        return idPago;
    }

    public void setIdPago(Long idPago) {
        this.idPago = idPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getPension() {
        return pension;
    }

    public void setPension(double pension) {
        this.pension = pension;
    }

    public double getSeguro() {
        return seguro;
    }

    public void setSeguro(double seguro) {
        this.seguro = seguro;
    }

    public double getCruzado() {
        return cruzado;
    }

    public void setCruzado(double cruzado) {
        this.cruzado = cruzado;
    }

    public double getNeto() {
        return neto;
    }

    public void setNeto(double neto) {
        this.neto = neto;
    }

    public double getRetenido() {
        return retenido;
    }

    public void setRetenido(double retenido) {
        this.retenido = retenido;
    }

    public double getExcedenteNoCruzado() {
        return excedenteNoCruzado;
    }

    public void setExcedenteNoCruzado(double excedenteNoCruzado) {
        this.excedenteNoCruzado = excedenteNoCruzado;
    }

    public Long getIdAsientoDevengo() {
        return idAsientoDevengo;
    }

    public void setIdAsientoDevengo(Long idAsientoDevengo) {
        this.idAsientoDevengo = idAsientoDevengo;
    }

    public Long getIdPagoProgramado() {
        return idPagoProgramado;
    }

    public void setIdPagoProgramado(Long idPagoProgramado) {
        this.idPagoProgramado = idPagoProgramado;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Long getIdAsientoPago() {
        return idAsientoPago;
    }

    public void setIdAsientoPago(Long idAsientoPago) {
        this.idAsientoPago = idAsientoPago;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<String> getNovedades() {
        return novedades;
    }

    public void setNovedades(List<String> novedades) {
        this.novedades = novedades;
    }

    public List<String> getReaperturaAlCorte() {
        return reaperturaAlCorte;
    }

    public void setReaperturaAlCorte(List<String> reaperturaAlCorte) {
        this.reaperturaAlCorte = reaperturaAlCorte;
    }
}
