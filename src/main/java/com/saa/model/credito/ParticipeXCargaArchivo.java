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
    private Double saldoActual;

    /** Interés Anual */
    @Basic
    @Column(name = "PXCAINAN")
    private Double interesAnual;

    /** Valor Seguro */
    @Basic
    @Column(name = "PXCAVLSG")
    private Double valorSeguro;

    /** Monto a Descontar */
    @Basic
    @Column(name = "PXCADSCT")
    private Double montoDescontar;

    /** Capital Descontado */
    @Basic
    @Column(name = "PXCACPDS")
    private Double capitalDescontado;

    /** Interés Descontado */
    @Basic
    @Column(name = "PXCAINDS")
    private Double interesDescontado;

    /** Seguro Descontado */
    @Basic
    @Column(name = "PXCASGDS")
    private Double seguroDescontado;

    /** Total Descontado */
    @Basic
    @Column(name = "PXCADSDO")
    private Double totalDescontado;

    /** Capital No Descontado */
    @Basic
    @Column(name = "PXCACPND")
    private Double capitalNoDescontado;

    /** Interés No Descontado */
    @Basic
    @Column(name = "PXCAITND")
    private Double interesNoDescontado;

    /** Desgravamen No Descontado */
    @Basic
    @Column(name = "PXCADSND")
    private Double desgravamenNoDescontado;
    
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

    public Double getSaldoActual() {
        return saldoActual;
    }

    public void setSaldoActual(Double saldoActual) {
        this.saldoActual = saldoActual;
    }

    public Double getInteresAnual() {
        return interesAnual;
    }

    public void setInteresAnual(Double interesAnual) {
        this.interesAnual = interesAnual;
    }

    public Double getValorSeguro() {
        return valorSeguro;
    }

    public void setValorSeguro(Double valorSeguro) {
        this.valorSeguro = valorSeguro;
    }

    public Double getMontoDescontar() {
        return montoDescontar;
    }

    public void setMontoDescontar(Double montoDescontar) {
        this.montoDescontar = montoDescontar;
    }

    public Double getCapitalDescontado() {
        return capitalDescontado;
    }

    public void setCapitalDescontado(Double capitalDescontado) {
        this.capitalDescontado = capitalDescontado;
    }

    public Double getInteresDescontado() {
        return interesDescontado;
    }

    public void setInteresDescontado(Double interesDescontado) {
        this.interesDescontado = interesDescontado;
    }

    public Double getSeguroDescontado() {
        return seguroDescontado;
    }

    public void setSeguroDescontado(Double seguroDescontado) {
        this.seguroDescontado = seguroDescontado;
    }

    public Double getTotalDescontado() {
        return totalDescontado;
    }

    public void setTotalDescontado(Double totalDescontado) {
        this.totalDescontado = totalDescontado;
    }

    public Double getCapitalNoDescontado() {
        return capitalNoDescontado;
    }

    public void setCapitalNoDescontado(Double capitalNoDescontado) {
        this.capitalNoDescontado = capitalNoDescontado;
    }

    public Double getInteresNoDescontado() {
        return interesNoDescontado;
    }

    public void setInteresNoDescontado(Double interesNoDescontado) {
        this.interesNoDescontado = interesNoDescontado;
    }

    public Double getDesgravamenNoDescontado() {
        return desgravamenNoDescontado;
    }

    public void setDesgravamenNoDescontado(Double desgravamenNoDescontado) {
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
