package com.saa.model.crd;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Representa la tabla HDAP (Histórico de Desglose de Aportes de Partícipes).
 * Almacena el desglose detallado de aportes y préstamos de Petrocomercial.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HDAP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "HistoricoDesgloseAporteParticipeAll", query = "select e from HistoricoDesgloseAporteParticipe e"),
    @NamedQuery(name = "HistoricoDesgloseAporteParticipeId", query = "select e from HistoricoDesgloseAporteParticipe e where e.orden = :id"),
    @NamedQuery(name = "HistoricoDesgloseAporteParticipeByCarga", query = "select e from HistoricoDesgloseAporteParticipe e where e.idCarga = :idCarga"),
    @NamedQuery(name = "HistoricoDesgloseAporteParticipeByCedula", query = "select e from HistoricoDesgloseAporteParticipe e where e.cedula = :cedula")
})
public class HistoricoDesgloseAporteParticipe implements Serializable {

    // PK
    @Id
    @Basic
    @Column(name = "HDAPORDE")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orden;

    /** Código interno del partícipe en Petrocomercial */
    @Basic
    @Column(name = "HDAPCODI", length = 20)
    private String codigoInterno;

    /** Número de cédula de identidad del partícipe */
    @Basic
    @Column(name = "HDAPCEDU", length = 15)
    private String cedula;

    /** Valor de aporte para Jubilación */
    @Basic
    @Column(name = "HDAPJUBI")
    private Double aporteJubilacion;

    /** Valor de aporte para Cesantía */
    @Basic
    @Column(name = "HDAPCESA")
    private Double aporteCesantia;

    /** Sumatoria total de beneficios (Aportes) */
    @Basic
    @Column(name = "HDAPTOBE")
    private Double totalBeneficios;

    /** Cuota de Préstamo Emergente */
    @Basic
    @Column(name = "HDAPEMER")
    private Double prestamoEmergente;

    /** Cuota de Préstamo Quirografario */
    @Basic
    @Column(name = "HDAPQUIR")
    private Double prestamoQuirografario;

    /** Cuota de Préstamo Hipotecario */
    @Basic
    @Column(name = "HDAPHIPO")
    private Double prestamoHipotecario;

    /** Cuota de Préstamo Prendario */
    @Basic
    @Column(name = "HDAPPREN")
    private Double prestamoPrendario;

    /** Sumatoria total de cuotas de préstamos */
    @Basic
    @Column(name = "HDAPTOPR")
    private Double totalPrestamos;

    /** Cuota de Préstamo Vehicular */
    @Basic
    @Column(name = "HDAPVEHI")
    private Double prestamoVehicular;

    /** Cuota de Seguro de Incendios */
    @Basic
    @Column(name = "HDAPINCE")
    private Double seguroIncendios;

    /** Cuota de mantenimiento/préstamo Tonsupa */
    @Basic
    @Column(name = "HDAPTONS")
    private Double tonsupa;

    /** Descuento Total (Suma global del registro) */
    @Basic
    @Column(name = "HDAPDSCT")
    private Double descuentoTotal;

    /** ID de carga relacionado con la tabla CRAR/PRCA */
    @Basic
    @Column(name = "HDAPIDCA")
    private Long idCarga;

    /** Fecha y hora técnica de carga a la tabla */
    @Basic
    @Column(name = "HDAPFCTR")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCarga;

    /** Usuario de base de datos que realizó la carga */
    @Basic
    @Column(name = "HDAPUSAR", length = 30)
    private String usuarioCarga;

    /** Estado del registro (1: Cargado, 2: Procesado) */
    @Basic
    @Column(name = "HDAPESTD")
    private Integer estado;

    // Getters y Setters

    public Long getOrden() {
        return orden;
    }

    public void setOrden(Long orden) {
        this.orden = orden;
    }

    public String getCodigoInterno() {
        return codigoInterno;
    }

    public void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public Double getAporteJubilacion() {
        return aporteJubilacion;
    }

    public void setAporteJubilacion(Double aporteJubilacion) {
        this.aporteJubilacion = aporteJubilacion;
    }

    public Double getAporteCesantia() {
        return aporteCesantia;
    }

    public void setAporteCesantia(Double aporteCesantia) {
        this.aporteCesantia = aporteCesantia;
    }

    public Double getTotalBeneficios() {
        return totalBeneficios;
    }

    public void setTotalBeneficios(Double totalBeneficios) {
        this.totalBeneficios = totalBeneficios;
    }

    public Double getPrestamoEmergente() {
        return prestamoEmergente;
    }

    public void setPrestamoEmergente(Double prestamoEmergente) {
        this.prestamoEmergente = prestamoEmergente;
    }

    public Double getPrestamoQuirografario() {
        return prestamoQuirografario;
    }

    public void setPrestamoQuirografario(Double prestamoQuirografario) {
        this.prestamoQuirografario = prestamoQuirografario;
    }

    public Double getPrestamoHipotecario() {
        return prestamoHipotecario;
    }

    public void setPrestamoHipotecario(Double prestamoHipotecario) {
        this.prestamoHipotecario = prestamoHipotecario;
    }

    public Double getPrestamoPrendario() {
        return prestamoPrendario;
    }

    public void setPrestamoPrendario(Double prestamoPrendario) {
        this.prestamoPrendario = prestamoPrendario;
    }

    public Double getTotalPrestamos() {
        return totalPrestamos;
    }

    public void setTotalPrestamos(Double totalPrestamos) {
        this.totalPrestamos = totalPrestamos;
    }

    public Double getPrestamoVehicular() {
        return prestamoVehicular;
    }

    public void setPrestamoVehicular(Double prestamoVehicular) {
        this.prestamoVehicular = prestamoVehicular;
    }

    public Double getSeguroIncendios() {
        return seguroIncendios;
    }

    public void setSeguroIncendios(Double seguroIncendios) {
        this.seguroIncendios = seguroIncendios;
    }

    public Double getTonsupa() {
        return tonsupa;
    }

    public void setTonsupa(Double tonsupa) {
        this.tonsupa = tonsupa;
    }

    public Double getDescuentoTotal() {
        return descuentoTotal;
    }

    public void setDescuentoTotal(Double descuentoTotal) {
        this.descuentoTotal = descuentoTotal;
    }

    public Long getIdCarga() {
        return idCarga;
    }

    public void setIdCarga(Long idCarga) {
        this.idCarga = idCarga;
    }

    public Date getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(Date fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public String getUsuarioCarga() {
        return usuarioCarga;
    }

    public void setUsuarioCarga(String usuarioCarga) {
        this.usuarioCarga = usuarioCarga;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((orden == null) ? 0 : orden.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HistoricoDesgloseAporteParticipe other = (HistoricoDesgloseAporteParticipe) obj;
        if (orden == null) {
            if (other.orden != null)
                return false;
        } else if (!orden.equals(other.orden))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HistoricoDesgloseAporteParticipe [orden=" + orden + ", codigoInterno=" + codigoInterno + ", cedula="
                + cedula + ", totalBeneficios=" + totalBeneficios + ", totalPrestamos=" + totalPrestamos
                + ", descuentoTotal=" + descuentoTotal + "]";
    }
}
