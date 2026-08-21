package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
import jakarta.persistence.UniqueConstraint;

/**
 * Resumen diario de asistencia por empleado.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RSMN",schema = "RHH",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "MPLDCDGO", "RSMNFCHA" })
    }
)
@NamedQueries({
    @NamedQuery(name = "ResumenNominaId", query = "select e from ResumenNomina e where e.codigo=:id"),
    @NamedQuery(name = "ResumenNominaAll", query = "select e from ResumenNomina e")
})
public class ResumenNomina implements Serializable, EntidadAuditableFecha {

    /**
     * Código único del resumen.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "RSMNCDGO")
    private Long codigo;

    /**
     * Empleado.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Fecha del resumen.
     */
    @Basic
    @Column(name = "RSMNFCHA", nullable = false)
    private LocalDate fecha;

    /**
     * Hora de entrada (HH24:MI).
     */
    @Basic
    @Column(name = "RSMNENTR")
    private String horaEntrada;

    /**
     * Hora de salida (HH24:MI).
     */
    @Basic
    @Column(name = "RSMNSLDA")
    private String horaSalida;

    /**
     * Minutos de tardanza.
     */
    @Basic
    @Column(name = "RSMNTRDE")
    private Integer minutosTarde;

    /**
     * Minutos extra.
     */
    @Basic
    @Column(name = "RSMNEXTR")
    private Integer minutosExtra;

    /**
     * Indica ausencia (S/N).
     */
    @Basic
    @Column(name = "RSMNASNT")
    private String ausencia;

    /**
     * Indica justificación (S/N).
     */
    @Basic
    @Column(name = "RSMNJSTF")
    private String justificado;

    /**
     * Fuente del registro (CALCULO / AJUSTE).
     */
    @Basic
    @Column(name = "RSMNFNTE")
    private Long fuente;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "RSMNFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "RSMNUSRR")
    private String usuarioRegistro;


    /**
     * Horas efectivamente trabajadas en el dia.
     */
    @Basic
    @Column(name = "RSMNHRTR")
    private Double horasTrabajadas;

    /**
     * Horas suplementarias, las del 50 por ciento.
     */
    @Basic
    @Column(name = "RSMNHRSP")
    private Double horasSuplementarias;

    /**
     * Horas extraordinarias, las del 100 por ciento.
     */
    @Basic
    @Column(name = "RSMNHREX")
    private Double horasExtraordinarias;

    /**
     * Horas de jornada ordinaria con recargo nocturno.
     */
    @Basic
    @Column(name = "RSMNHRNC")
    private Double horasNocturnas;

    /**
     * Minutos de salida anticipada.
     */
    @Basic
    @Column(name = "RSMNSLAN")
    private Integer minutosSalidaAnticipada;

    /**
     * Hora de entrada real, de la primera marcacion del dia.
     */
    @Basic
    @Column(name = "RSMNENTT")
    private LocalDateTime entradaReal;

    /**
     * Hora de salida real, de la ultima marcacion del dia.
     */
    @Basic
    @Column(name = "RSMNSLDT")
    private LocalDateTime salidaReal;

    /**
     * Marcaciones inconsistentes (S/N). Se marca cuando el dia tiene un numero impar de
     * marcaciones: el sistema no adivina cual falta, lo deja para revision manual.
     */
    @Basic
    @Column(name = "RSMNINCN", length = 1)
    private String inconsistente;

    /**
     * Ya fue procesado en un periodo cerrado (S/N).
     */
    @Basic
    @Column(name = "RSMNPRCS", length = 1)
    private String procesado;

    /**
     * Justificacion de la correccion manual.
     */
    @Basic
    @Column(name = "RSMNJSTC", length = 300)
    private String justificacion;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Tipo de ausencia del dia: detalle del rubro RHH_TIPO_AUSENCIA. El motor descuenta de los dias trabajados los de tipo FALTA_INJUSTIFICADA y PERMISO_SIN_GOCE.
     */
    @Basic
    @Column(name = "RSMNTPAS")
    private Long tipoAusencia;

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

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(String horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public String getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(String horaSalida) {
        this.horaSalida = horaSalida;
    }

    public Integer getMinutosTarde() {
        return minutosTarde;
    }

    public void setMinutosTarde(Integer minutosTarde) {
        this.minutosTarde = minutosTarde;
    }

    public Integer getMinutosExtra() {
        return minutosExtra;
    }

    public void setMinutosExtra(Integer minutosExtra) {
        this.minutosExtra = minutosExtra;
    }

    public String getAusencia() {
        return ausencia;
    }

    public void setAusencia(String ausencia) {
        this.ausencia = ausencia;
    }

    public String getJustificado() {
        return justificado;
    }

    public void setJustificado(String justificado) {
        this.justificado = justificado;
    }

    public Long getFuente() {
        return fuente;
    }

    public void setFuente(Long fuente) {
        this.fuente = fuente;
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

    public Long getTipoAusencia() {
        return tipoAusencia;
    }

    public void setTipoAusencia(Long tipoAusencia) {
        this.tipoAusencia = tipoAusencia;
    }

    public Double getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(Double horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    public Double getHorasSuplementarias() {
        return horasSuplementarias;
    }

    public void setHorasSuplementarias(Double horasSuplementarias) {
        this.horasSuplementarias = horasSuplementarias;
    }

    public Double getHorasExtraordinarias() {
        return horasExtraordinarias;
    }

    public void setHorasExtraordinarias(Double horasExtraordinarias) {
        this.horasExtraordinarias = horasExtraordinarias;
    }

    public Double getHorasNocturnas() {
        return horasNocturnas;
    }

    public void setHorasNocturnas(Double horasNocturnas) {
        this.horasNocturnas = horasNocturnas;
    }

    public Integer getMinutosSalidaAnticipada() {
        return minutosSalidaAnticipada;
    }

    public void setMinutosSalidaAnticipada(Integer minutosSalidaAnticipada) {
        this.minutosSalidaAnticipada = minutosSalidaAnticipada;
    }

    public LocalDateTime getEntradaReal() {
        return entradaReal;
    }

    public void setEntradaReal(LocalDateTime entradaReal) {
        this.entradaReal = entradaReal;
    }

    public LocalDateTime getSalidaReal() {
        return salidaReal;
    }

    public void setSalidaReal(LocalDateTime salidaReal) {
        this.salidaReal = salidaReal;
    }

    public String getInconsistente() {
        return inconsistente;
    }

    public void setInconsistente(String inconsistente) {
        this.inconsistente = inconsistente;
    }

    public String getProcesado() {
        return procesado;
    }

    public void setProcesado(String procesado) {
        this.procesado = procesado;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }
}
