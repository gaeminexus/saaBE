package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla VGCN (VigenciaContrato): historial de vigencias de aporte de un
 * {@link Contrato}, una fila por contrato + tipo de aporte + periodo. El valor operativo
 * es siempre {@code monto}; el porcentaje sólo recalcula el monto al CREAR una vigencia,
 * nunca al vuelo (ver docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md D7/D8).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "VGCN", schema = "CRD")
@SequenceGenerator(name = "SQ_VGCNCDGO", sequenceName = "CRD.SQ_VGCNCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "VigenciaContratoAll", query = "select e from VigenciaContrato e"),
    @NamedQuery(name = "VigenciaContratoId", query = "select e from VigenciaContrato e where e.codigo = :id")
})
public class VigenciaContrato implements Serializable {

    /** Código de la vigencia. */
    @Id
    @Basic
    @Column(name = "VGCNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_VGCNCDGO")
    private Long codigo;

    /** FK Contrato. */
    @ManyToOne
    @JoinColumn(name = "CNTRCDGO", referencedColumnName = "CNTRCDGO")
    private Contrato contrato;

    /** FK Tipo de Aporte (9 jubilación, 11 cesantía). */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** Inicio de vigencia. */
    @Basic
    @Column(name = "VGCNFCIN")
    private LocalDate fechaInicio;

    /** Fin de vigencia; NULL = vigente. */
    @Basic
    @Column(name = "VGCNFCFN")
    private LocalDate fechaFin;

    /** Monto mensual OPERATIVO: el valor que efectivamente se cobra. */
    @Basic
    @Column(name = "VGCNMNTO")
    private Double monto;

    /** Porcentaje aplicado; NULL si el modo es FIJO. */
    @Basic
    @Column(name = "VGCNPRCN")
    private Double porcentaje;

    /** Remuneración usada en el cálculo; NULL si el modo es FIJO. */
    @Basic
    @Column(name = "VGCNRMUN")
    private Double remuneracion;

    /** Modo (rubro 236): 1 CALCULADO, 2 FIJO -> ver {@link com.saa.rubros.CrdModoVigenciaContrato}. */
    @Basic
    @Column(name = "VGCNMODO")
    private Long modo;

    /** HSTRCDGO de origen en la migración, sin FK, sólo trazabilidad. */
    @Basic
    @Column(name = "VGCNIDHS")
    private Long idHistorialSueldo;

    /** Observación. */
    @Basic
    @Column(name = "VGCNOBSR", length = 2000)
    private String observacion;

    /** Estado: 1 activo, 0 anulado. */
    @Basic
    @Column(name = "VGCNIDST")
    private Long idEstado;

    /** Usuario de registro. */
    @Basic
    @Column(name = "VGCNUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha de registro. */
    @Basic
    @Column(name = "VGCNFCRG")
    private LocalDateTime fechaRegistro;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public Double getRemuneracion() {
        return remuneracion;
    }

    public void setRemuneracion(Double remuneracion) {
        this.remuneracion = remuneracion;
    }

    public Long getModo() {
        return modo;
    }

    public void setModo(Long modo) {
        this.modo = modo;
    }

    public Long getIdHistorialSueldo() {
        return idHistorialSueldo;
    }

    public void setIdHistorialSueldo(Long idHistorialSueldo) {
        this.idHistorialSueldo = idHistorialSueldo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
