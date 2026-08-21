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
 * Novedad puntual del periodo: un ingreso o descuento por una sola vez. En la carga historica es la via para meter a mano dias trabajados y horas extra sin biometrico.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NVNM", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "NovedadNominaId", query = "select e from NovedadNomina e where e.codigo=:id"),
    @NamedQuery(name = "NovedadNominaAll", query = "select e from NovedadNomina e")
})
public class NovedadNomina implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la novedad.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "NVNMCDGO")
    private Long codigo;

    /**
     * Periodo al que se aplica la novedad.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Empleado afectado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Concepto con el que se aplica.
     */
    @ManyToOne
    @JoinColumn(name = "CPNMCDGO", referencedColumnName = "CPNMCDGO")
    private ConceptoNomina conceptoNomina;

    /**
     * Cantidad: horas, dias o unidades.
     */
    @Basic
    @Column(name = "NVNMCANT")
    private Double cantidad;

    /**
     * Valor de la novedad.
     */
    @Basic
    @Column(name = "NVNMVLRR")
    private Double valor;

    /**
     * Descripcion de la novedad.
     */
    @Basic
    @Column(name = "NVNMDSCR", length = 300)
    private String descripcion;

    /**
     * Aprobada para su inclusion en el calculo (S/N). El motor solo toma las aprobadas.
     */
    @Basic
    @Column(name = "NVNMAPRB", length = 1)
    private String aprobada;

    /**
     * Usuario que aprobo la novedad.
     */
    @Basic
    @Column(name = "NVNMUSAP", length = 60)
    private String usuarioAprueba;

    /**
     * Fecha de aprobacion.
     */
    @Basic
    @Column(name = "NVNMFCAP")
    private LocalDate fechaAprobacion;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "NVNMESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "NVNMFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "NVNMUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public ConceptoNomina getConceptoNomina() {
        return conceptoNomina;
    }

    public void setConceptoNomina(ConceptoNomina conceptoNomina) {
        this.conceptoNomina = conceptoNomina;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getAprobada() {
        return aprobada;
    }

    public void setAprobada(String aprobada) {
        this.aprobada = aprobada;
    }

    public String getUsuarioAprueba() {
        return usuarioAprueba;
    }

    public void setUsuarioAprueba(String usuarioAprueba) {
        this.usuarioAprueba = usuarioAprueba;
    }

    public LocalDate getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDate fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
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
