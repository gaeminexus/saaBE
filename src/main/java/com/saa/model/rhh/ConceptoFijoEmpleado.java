package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

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
 * Concepto de nomina fijo y vigente para un empleado (bonos, movilizacion, alimentacion). El motor lo recoge en cada periodo dentro de su rango de vigencia.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CPXM", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ConceptoFijoEmpleadoId", query = "select e from ConceptoFijoEmpleado e where e.codigo=:id"),
    @NamedQuery(name = "ConceptoFijoEmpleadoAll", query = "select e from ConceptoFijoEmpleado e")
})
public class ConceptoFijoEmpleado implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del concepto fijo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CPXMCDGO")
    private Long codigo;

    /**
     * Empleado al que se aplica el concepto.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Contrato al que se ata el concepto; nulo lo aplica a cualquier contrato vigente.
     */
    @ManyToOne
    @JoinColumn(name = "CNTECDGO", referencedColumnName = "CNTECDGO")
    private ContratoEmpleado contrato;

    /**
     * Concepto del catalogo que se aplica.
     */
    @ManyToOne
    @JoinColumn(name = "CPNMCDGO", referencedColumnName = "CPNMCDGO")
    private ConceptoNomina concepto;

    /**
     * Valor fijo del concepto para este empleado.
     */
    @Basic
    @Column(name = "CPXMVLRR")
    private Double valor;

    /**
     * Porcentaje, cuando el concepto es porcentual.
     */
    @Basic
    @Column(name = "CPXMPRCN")
    private Double porcentaje;

    /**
     * Cantidad, cuando el concepto es por cantidad.
     */
    @Basic
    @Column(name = "CPXMCANT")
    private Double cantidad;

    /**
     * Vigente desde.
     */
    @Basic
    @Column(name = "CPXMFCHI")
    private LocalDate fechaInicio;

    /**
     * Vigente hasta; nulo significa indefinido.
     */
    @Basic
    @Column(name = "CPXMFCHF")
    private LocalDate fechaFin;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "CPXMOBSR", length = 500)
    private String observacion;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "CPXMESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CPXMFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "CPXMUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public ContratoEmpleado getContrato() {
        return contrato;
    }

    public void setContrato(ContratoEmpleado contrato) {
        this.contrato = contrato;
    }

    public ConceptoNomina getConcepto() {
        return concepto;
    }

    public void setConcepto(ConceptoNomina concepto) {
        this.concepto = concepto;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
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

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
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
}
