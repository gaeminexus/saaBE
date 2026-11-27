package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** C53: resultado del control de aportes del mes (A: sin asiento; B: 2.1 contable contra el modulo). Solo lectura. */
public class ResultadoControlAportes implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Un aporte vigente del mes de REGISTRO APORTE sin asiento ni cobro que lo respalde (sin datos del partícipe: solo ids). */
    public static class AporteSinAsiento implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long idAporte;
        private Long idEntidad;
        private Long idTipoAporte;
        private double valor;
        private String fecha;

        public Long getIdAporte() { return idAporte; }
        public void setIdAporte(Long idAporte) { this.idAporte = idAporte; }
        public Long getIdEntidad() { return idEntidad; }
        public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
        public Long getIdTipoAporte() { return idTipoAporte; }
        public void setIdTipoAporte(Long idTipoAporte) { this.idTipoAporte = idTipoAporte; }
        public double getValor() { return valor; }
        public void setValor(double valor) { this.valor = valor; }
        public String getFecha() { return fecha; }
        public void setFecha(String fecha) { this.fecha = fecha; }
    }

    /** Una cuenta (o el total 2.1) donde el modulo y la contabilidad no coinciden. */
    public static class DiferenciaCuenta implements Serializable {
        private static final long serialVersionUID = 1L;
        private String cuenta;
        private String tipos;
        private double valorModulo;
        private double saldoContable;
        private double diferencia;

        public String getCuenta() { return cuenta; }
        public void setCuenta(String cuenta) { this.cuenta = cuenta; }
        public String getTipos() { return tipos; }
        public void setTipos(String tipos) { this.tipos = tipos; }
        public double getValorModulo() { return valorModulo; }
        public void setValorModulo(double valorModulo) { this.valorModulo = valorModulo; }
        public double getSaldoContable() { return saldoContable; }
        public void setSaldoContable(double saldoContable) { this.saldoContable = saldoContable; }
        public double getDiferencia() { return diferencia; }
        public void setDiferencia(double diferencia) { this.diferencia = diferencia; }
    }

    private List<AporteSinAsiento> sinAsiento = new ArrayList<AporteSinAsiento>();
    private double totalSinAsiento;
    /** Cuentas con diferencia, y al final el total 2.1 si tambien difiere. Vacia: la 2.1 cuadra con el modulo. */
    private List<DiferenciaCuenta> diferencias = new ArrayList<DiferenciaCuenta>();

    public List<AporteSinAsiento> getSinAsiento() { return sinAsiento; }
    public void setSinAsiento(List<AporteSinAsiento> sinAsiento) { this.sinAsiento = sinAsiento; }
    public double getTotalSinAsiento() { return totalSinAsiento; }
    public void setTotalSinAsiento(double totalSinAsiento) { this.totalSinAsiento = totalSinAsiento; }
    public List<DiferenciaCuenta> getDiferencias() { return diferencias; }
    public void setDiferencias(List<DiferenciaCuenta> diferencias) { this.diferencias = diferencias; }
}
