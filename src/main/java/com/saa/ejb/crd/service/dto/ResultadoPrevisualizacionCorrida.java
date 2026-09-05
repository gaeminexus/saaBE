package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de {@code PagoPensionComplementariaService#previsualizarCorrida} — la ESTIMACIÓN
 * de lo que generaría {@code generarPagosDelMes} para un período, sin escribir nada. Ver
 * API-PAGO-PENSION-COMPLEMENTARIA.md §4bis.
 *
 * ⚠️ {@code montoACruzar} es {@code min(...)}: el motor calcula mora e interés al aplicar de
 * verdad, y eso NO se simula. El monto real de la corrida puede diferir.
 */
public class ResultadoPrevisualizacionCorrida {

    private Integer anio;
    private Integer mes;

    private int evaluados;
    private int aptos;
    private int bloqueados;

    /** Suma de {@code montoACruzar} de todos los jubilados aptos. No sale de la asociación. */
    private double totalACruzarPrestamos;

    /** Suma de {@code montoADinero} — esto sí es dinero que saldría al banco. */
    private double totalADinero;

    /**
     * Lo que se descontaría de las cuentas de pensión complementaria (aporte 23): suma de
     * {@code total} de cada fila apta.
     *
     * ⛔ Desde la ampliación del 2026-09-04 ya NO es {@code totalACruzarPrestamos +
     * totalADinero}: hay una TERCERA porción, {@link #totalSeguroInternoGeneral}, que también se
     * descuenta y no sale al banco. La identidad vigente es
     * {@code totalACruzarPrestamos + totalADinero + totalSeguroInternoGeneral}.
     */
    private double totalGeneral;

    /**
     * §4bis del contrato, pedido del usuario 2026-09-04: suma del seguro médico
     * ({@link DetallePrevisualizacionJubilado#getTotalSeguro()}) de todos los jubilados
     * evaluados — cuenta contable distinta de la pensión (plantilla alterno 35).
     */
    private double totalSeguroGeneral;

    /**
     * §6 (ampliación 2026-09-04): la parte de {@link #totalSeguroGeneral} que se traspasaría
     * INTERNAMENTE a {@code 2.3.90.90.06 SEGURO POR PAGAR JUBILADOS} —jubilados sin certificado
     * bancario— sin salir al banco. Es un SUBCONJUNTO de {@code totalSeguroGeneral} y NO está
     * incluido en {@code totalADinero}; sí está incluido en {@code totalGeneral}.
     */
    private double totalSeguroInternoGeneral;

    private List<DetallePrevisualizacionJubilado> detalle = new ArrayList<>();

    public ResultadoPrevisualizacionCorrida() {
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

    public int getAptos() {
        return aptos;
    }

    public void setAptos(int aptos) {
        this.aptos = aptos;
    }

    public int getBloqueados() {
        return bloqueados;
    }

    public void setBloqueados(int bloqueados) {
        this.bloqueados = bloqueados;
    }

    public double getTotalACruzarPrestamos() {
        return totalACruzarPrestamos;
    }

    public void setTotalACruzarPrestamos(double totalACruzarPrestamos) {
        this.totalACruzarPrestamos = totalACruzarPrestamos;
    }

    public double getTotalADinero() {
        return totalADinero;
    }

    public void setTotalADinero(double totalADinero) {
        this.totalADinero = totalADinero;
    }

    public double getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(double totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public double getTotalSeguroGeneral() {
        return totalSeguroGeneral;
    }

    public void setTotalSeguroGeneral(double totalSeguroGeneral) {
        this.totalSeguroGeneral = totalSeguroGeneral;
    }

    public double getTotalSeguroInternoGeneral() {
        return totalSeguroInternoGeneral;
    }

    public void setTotalSeguroInternoGeneral(double totalSeguroInternoGeneral) {
        this.totalSeguroInternoGeneral = totalSeguroInternoGeneral;
    }

    public List<DetallePrevisualizacionJubilado> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<DetallePrevisualizacionJubilado> detalle) {
        this.detalle = detalle;
    }
}
