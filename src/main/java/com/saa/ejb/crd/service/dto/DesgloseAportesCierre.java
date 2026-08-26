package com.saa.ejb.crd.service.dto;

/**
 * De dónde sale el importe de aportes que el neteo (⑥) reversa como no cobrado.
 *
 * <p>
 * Hasta ahora el sub-proceso devolvía solo el neto y contabilidad no podía ver su origen.
 * Este desglose expone los tres componentes: lo que se esperaba cobrar, lo que se registró
 * y la diferencia entre ambos.
 * </p>
 *
 * <h3>La fórmula, con sus dos matices</h3>
 * <pre>
 *   diferencia  = esperado − registrado        (CON signo)
 *   noCobrado   = max(diferencia, 0)           ← es lo que va al asiento
 *   excesoCobro = max(−diferencia, 0)
 * </pre>
 * <p>
 * <b>El piso en cero esconde información, y por eso está aquí.</b> Cuando lo registrado
 * supera a lo esperado —julio 2026: 156.797 registrados contra 121.161 esperados— la
 * diferencia es negativa y el asiento no la puede llevar: un neteo negativo invertiría el
 * asiento. Eso no es un error, lo resuelve el proceso de cobro en exceso (§3.7 del
 * levantamiento), pero antes desaparecía sin dejar rastro. Ahora sale en
 * {@code excesoCobro} para que se vea.
 * </p>
 *
 * <p>
 * <b>Lo registrado depende de que el archivo Petro del mes esté cargado</b>: las filas de
 * {@code CRD.APRT} las crea la fase 3 de la carga. Ver {@link ControlArchivoPetro}.
 * </p>
 */
public class DesgloseAportesCierre {

    /** Aporte mensual esperado: jubilación + cesantía de los partícipes activos. */
    private Double esperado;

    /** Componente de jubilación del esperado (tipo de aporte 9). */
    private Double esperadoJubilacion;

    /** Componente de cesantía del esperado (tipo de aporte 11). */
    private Double esperadoCesantia;

    /** Partícipes activos que entran en el esperado. */
    private Long participes;

    /** Aportes efectivamente registrados en el mes cerrado, solo movimientos positivos. */
    private Double registrado;

    /** Componente de jubilación de lo registrado (tipo 9). */
    private Double registradoJubilacion;

    /** Componente de cesantía de lo registrado (tipo 11). */
    private Double registradoCesantia;

    /** {@code esperado − registrado}, CON signo. Negativo = se cobró de más. */
    private Double diferencia;

    /** {@code max(diferencia, 0)}: el importe que efectivamente va al asiento de neteo. */
    private Double noCobrado;

    /**
     * {@code max(-diferencia, 0)}: lo cobrado por encima de lo esperado. Cero es lo normal.
     * Si es mayor que cero, el asiento NO lo lleva — lo resuelve el proceso de cobro en
     * exceso — pero hay que verlo.
     */
    private Double excesoCobro;

    /** Primer día del mes cerrado, que es desde cuándo se contaron los registrados. */
    private java.time.LocalDate desde;

    /** Último día del mes cerrado, que es hasta cuándo se contaron los registrados. */
    private java.time.LocalDate hasta;

    public DesgloseAportesCierre() {
    }

    public Double getEsperado() {
        return esperado;
    }

    public void setEsperado(Double esperado) {
        this.esperado = esperado;
    }

    public Double getEsperadoJubilacion() {
        return esperadoJubilacion;
    }

    public void setEsperadoJubilacion(Double esperadoJubilacion) {
        this.esperadoJubilacion = esperadoJubilacion;
    }

    public Double getEsperadoCesantia() {
        return esperadoCesantia;
    }

    public void setEsperadoCesantia(Double esperadoCesantia) {
        this.esperadoCesantia = esperadoCesantia;
    }

    public Long getParticipes() {
        return participes;
    }

    public void setParticipes(Long participes) {
        this.participes = participes;
    }

    public Double getRegistrado() {
        return registrado;
    }

    public void setRegistrado(Double registrado) {
        this.registrado = registrado;
    }

    public Double getRegistradoJubilacion() {
        return registradoJubilacion;
    }

    public void setRegistradoJubilacion(Double registradoJubilacion) {
        this.registradoJubilacion = registradoJubilacion;
    }

    public Double getRegistradoCesantia() {
        return registradoCesantia;
    }

    public void setRegistradoCesantia(Double registradoCesantia) {
        this.registradoCesantia = registradoCesantia;
    }

    public Double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(Double diferencia) {
        this.diferencia = diferencia;
    }

    public Double getNoCobrado() {
        return noCobrado;
    }

    public void setNoCobrado(Double noCobrado) {
        this.noCobrado = noCobrado;
    }

    public Double getExcesoCobro() {
        return excesoCobro;
    }

    public void setExcesoCobro(Double excesoCobro) {
        this.excesoCobro = excesoCobro;
    }

    public java.time.LocalDate getDesde() {
        return desde;
    }

    public void setDesde(java.time.LocalDate desde) {
        this.desde = desde;
    }

    public java.time.LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(java.time.LocalDate hasta) {
        this.hasta = hasta;
    }
}
