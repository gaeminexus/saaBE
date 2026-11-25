package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lote 7 (D-186): resultado de {@code POST /rest/prov/ajusteTopeMora}. Contrato:
 * docs/logica-negocio/crd/API-AJUSTE-TOPE-PROVISION-MORA.md.
 */
public class ResultadoAjusteTopeMora implements Serializable {

    private static final long serialVersionUID = 1L;

    private String corte;
    private Long idAsiento;
    /** true si esta llamada escribió el ajuste. */
    private boolean grabado;
    /** true si ya había ajuste tipo 10 vigente de ese asiento (no se repite). */
    private boolean yaGrabado;
    /** Σ del ajuste (lo calculado, o lo ya grabado si {@code yaGrabado}). */
    private double totalAjuste;
    /** Σ debe − haber de las cuentas de provisión del asiento. */
    private double totalAsiento;
    /** true si, por cuenta, |ajuste − asiento| ≤ 0,01. */
    private boolean cuadra;
    private int cuotasAjustadas;
    private List<Tipo> porTipo = new ArrayList<>();
    private List<Cuota> detalle = new ArrayList<>();
    private List<String> advertencias = new ArrayList<>();

    /** Ajuste por tipo de préstamo y su cuenta (1.4.99.05 de la plantilla 36, papel 81). */
    public static class Tipo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long idTipoPrestamo;
        private String cuenta;
        private double ajuste;
        /** Debe − haber de esa cuenta en el asiento (compartida entre tipos: el mismo valor en cada tipo). */
        private double asiento;
        private double diferencia;

        public Long getIdTipoPrestamo() { return idTipoPrestamo; }
        public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }
        public String getCuenta() { return cuenta; }
        public void setCuenta(String cuenta) { this.cuenta = cuenta; }
        public double getAjuste() { return ajuste; }
        public void setAjuste(double ajuste) { this.ajuste = ajuste; }
        public double getAsiento() { return asiento; }
        public void setAsiento(double asiento) { this.asiento = asiento; }
        public double getDiferencia() { return diferencia; }
        public void setDiferencia(double diferencia) { this.diferencia = diferencia; }
    }

    /** Una cuota con ajuste. */
    public static class Cuota implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long idCuota;
        private Long idPrestamo;
        private Long operacion;
        private Long numeroCuota;
        private Long idTipoPrestamo;
        private double saldoProvisionadoMora;
        private double moraCalculadaPendiente;
        private double moraContabilizadaPendiente;
        private double tope;
        private double ajuste;

        public Long getIdCuota() { return idCuota; }
        public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }
        public Long getIdPrestamo() { return idPrestamo; }
        public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
        public Long getOperacion() { return operacion; }
        public void setOperacion(Long operacion) { this.operacion = operacion; }
        public Long getNumeroCuota() { return numeroCuota; }
        public void setNumeroCuota(Long numeroCuota) { this.numeroCuota = numeroCuota; }
        public Long getIdTipoPrestamo() { return idTipoPrestamo; }
        public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }
        public double getSaldoProvisionadoMora() { return saldoProvisionadoMora; }
        public void setSaldoProvisionadoMora(double v) { this.saldoProvisionadoMora = v; }
        public double getMoraCalculadaPendiente() { return moraCalculadaPendiente; }
        public void setMoraCalculadaPendiente(double v) { this.moraCalculadaPendiente = v; }
        public double getMoraContabilizadaPendiente() { return moraContabilizadaPendiente; }
        public void setMoraContabilizadaPendiente(double v) { this.moraContabilizadaPendiente = v; }
        public double getTope() { return tope; }
        public void setTope(double tope) { this.tope = tope; }
        public double getAjuste() { return ajuste; }
        public void setAjuste(double ajuste) { this.ajuste = ajuste; }
    }

    public String getCorte() { return corte; }
    public void setCorte(String corte) { this.corte = corte; }
    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
    public boolean isGrabado() { return grabado; }
    public void setGrabado(boolean grabado) { this.grabado = grabado; }
    public boolean isYaGrabado() { return yaGrabado; }
    public void setYaGrabado(boolean yaGrabado) { this.yaGrabado = yaGrabado; }
    public double getTotalAjuste() { return totalAjuste; }
    public void setTotalAjuste(double totalAjuste) { this.totalAjuste = totalAjuste; }
    public double getTotalAsiento() { return totalAsiento; }
    public void setTotalAsiento(double totalAsiento) { this.totalAsiento = totalAsiento; }
    public boolean isCuadra() { return cuadra; }
    public void setCuadra(boolean cuadra) { this.cuadra = cuadra; }
    public int getCuotasAjustadas() { return cuotasAjustadas; }
    public void setCuotasAjustadas(int cuotasAjustadas) { this.cuotasAjustadas = cuotasAjustadas; }
    public List<Tipo> getPorTipo() { return porTipo; }
    public void setPorTipo(List<Tipo> porTipo) { this.porTipo = porTipo; }
    public List<Cuota> getDetalle() { return detalle; }
    public void setDetalle(List<Cuota> detalle) { this.detalle = detalle; }
    public List<String> getAdvertencias() { return advertencias; }
    public void setAdvertencias(List<String> advertencias) { this.advertencias = advertencias; }
}
