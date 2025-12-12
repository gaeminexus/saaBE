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
 * Representa la tabla PXCA (ParticipeXCargaArchivo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PXCA", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ParticipeXCargaArchivoAll", query = "select e from ParticipeXCargaArchivo e"),
    @NamedQuery(name = "ParticipeXCargaArchivoId", query = "select e from ParticipeXCargaArchivo e where e.codigo = :id")
})
public class ParticipeXCargaArchivo implements Serializable {

    // PK
    @Id
    @Basic
    @Column(name = "PXCACDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Detalle Carga Archivo */
    @ManyToOne
    @JoinColumn(name = "DTCACDGO", referencedColumnName = "DTCACDGO")
    private DetalleCargaArchivo detalleCargaArchivo;

    /** Código Petro */
    @Basic
    @Column(name = "PXCACDPT")
    private Long codigoPetro;

    /** Nombre del partícipe */
    @Basic
    @Column(name = "PXCANMBR", length = 2000)
    private String nombre;

    /** Plazo inicial */
    @Basic
    @Column(name = "PXCAPLIN")
    private Long plazoInicial;

    /** Meses plazo */
    @Basic
    @Column(name = "PXCAMSPL")
    private Long mesesPlazo;

    /** Saldo Actual */
    @Basic
    @Column(name = "PXCASLAC")
    private Long saldoActual;

    /** Interés Anual */
    @Basic
    @Column(name = "PXCAINAN")
    private Long interesAnual;

    /** Valor Seguro */
    @Basic
    @Column(name = "PXCAVLSG")
    private Long valorSeguro;

    /** Monto a Descontar */
    @Basic
    @Column(name = "PXCADSCT")
    private Long montoDescontar;

    /** Capital Descontado */
    @Basic
    @Column(name = "PXCACPDS")
    private Long capitalDescontado;

    /** Interés Descontado */
    @Basic
    @Column(name = "PXCAINDS")
    private Long interesDescontado;

    /** Seguro Descontado */
    @Basic
    @Column(name = "PXCASGDS")
    private Long seguroDescontado;

    /** Total Descontado */
    @Basic
    @Column(name = "PXCADSDO")
    private Long totalDescontado;

    /** Capital No Descontado */
    @Basic
    @Column(name = "PXCACPND")
    private Long capitalNoDescontado;

    /** Interés No Descontado */
    @Basic
    @Column(name = "PXCAITND")
    private Long interesNoDescontado;

    /** Desgravamen No Descontado */
    @Basic
    @Column(name = "PXCADSND")
    private Long desgravamenNoDescontado;
    
    /** Estado revisión */
    @Basic
    @Column(name = "PXCAESRV")
    private Long estadoRevision;
    
    /** Novedades durante la carga */
    @Basic
    @Column(name = "PXCANVCA")
    private Long novedadesCarga;

    /** Estado del registro */
    @Basic
    @Column(name = "PXCAESTD")
    private Long estado;
    
    /** Novedades Financieras */
    @Basic
    @Column(name = "PXCANVFN")
    private Long novedadesFinancieras;
    


    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public DetalleCargaArchivo getDetalleCargaArchivo() {
        return detalleCargaArchivo;
    }

    public void setDetalleCargaArchivo(DetalleCargaArchivo detalleCargaArchivo) {
        this.detalleCargaArchivo = detalleCargaArchivo;
    }

    public Long getCodigoPetro() {
        return codigoPetro;
    }

    public void setCodigoPetro(Long codigoPetro) {
        this.codigoPetro = codigoPetro;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getPlazoInicial() {
        return plazoInicial;
    }

    public void setPlazoInicial(Long plazoInicial) {
        this.plazoInicial = plazoInicial;
    }

    public Long getMesesPlazo() {
        return mesesPlazo;
    }

    public void setMesesPlazo(Long mesesPlazo) {
        this.mesesPlazo = mesesPlazo;
    }

    public Long getSaldoActual() {
        return saldoActual;
    }

    public void setSaldoActual(Long saldoActual) {
        this.saldoActual = saldoActual;
    }

    public Long getInteresAnual() {
        return interesAnual;
    }

    public void setInteresAnual(Long interesAnual) {
        this.interesAnual = interesAnual;
    }

    public Long getValorSeguro() {
        return valorSeguro;
    }

    public void setValorSeguro(Long valorSeguro) {
        this.valorSeguro = valorSeguro;
    }

    public Long getMontoDescontar() {
        return montoDescontar;
    }

    public void setMontoDescontar(Long montoDescontar) {
        this.montoDescontar = montoDescontar;
    }

    public Long getCapitalDescontado() {
        return capitalDescontado;
    }

    public void setCapitalDescontado(Long capitalDescontado) {
        this.capitalDescontado = capitalDescontado;
    }

    public Long getInteresDescontado() {
        return interesDescontado;
    }

    public void setInteresDescontado(Long interesDescontado) {
        this.interesDescontado = interesDescontado;
    }

    public Long getSeguroDescontado() {
        return seguroDescontado;
    }

    public void setSeguroDescontado(Long seguroDescontado) {
        this.seguroDescontado = seguroDescontado;
    }

    public Long getTotalDescontado() {
        return totalDescontado;
    }

    public void setTotalDescontado(Long totalDescontado) {
        this.totalDescontado = totalDescontado;
    }

    public Long getCapitalNoDescontado() {
        return capitalNoDescontado;
    }

    public void setCapitalNoDescontado(Long capitalNoDescontado) {
        this.capitalNoDescontado = capitalNoDescontado;
    }

    public Long getInteresNoDescontado() {
        return interesNoDescontado;
    }

    public void setInteresNoDescontado(Long interesNoDescontado) {
        this.interesNoDescontado = interesNoDescontado;
    }

    public Long getDesgravamenNoDescontado() {
        return desgravamenNoDescontado;
    }

    public void setDesgravamenNoDescontado(Long desgravamenNoDescontado) {
        this.desgravamenNoDescontado = desgravamenNoDescontado;
    }

    public Long getEstadoRevision() {
        return estadoRevision;
    }

    public void setEstadoRevision(Long estadoRevision) {
        this.estadoRevision = estadoRevision;
    }
    
    public Long getNovedadesCarga() {
		return novedadesCarga; 
	}

	public void setNovedadesCarga(Long novedadesCarga) {
		this.novedadesCarga = novedadesCarga;
	}

	public Long getEstado() {
        return estado;
    }
    
    public void setEstado(Long estado) {
        this.estado = estado;
    }

	public Long getNovedadesFinancieras() {
		return novedadesFinancieras;
	}

	public void setNovedadesFinancieras(Long novedadesFinancieras) {
		this.novedadesFinancieras = novedadesFinancieras;
	}
}
