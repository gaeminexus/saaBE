package com.saa.model.rpr;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * RPR.SG41 — historia del estado de los partícipes reportado a la Superintendencia en la G41 (D-038, 29-oct-2026).
 *
 * <p>La G41 es mensual y trae solo a los partícipes "que ingresen, actualicen o eliminen información" (Manual de
 * Catastros FCPC v1.2, §2.2): I = nuevo, A = cambió el estado civil, el estado del partícipe o la relación laboral.
 * Para saber qué cambió hay que conocer lo último reportado: este registro guarda, por sujeto y período, el estado
 * del partícipe (A activo / P pasivo, tabla 195). Una fila por sujeto y período, así un mes pasado se puede
 * regenerar sin perder el estado anterior.</p>
 *
 * <p>Origen: {@code SEMILLA} (estado de mayo 2025 de la G42 presentada, sql 337), {@code HM41} (G41 presentada en
 * mayo 2025) o {@code EJECUCION} (G41 generada por el SAA; {@link #detalleEjecucion} = EJRD de esa G41).</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "SG41", schema = "RPR")
public class SujetoReportadoG41 implements Serializable {

    public static final String ORIGEN_SEMILLA   = "SEMILLA";
    public static final String ORIGEN_HM41      = "HM41";
    public static final String ORIGEN_EJECUCION = "EJECUCION";

    @Id
    @Basic
    @Column(name = "SG41CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del partícipe (C/E). */
    @Basic
    @Column(name = "SG41TIDP", length = 1)
    private String tipoIdentificacion;

    /** Identificación del partícipe. */
    @Basic
    @Column(name = "SG41IDPR", length = 13)
    private String identificacion;

    /** Estado del partícipe desde ese período (A/P, tabla 195). */
    @Basic
    @Column(name = "SG41ESPR", length = 1)
    private String estadoParticipe;

    /** Estado del registro enviado (I/A/E, tabla 11); nulo en la semilla (no se envió). */
    @Basic
    @Column(name = "SG41ESRG", length = 1)
    private String estadoRegistro;

    /** Período AAAAMM al que corresponde el estado. */
    @Basic
    @Column(name = "SG41PRDO")
    private Long periodo;

    /** SEMILLA | HM41 | EJECUCION. */
    @Basic
    @Column(name = "SG41ORGN", length = 10)
    private String origen;

    /** EJRD de la G41 que lo reportó (solo EJECUCION). Id, sin relación, para no atar la limpieza de EJRD. */
    @Basic
    @Column(name = "SG41EJRD")
    private Long detalleEjecucion;

    /** Fecha en que se registró. */
    @Basic
    @Column(name = "SG41FCRG")
    private LocalDate fechaRegistro;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getEstadoParticipe() { return estadoParticipe; }
    public void setEstadoParticipe(String estadoParticipe) { this.estadoParticipe = estadoParticipe; }
    public String getEstadoRegistro() { return estadoRegistro; }
    public void setEstadoRegistro(String estadoRegistro) { this.estadoRegistro = estadoRegistro; }
    public Long getPeriodo() { return periodo; }
    public void setPeriodo(Long periodo) { this.periodo = periodo; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public Long getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(Long detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
