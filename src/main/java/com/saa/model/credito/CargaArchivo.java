package com.saa.model.credito;

import java.io.Serializable;
import java.sql.Timestamp;

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
 * Representa la tabla CRAR (CargaArchivo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRAR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CargaArchivoAll", query = "select e from CargaArchivo e"),
    @NamedQuery(name = "CargaArchivoId", query = "select e from CargaArchivo e where e.codigo = :id")
})
public class CargaArchivo implements Serializable {

    // PK
    @Id
    @Basic
    @Column(name = "CRARCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Nombre del archivo cargado */
    @Basic
    @Column(name = "CRARNMBR", length = 2000)
    private String nombre;

    /** Fecha de carga */
    @Basic
    @Column(name = "CRARFCCR")
    private Timestamp fechaCarga;

    /** Usuario que cargó */
    @Basic
    @Column(name = "CRARUSCD")
    private Long codigoUsuario;

    /** FK - Filial */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /** Ruta del archivo en el servidor */
    @Basic
    @Column(name = "CRARRTAS", length = 2000)
    private String rutaArchivo;

    /** Mes de afectación */
    @Basic
    @Column(name = "CRARMSAF")
    private Long mesAfectacion;

    /** Año de afectación */
    @Basic
    @Column(name = "CRARANAF")
    private Long anioAfectacion;

    /** Total Saldo Actual */
    @Basic
    @Column(name = "CRARTTSA")
    private Long totalSaldoActual;

    /** Total Interés Anual */
    @Basic
    @Column(name = "CRARTTIA")
    private Long totalInteresAnual;

    /** Total Valor Seguro */
    @Basic
    @Column(name = "CRARTTVS")
    private Long totalValorSeguro;

    /** Total Descontar */
    @Basic
    @Column(name = "CRARTTDS")
    private Long totalDescontar;

    /** Total Capital Descontado */
    @Basic
    @Column(name = "CRARTTCD")
    private Long totalCapitalDescontado;

    /** Total Interés Descontado */
    @Basic
    @Column(name = "CRARTTID")
    private Long totalInteresDescontado;

    /** Total Seguro Descontado */
    @Basic
    @Column(name = "CRARTTSD")
    private Long totalSeguroDescontado;

    /** Total Descontado */
    @Basic
    @Column(name = "CRARTTDO")
    private Long totalDescontado;

    /** Total Capital No Descontado */
    @Basic
    @Column(name = "CRARTCND")
    private Long totalCapitalNoDescontado;

    /** Total Interés No Descontado */
    @Basic
    @Column(name = "CRARTIND")
    private Long totalInteresNoDescontado;

    /** Total Desgravamen No Descontado */
    @Basic
    @Column(name = "CRARTDND")
    private Long totalDesgravamenNoDescontado;

    /** Estado del registro */
    @Basic
    @Column(name = "CRARESTD")
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Timestamp getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(Timestamp fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public Long getCodigoUsuario() {
        return codigoUsuario;
    }

    public void setCodigoUsuario(Long codigoUsuario) {
        this.codigoUsuario = codigoUsuario;
    }

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Long getMesAfectacion() {
        return mesAfectacion;
    }

    public void setMesAfectacion(Long mesAfectacion) {
        this.mesAfectacion = mesAfectacion;
    }

    public Long getAnioAfectacion() {
        return anioAfectacion;
    }

    public void setAnioAfectacion(Long anioAfectacion) {
        this.anioAfectacion = anioAfectacion;
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
