package com.saa.ejb.crd.service.dto;

/**
 * Un renglón del detalle de {@link ResultadoPrevisualizacionCorrida} — la estimación de un
 * jubilado, SIN escribir nada. Ver API-PAGO-PENSION-COMPLEMENTARIA.md §4bis.
 */
public class DetallePrevisualizacionJubilado {

    private Long idEntidad;
    private String nombre;

    /** Cuántos meses adeudados desde el ancla hasta el mes de la corrida (0 si está al día). */
    private int mesesAdeudados;

    /** Estimado: cuánto absorbería la deuda exigible del préstamo. No sale de la asociación. */
    private double montoACruzar;

    /** Estimado: cuánto saldría como orden de pago hacia tesorería (0 si no tiene certificado). */
    private double montoADinero;

    /** {@code montoACruzar + montoADinero}. */
    private double total;

    /**
     * §4bis del contrato, pedido del usuario 2026-09-04: desglose pensión/seguro médico —
     * cuentas contables distintas (plantilla alterno 35), no un detalle de pantalla.
     *
     * ⛔ {@code valorPensionMensual + valorSeguroMensual == VPPC.valorPagar}. {@code valorPagar}
     * YA INCLUYE el seguro, no se suman aparte.
     */
    private double valorPensionMensual;

    /** El seguro médico de UN mes ({@code VPPC.valorSeguro}), no acumulado. */
    private double valorSeguroMensual;

    /**
     * Pensión acumulada de {@code mesesAdeudados} meses. NOMINAL: {@code mesesAdeudados ×
     * valorPensionMensual}, SIN prorratear si el tope (deuda exigible o saldo) termina
     * recortando {@code total} por debajo de {@code mesesAdeudados × VPPC.valorPagar} — mismo
     * criterio nominal que ya usa {@code PGPC.valorPension} en la corrida real (ítem 7 del
     * reporte del retroactivo). Por eso {@code totalPension + totalSeguro} puede ser MAYOR que
     * {@code total} cuando el último mes queda topado: reflejan lo ADEUDADO, no lo que este
     * llamado puede efectivamente pagar. Cómo prorratear ese último mes entre las dos cuentas
     * es una decisión contable pendiente del usuario — no se inventó un criterio acá.
     */
    private double totalPension;

    /** Seguro médico acumulado — mismo criterio NOMINAL que {@link #totalPension}. */
    private double totalSeguro;

    private boolean tienePrestamo;
    private boolean tieneCertificado;

    /**
     * "COMPLETA" | "SOLO_CRUCE" | "BLOQUEADO" | "AL_DIA" — mismo campo y mismo significado que
     * {@link DetallePagoPension#getParticipacion()}. {@code AL_DIA}: sin meses adeudados a este
     * período (apto=true, no es bloqueo) — corrección 2026-09-05, ver el JavaDoc del par en
     * {@code DetallePagoPension}.
     *
     * ⛔⛔ DEFAULT {@code "BLOQUEADO"}, no {@code null} — mismo motivo que
     * {@code DetallePagoPension}: que un return nuevo olvidado sea visible como bloqueado, no
     * invisible como "sin novedad".
     */
    private String participacion = "BLOQUEADO";

    /** {@code false} si este jubilado no entra en la corrida real. */
    private boolean apto;

    /** Por qué no es apto — {@code null} si {@code apto = true}. */
    private String motivoBloqueo;

    public DetallePrevisualizacionJubilado() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getMesesAdeudados() {
        return mesesAdeudados;
    }

    public void setMesesAdeudados(int mesesAdeudados) {
        this.mesesAdeudados = mesesAdeudados;
    }

    public double getMontoACruzar() {
        return montoACruzar;
    }

    public void setMontoACruzar(double montoACruzar) {
        this.montoACruzar = montoACruzar;
    }

    public double getMontoADinero() {
        return montoADinero;
    }

    public void setMontoADinero(double montoADinero) {
        this.montoADinero = montoADinero;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getValorPensionMensual() {
        return valorPensionMensual;
    }

    public void setValorPensionMensual(double valorPensionMensual) {
        this.valorPensionMensual = valorPensionMensual;
    }

    public double getValorSeguroMensual() {
        return valorSeguroMensual;
    }

    public void setValorSeguroMensual(double valorSeguroMensual) {
        this.valorSeguroMensual = valorSeguroMensual;
    }

    public double getTotalPension() {
        return totalPension;
    }

    public void setTotalPension(double totalPension) {
        this.totalPension = totalPension;
    }

    public double getTotalSeguro() {
        return totalSeguro;
    }

    public void setTotalSeguro(double totalSeguro) {
        this.totalSeguro = totalSeguro;
    }

    public boolean isTienePrestamo() {
        return tienePrestamo;
    }

    public void setTienePrestamo(boolean tienePrestamo) {
        this.tienePrestamo = tienePrestamo;
    }

    public boolean isTieneCertificado() {
        return tieneCertificado;
    }

    public void setTieneCertificado(boolean tieneCertificado) {
        this.tieneCertificado = tieneCertificado;
    }

    public String getParticipacion() {
        return participacion;
    }

    public void setParticipacion(String participacion) {
        this.participacion = participacion;
    }

    public boolean isApto() {
        return apto;
    }

    public void setApto(boolean apto) {
        this.apto = apto;
    }

    public String getMotivoBloqueo() {
        return motivoBloqueo;
    }

    public void setMotivoBloqueo(String motivoBloqueo) {
        this.motivoBloqueo = motivoBloqueo;
    }
}
