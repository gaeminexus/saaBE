package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de correr {@code PagoPensionComplementariaService#generarPagosDelMes} para un
 * período. Un jubilado con datos malos (sin VPPC activo, sin cuenta bancaria, saldo
 * insuficiente) no aborta el lote — se cuenta como error y el resto sigue, mismo criterio que
 * {@code DevolucionAporteService#sincronizarPagos}.
 */
public class ResultadoGeneracionPagosPension {

    private Integer anio;
    private Integer mes;

    /** Cuántos jubilados JUBILADO_COMPLEMENTARIO con VPPC activo se evaluaron */
    private int evaluados;

    /** Pagos PGPC nuevos generados en esta corrida */
    private int generados;

    /** Ya tenían PGPC para este período (idempotencia) — no es error, se informa */
    private int yaGenerados;

    private int conError;

    private List<String> errores = new ArrayList<>();

    /** Suma de {@code valorPension + valorSeguro} de todos los PGPC generados en esta corrida. */
    private double totalPagado;

    /** Cuánto de lo pagado se cruzó contra deuda de préstamos vigentes (PLAN-PAGO-JUBILADOS.md §3). */
    private double totalCruzadoAPrestamos;

    /** Cuánto salió efectivamente como órdenes de pago hacia tesorería. */
    private double totalOrdenesGeneradas;

    /**
     * §4bis del contrato, pedido del usuario 2026-09-04: suma del seguro médico
     * ({@link DetallePagoPension#getTotalSeguro()}) de todos los jubilados de la corrida — va
     * a una cuenta contable distinta de la pensión (plantilla alterno 35).
     */
    private double totalSeguroGeneral;

    /**
     * §4quater del contrato (2026-09-05): código de la orden de pago AGREGADA al proveedor del
     * seguro médico por {@link #totalSeguroGeneral} de este período —
     * {@code PagoProgramado.codigo}, {@code OrigenPagoExterno.CRD_SEGURO_JUBILADOS}. {@code null}
     * si no hubo seguro que pagar ($0) o si ya existía una orden vigente para el período
     * (idempotencia por {@code (origen, idOrigen)}).
     */
    private Long idPagoProveedorSeguro;

    /** Un renglón por jubilado evaluado (generado, ya existía, o con error). */
    private List<DetallePagoPension> detalle = new ArrayList<>();

    public ResultadoGeneracionPagosPension() {
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public int getEvaluados() {
        return evaluados;
    }

    public void setEvaluados(int evaluados) {
        this.evaluados = evaluados;
    }

    public int getGenerados() {
        return generados;
    }

    public void setGenerados(int generados) {
        this.generados = generados;
    }

    public int getYaGenerados() {
        return yaGenerados;
    }

    public void setYaGenerados(int yaGenerados) {
        this.yaGenerados = yaGenerados;
    }

    public int getConError() {
        return conError;
    }

    public void setConError(int conError) {
        this.conError = conError;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }

    public double getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(double totalPagado) {
        this.totalPagado = totalPagado;
    }

    public double getTotalCruzadoAPrestamos() {
        return totalCruzadoAPrestamos;
    }

    public void setTotalCruzadoAPrestamos(double totalCruzadoAPrestamos) {
        this.totalCruzadoAPrestamos = totalCruzadoAPrestamos;
    }

    public double getTotalOrdenesGeneradas() {
        return totalOrdenesGeneradas;
    }

    public void setTotalOrdenesGeneradas(double totalOrdenesGeneradas) {
        this.totalOrdenesGeneradas = totalOrdenesGeneradas;
    }

    public double getTotalSeguroGeneral() {
        return totalSeguroGeneral;
    }

    public void setTotalSeguroGeneral(double totalSeguroGeneral) {
        this.totalSeguroGeneral = totalSeguroGeneral;
    }

    public Long getIdPagoProveedorSeguro() {
        return idPagoProveedorSeguro;
    }

    public void setIdPagoProveedorSeguro(Long idPagoProveedorSeguro) {
        this.idPagoProveedorSeguro = idPagoProveedorSeguro;
    }

    public List<DetallePagoPension> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<DetallePagoPension> detalle) {
        this.detalle = detalle;
    }
}
