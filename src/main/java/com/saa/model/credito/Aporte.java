package com.saa.model.credito;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * Representa la tabla APRT (Aporte).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "APRT", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "AporteAll", query = "select e from Aporte e"),
    @NamedQuery(name = "AporteId", query = "select e from Aporte e where e.codigo = :id")
})
public class Aporte implements Serializable {

    /**
     * Código del aporte.
     */
    @Id
    @Basic
    @Column(name = "APRTCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * ID Filial.
     */
    /** FK - Código Tipo Préstamo */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /**
     * ID de la entidad.
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /*
     * ID del contrato.
     */
    @ManyToOne
    @JoinColumn(name = "CNTRCDGO", referencedColumnName = "CNTRCDGO")
    private Contrato contrato;
    
    
    /**
     * Código de tipo de aporte.
     */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /**
     * Fecha de transacción.
     */
    @Basic
    @Column(name = "APRTFCTR")
    private LocalDateTime fechaTransaccion;

    /**
     * Glosa.
     */
    @Basic
    @Column(name = "APRTGLSA", length = 2000)
    private String glosa;

    /**
     * Valor.
     */
    @Basic
    @Column(name = "APRTVLRR")
    private Double valor;

    /**
     * Valor pagado.
     */
    @Basic
    @Column(name = "APRTVLPG")
    private Double valorPagado;

    /**
     * Saldo.
     */
    @Basic
    @Column(name = "APRTSLDO")
    private Double saldo;

    /**
     * ID Sistema ASOPREP.
     */
    @Basic
    @Column(name = "APRTIDAS")
    private Long idAsoprep;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "APRTFCRG")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario de registro.
     */
    @Basic
    @Column(name = "APRTUSRG", length = 50)
    private String usuarioRegistro;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "APRTIDST")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Filial getFilia() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public void setContrato(Contrato contrato) {
        this.contrato = contrato;
    }

    public TipoAporte getTipoAporte() {
        return tipoAporte;
    }

    public void setTipoAporte(TipoAporte tipoAporte) {
        this.tipoAporte = tipoAporte;
    }

    public LocalDateTime getFechaTransaccion() {
        return fechaTransaccion;
    }

    public void setFechaTransaccion(LocalDateTime fechaTransaccion) {
        this.fechaTransaccion = fechaTransaccion;
    }

    public String getGlosa() {
        return glosa;
    }

    public void setGlosa(String glosa) {
        this.glosa = glosa;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getValorPagado() {
        return valorPagado;
    }

    public void setValorPagado(Double valorPagado) {
        this.valorPagado = valorPagado;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Long getIdAsoprep() {
        return idAsoprep;
    }

    public void setIdAsoprep(Long idAsoprep) {
        this.idAsoprep = idAsoprep;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

