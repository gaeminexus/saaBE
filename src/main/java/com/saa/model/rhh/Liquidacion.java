package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import com.saa.basico.util.EntidadAuditableFecha;

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
 * Liquidación de salida del empleado (finiquito).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "LQDC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "LiquidacionId", query = "select e from Liquidacion e where e.codigo=:id"),
    @NamedQuery(name = "LiquidacionAll", query = "select e from Liquidacion e")
})
public class Liquidacion implements Serializable, EntidadAuditableFecha {

    /**
     * Código único de la liquidación.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "LQDCCDGO")
    private Long codigo;

    /**
     * Empleado liquidado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Contrato liquidado.
     */
    @ManyToOne
    @JoinColumn(name = "CNTECDGO", referencedColumnName = "CNTECDGO", nullable = false)
    private ContratoEmpleado contratoEmpleado;

    /**
     * Fecha de salida.
     */
    @Basic
    @Column(name = "LQDCFCHS", nullable = false)
    private LocalDate fechaSalida;

    /**
     * Motivo de salida.
     */
    @Basic
    @Column(name = "LQDCMTVO")
    private String motivo;

    /**
     * Neto a pagar.
     */
    @Basic
    @Column(name = "LQDCNETO")
    private Double neto;

    /**
     * Estado de la liquidación.
     */
    @Basic
    @Column(name = "LQDCESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "LQDCFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "LQDCUSRR")
    private String usuarioRegistro;


    /**
     * Causal de terminacion. Decide si hay desahucio, despido, decimos proporcionales y
     * vacaciones proporcionales.
     */
    @ManyToOne
    @JoinColumn(name = "CSTRCDGO", referencedColumnName = "CSTRCDGO")
    private CausalTerminacion causalTerminacion;

    /**
     * Fecha de ingreso, congelada para el calculo de la antiguedad.
     */
    @Basic
    @Column(name = "LQDCFCIN")
    private LocalDate fechaIngreso;

    /**
     * Anios de servicio cumplidos a la fecha de salida.
     */
    @Basic
    @Column(name = "LQDCANSR")
    private Double aniosServicio;

    /**
     * Ultima remuneracion, base del desahucio y de la indemnizacion.
     */
    @Basic
    @Column(name = "LQDCULRM")
    private Double ultimaRemuneracion;

    /**
     * Total de ingresos del finiquito.
     */
    @Basic
    @Column(name = "LQDCTTIN")
    private Double totalIngresos;

    /**
     * Total de descuentos del finiquito.
     */
    @Basic
    @Column(name = "LQDCTTDS")
    private Double totalDescuentos;

    /**
     * Bonificacion por desahucio (Art. 185 CT).
     */
    @Basic
    @Column(name = "LQDCDSHC")
    private Double desahucio;

    /**
     * Indemnizacion por despido intempestivo (Art. 188 CT).
     */
    @Basic
    @Column(name = "LQDCDSPD")
    private Double despidoIntempestivo;

    /**
     * Jubilacion patronal, cuando la causal la genera.
     */
    @Basic
    @Column(name = "LQDCJBPT")
    private Double jubilacionPatronal;

    /**
     * Numero del acta de finiquito en el SUT.
     */
    @Basic
    @Column(name = "LQDCACSU", length = 50)
    private String actaSut;

    /**
     * Fecha de registro en el SUT.
     */
    @Basic
    @Column(name = "LQDCFCSU")
    private LocalDate fechaSut;

    /**
     * Codigo del asiento contable de la liquidacion. Sin relacion JPA: cruza a CNT, igual
     * que <code>PRDNASNT</code>.
     */
    @Basic
    @Column(name = "ASNTCDGO")
    private Long asiento;

    /**
     * Fecha de aprobacion.
     */
    @Basic
    @Column(name = "LQDCFCAP")
    private LocalDate fechaAprobacion;

    /**
     * Usuario que aprobo.
     */
    @Basic
    @Column(name = "LQDCUSAP", length = 60)
    private String usuarioAprueba;

    // =============================
    // Getters y Setters
    // =============================

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
        return contratoEmpleado;
    }

    public void setContrato(ContratoEmpleado contrato) {
        this.contratoEmpleado = contrato;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Double getNeto() {
        return neto;
    }

    public void setNeto(Double neto) {
        this.neto = neto;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public CausalTerminacion getCausalTerminacion() {
        return causalTerminacion;
    }

    public void setCausalTerminacion(CausalTerminacion causalTerminacion) {
        this.causalTerminacion = causalTerminacion;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Double getAniosServicio() {
        return aniosServicio;
    }

    public void setAniosServicio(Double aniosServicio) {
        this.aniosServicio = aniosServicio;
    }

    public Double getUltimaRemuneracion() {
        return ultimaRemuneracion;
    }

    public void setUltimaRemuneracion(Double ultimaRemuneracion) {
        this.ultimaRemuneracion = ultimaRemuneracion;
    }

    public Double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(Double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public Double getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(Double totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public Double getDesahucio() {
        return desahucio;
    }

    public void setDesahucio(Double desahucio) {
        this.desahucio = desahucio;
    }

    public Double getDespidoIntempestivo() {
        return despidoIntempestivo;
    }

    public void setDespidoIntempestivo(Double despidoIntempestivo) {
        this.despidoIntempestivo = despidoIntempestivo;
    }

    public Double getJubilacionPatronal() {
        return jubilacionPatronal;
    }

    public void setJubilacionPatronal(Double jubilacionPatronal) {
        this.jubilacionPatronal = jubilacionPatronal;
    }

    public String getActaSut() {
        return actaSut;
    }

    public void setActaSut(String actaSut) {
        this.actaSut = actaSut;
    }

    public LocalDate getFechaSut() {
        return fechaSut;
    }

    public void setFechaSut(LocalDate fechaSut) {
        this.fechaSut = fechaSut;
    }

    public Long getAsiento() {
        return asiento;
    }

    public void setAsiento(Long asiento) {
        this.asiento = asiento;
    }

    public LocalDate getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDate fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getUsuarioAprueba() {
        return usuarioAprueba;
    }

    public void setUsuarioAprueba(String usuarioAprueba) {
        this.usuarioAprueba = usuarioAprueba;
    }
}
