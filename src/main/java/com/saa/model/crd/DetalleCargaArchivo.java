package com.saa.model.crd;

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
    private Double totalParticipes;

    /** Total Saldo Actual */
    @Basic
    @Column(name = "DTCATTSA")
    private Double totalSaldoActual;

    /** Total Interés Anual */
    @Basic
    @Column(name = "DTCATTIA")
    private Double totalInteresAnual;

    /** Total Valor Seguro */
    @Basic
    @Column(name = "DTCATTVS")
    private Double totalValorSeguro;

    /** Total Descontar */
    @Basic
    @Column(name = "DTCATTDS")
    private Double totalDescontar;

    /** Total Capital Descontado */
    @Basic
    @Column(name = "DTCATTCD")
    private Double totalCapitalDescontado;

    /** Total Interés Descontado */
    @Basic
    @Column(name = "DTCATTID")
    private Double totalInteresDescontado;

    /** Total Seguro Descontado */
    @Basic
    @Column(name = "DTCATTSD")
    private Double totalSeguroDescontado;

    /** Total Descontado */
    @Basic
    @Column(name = "DTCATTDO")
    private Double totalDescontado;

    /** Total Capital No Descontado */
    @Basic
    @Column(name = "DTCATCND")
    private Double totalCapitalNoDescontado;

    /** Total Interés No Descontado */
    @Basic
    @Column(name = "DTCATIND")
    private Double totalInteresNoDescontado;

    /** Total Desgravamen No Descontado */
    @Basic
    @Column(name = "DTCATDND")
    private Double totalDesgravamenNoDescontado;

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

    public Double getTotalParticipes() {
        return totalParticipes;
    }

    public void setTotalParticipes(Double totalParticipes) {
        this.totalParticipes = totalParticipes;
    }

    public Double getTotalSaldoActual() {
        return totalSaldoActual;
    }

    public void setTotalSaldoActual(Double totalSaldoActual) {
        this.totalSaldoActual = totalSaldoActual;
    }

    public Double getTotalInteresAnual() {
        return totalInteresAnual;
    }

    public void setTotalInteresAnual(Double totalInteresAnual) {
        this.totalInteresAnual = totalInteresAnual;
    }

    public Double getTotalValorSeguro() {
        return totalValorSeguro;
    }

    public void setTotalValorSeguro(Double totalValorSeguro) {
        this.totalValorSeguro = totalValorSeguro;
    }

    public Double getTotalDescontar() {
        return totalDescontar;
    }

    public void setTotalDescontar(Double totalDescontar) {
        this.totalDescontar = totalDescontar;
    }

    public Double getTotalCapitalDescontado() {
        return totalCapitalDescontado;
    }

    public void setTotalCapitalDescontado(Double totalCapitalDescontado) {
        this.totalCapitalDescontado = totalCapitalDescontado;
    }

    public Double getTotalInteresDescontado() {
        return totalInteresDescontado;
    }

    public void setTotalInteresDescontado(Double totalInteresDescontado) {
        this.totalInteresDescontado = totalInteresDescontado;
    }

    public Double getTotalSeguroDescontado() {
        return totalSeguroDescontado;
    }

    public void setTotalSeguroDescontado(Double totalSeguroDescontado) {
        this.totalSeguroDescontado = totalSeguroDescontado;
    }

    public Double getTotalDescontado() {
        return totalDescontado;
    }

    public void setTotalDescontado(Double totalDescontado) {
        this.totalDescontado = totalDescontado;
    }

    public Double getTotalCapitalNoDescontado() {
        return totalCapitalNoDescontado;
    }

    public void setTotalCapitalNoDescontado(Double totalCapitalNoDescontado) {
        this.totalCapitalNoDescontado = totalCapitalNoDescontado;
    }

    public Double getTotalInteresNoDescontado() {
        return totalInteresNoDescontado;
    }

    public void setTotalInteresNoDescontado(Double totalInteresNoDescontado) {
        this.totalInteresNoDescontado = totalInteresNoDescontado;
    }

    public Double getTotalDesgravamenNoDescontado() {
        return totalDesgravamenNoDescontado;
    }

    public void setTotalDesgravamenNoDescontado(Double totalDesgravamenNoDescontado) {
        this.totalDesgravamenNoDescontado = totalDesgravamenNoDescontado;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
