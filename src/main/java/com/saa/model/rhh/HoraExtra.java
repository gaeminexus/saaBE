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
 * Hora extra tipificada al 50, al 100 o con recargo nocturno del 25. El porcentaje de recargo sale del PDTRVLRN del rubro RHH_TIPO_HORA_EXTRA, nunca del codigo.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HREX", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "HoraExtraId", query = "select e from HoraExtra e where e.codigo=:id"),
    @NamedQuery(name = "HoraExtraAll", query = "select e from HoraExtra e")
})
public class HoraExtra implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la hora extra.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "HREXCDGO")
    private Long codigo;

    /**
     * Empleado que las genero.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Resumen diario de asistencia que las origino.
     */
    @ManyToOne
    @JoinColumn(name = "RSMNCDGO", referencedColumnName = "RSMNCDGO")
    private ResumenNomina resumenNomina;

    /**
     * Periodo en el que se pagaron.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Tipo: detalle del rubro RHH_TIPO_HORA_EXTRA.
     */
    @Basic
    @Column(name = "HREXTPHR")
    private Long tipoHoraExtra;

    /**
     * Fecha en que se generaron.
     */
    @Basic
    @Column(name = "HREXFCHA")
    private LocalDate fecha;

    /**
     * Numero de horas.
     */
    @Basic
    @Column(name = "HREXHORS")
    private Double horas;

    /**
     * Valor de la hora ordinaria usado en el calculo.
     */
    @Basic
    @Column(name = "HREXVLHR")
    private Double valorHora;

    /**
     * Porcentaje de recargo aplicado.
     */
    @Basic
    @Column(name = "HREXRCRG")
    private Double recargo;

    /**
     * Valor total a pagar.
     */
    @Basic
    @Column(name = "HREXVLOR")
    private Double valor;

    /**
     * Aprobada para pago (S/N). El motor solo toma las aprobadas.
     */
    @Basic
    @Column(name = "HREXAPRB", length = 1)
    private String aprobada;

    /**
     * Usuario que aprobo.
     */
    @Basic
    @Column(name = "HREXUSAP", length = 60)
    private String usuarioAprueba;

    /**
     * Fecha de aprobacion.
     */
    @Basic
    @Column(name = "HREXFCAP")
    private LocalDate fechaAprobacion;

    /**
     * Excede el tope legal diario o semanal y requiere aprobacion excepcional (S/N).
     */
    @Basic
    @Column(name = "HREXEXCP", length = 1)
    private String excedeTope;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "HREXOBSR", length = 300)
    private String observacion;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "HREXESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "HREXFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "HREXUSRR", length = 60)
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

    public ResumenNomina getResumenNomina() {
        return resumenNomina;
    }

    public void setResumenNomina(ResumenNomina resumenNomina) {
        this.resumenNomina = resumenNomina;
    }

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public Long getTipoHoraExtra() {
        return tipoHoraExtra;
    }

    public void setTipoHoraExtra(Long tipoHoraExtra) {
        this.tipoHoraExtra = tipoHoraExtra;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getHoras() {
        return horas;
    }

    public void setHoras(Double horas) {
        this.horas = horas;
    }

    public Double getValorHora() {
        return valorHora;
    }

    public void setValorHora(Double valorHora) {
        this.valorHora = valorHora;
    }

    public Double getRecargo() {
        return recargo;
    }

    public void setRecargo(Double recargo) {
        this.recargo = recargo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
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

    public String getExcedeTope() {
        return excedeTope;
    }

    public void setExcedeTope(String excedeTope) {
        this.excedeTope = excedeTope;
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
