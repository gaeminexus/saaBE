package com.saa.model.credito;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Representa la tabla DTCA (DetalleCargaArchivo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTCA", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "DetalleCargaArchivoAll", query = "select e from DetalleCargaArchivo e"),
    @NamedQuery(name = "DetalleCargaArchivoId", query = "select e from DetalleCargaArchivo e where e.codigo = :id")
})
public class DetalleCargaArchivo implements Serializable {

    // PK
    @Id
    @Basic
    @Column(name = "DTCACDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Carga Archivo */
    @ManyToOne
    @JoinColumn(name = "CRARCDGO", referencedColumnName = "CRARCDGO")
    private CargaArchivo cargaArchivo;

    /** Código Petro Producto */
    @Basic
    @Column(name = "DTCACDPP", length = 2000)
    private String codigoPetroProducto;

    /** Nombre Producto Petro */
    @Basic
    @Column(name = "DTCANMPP", length = 2000)
    private String nombreProductoPetro;

    /** Total partícipes por producto */
    @Basic
    @Column(name = "DTCATPXP")
    private Long totalParticipes;

    /** Total Saldo Actual */
    @Basic
    @Column(name = "DTCATTSA")
    private Long totalSaldoActual;

    /** Total Interés Anual */
    @Basic
    @Column(name = "DTCATTIA")
    private Long totalInteresAnual;

    /** Total Valor Seguro */
    @Basic
    @Column(name = "DTCATTVS")
    private Long totalValorSeguro;

    /** Total Descontar */
    @Basic
    @Column(name = "DTCATTDS")
    private Long totalDescontar;

    /** Total Capital Descontado */
    @Basic
    @Column(name = "DTCATTCD")
    private Long totalCapitalDescontado;

    /** Total Interés Descontado */
    @Basic
    @Column(name = "DTCATTID")
    private Long totalInteresDescontado;

    /** Total Seguro Descontado */
    @Basic
    @Column(name = "DTCATTSD")
    private Long totalSeguroDescontado;

    /** Total Descontado */
    @Basic
    @Column(name = "DTCATTDO")
    private Long totalDescontado;

    /** Total Capital No Descontado */
    @Basic
    @Column(name = "DTCATCND")
    private Long totalCapitalNoDescontado;

    /** Total Interés No Descontado */
    @Basic
    @Column(name = "DTCATIND")
    private Long totalInteresNoDescontado;

    /** Total Desgravamen No Descontado */
    @Basic
    @Column(name = "DTCATDND")
    private Long totalDesgravamenNoDescontado;

    /** Estado del registro */
    @Basic
    @Column(name = "DTCAESTD")
    private Long estado;

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public CargaArchivo getCargaArchivo() {
        return cargaArchivo;
    }

    public void setCargaArchivo(CargaArchivo cargaArchivo) {
        this.cargaArchivo = cargaArchivo;
    }

    public String getCodigoPetroProducto() {
        return codigoPetroProducto;
    }

    public void setCodigoPetroProducto(String codigoPetroProducto) {
        this.codigoPetroProducto = codigoPetroProducto;
    }

    public String getNombreProductoPetro() {
        return nombreProductoPetro;
    }

    public void setNombreProductoPetro(String nombreProductoPetro) {
        this.nombreProductoPetro = nombreProductoPetro;
    }

    public Long getTotalParticipes() {
        return totalParticipes;
    }

    public void setTotalParticipes(Long totalParticipes) {
        this.totalParticipes = totalParticipes;
    }

    public Long getTotalSaldoActual() {
        return totalSaldoActual;
    }

    public void setTotalSaldoActual(Long totalSaldoActual) {
        this.totalSaldoActual = totalSaldoActual;
    }

    public Long getTotalInteresAnual() {
        return totalInteresAnual;
    }

    public void setTotalInteresAnual(Long totalInteresAnual) {
        this.totalInteresAnual = totalInteresAnual;
    }

    public Long getTotalValorSeguro() {
        return totalValorSeguro;
    }

    public void setTotalValorSeguro(Long totalValorSeguro) {
        this.totalValorSeguro = totalValorSeguro;
    }

    public Long getTotalDescontar() {
        return totalDescontar;
    }

    public void setTotalDescontar(Long totalDescontar) {
        this.totalDescontar = totalDescontar;
    }

    public Long getTotalCapitalDescontado() {
        return totalCapitalDescontado;
    }

    public void setTotalCapitalDescontado(Long totalCapitalDescontado) {
        this.totalCapitalDescontado = totalCapitalDescontado;
    }

    public Long getTotalInteresDescontado() {
        return totalInteresDescontado;
    }

    public void setTotalInteresDescontado(Long totalInteresDescontado) {
        this.totalInteresDescontado = totalInteresDescontado;
    }

    public Long getTotalSeguroDescontado() {
        return totalSeguroDescontado;
    }

    public void setTotalSeguroDescontado(Long totalSeguroDescontado) {
        this.totalSeguroDescontado = totalSeguroDescontado;
    }

    public Long getTotalDescontado() {
        return totalDescontado;
    }

    public void setTotalDescontado(Long totalDescontado) {
        this.totalDescontado = totalDescontado;
    }

    public Long getTotalCapitalNoDescontado() {
        return totalCapitalNoDescontado;
    }

    public void setTotalCapitalNoDescontado(Long totalCapitalNoDescontado) {
        this.totalCapitalNoDescontado = totalCapitalNoDescontado;
    }

    public Long getTotalInteresNoDescontado() {
        return totalInteresNoDescontado;
    }

    public void setTotalInteresNoDescontado(Long totalInteresNoDescontado) {
        this.totalInteresNoDescontado = totalInteresNoDescontado;
    }

    public Long getTotalDesgravamenNoDescontado() {
        return totalDesgravamenNoDescontado;
    }

    public void setTotalDesgravamenNoDescontado(Long totalDesgravamenNoDescontado) {
        this.totalDesgravamenNoDescontado = totalDesgravamenNoDescontado;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
