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
     *
     * ⚠️ Corrección 2026-09-05: idéntico a {@link #totalSeguroInternoGeneral} ahora que el
     * seguro SIEMPRE se separa (ver el JavaDoc de ese campo).
     */
    private double totalSeguroGeneral;

    /**
     * ⚠️ Nombre heredado de 6abf436 (2026-09-04): ya NO es "sólo jubilados sin certificado", es
     * TODO el seguro médico de la corrida — corrección 2026-09-05, decisión del usuario: el
     * seguro se separa siempre (con o sin certificado) y sale como UN pago aparte a un
     * proveedor, nunca dentro de la orden del jubilado. Es este el número que correspondería a
     * esa orden agregada al proveedor — ver la investigación pendiente sobre cómo emitirla en
     * API-PAGO-PENSION-COMPLEMENTARIA.md. NO está incluido en {@code totalADinero}; sí está
     * incluido en {@code totalGeneral}. Propuesto renombrar a {@code totalSeguroProveedorGeneral}
     * — no aplicado, requiere coordinar con el frontend.
     */
    private double totalSeguroInternoGeneral;

    /**
     * Campo 2026-09-05, alcance ampliado el mismo día: {@code false} si CUALQUIERA de las
     * condiciones que la corrida real verifica al principio (antes de tocar el primer jubilado)
     * del lado del proveedor del seguro médico falla — el titular no se resuelve por RUC, la
     * cuenta contable del devengo no coincide con la del producto de pago 516, o el proveedor no
     * tiene una única cuenta bancaria activa. El detalle de CUÁL de las tres fue queda en
     * {@link #mensajeProveedorSeguro}. El prevuelo es, en este momento, el ÚNICO ensayo antes de
     * mover plata real: el frontend tiene que mostrar esto de forma prominente, no como una nota
     * al pie.
     */
    private boolean proveedorSeguroEncontrado = true;

    /** Detalle de por qué {@link #proveedorSeguroEncontrado} es {@code false}. {@code null} si es {@code true}. */
    private String mensajeProveedorSeguro;

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

    public boolean isProveedorSeguroEncontrado() {
        return proveedorSeguroEncontrado;
    }

    public void setProveedorSeguroEncontrado(boolean proveedorSeguroEncontrado) {
        this.proveedorSeguroEncontrado = proveedorSeguroEncontrado;
    }

    public String getMensajeProveedorSeguro() {
        return mensajeProveedorSeguro;
    }

    public void setMensajeProveedorSeguro(String mensajeProveedorSeguro) {
        this.mensajeProveedorSeguro = mensajeProveedorSeguro;
    }

    public List<DetallePrevisualizacionJubilado> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<DetallePrevisualizacionJubilado> detalle) {
        this.detalle = detalle;
    }
}
