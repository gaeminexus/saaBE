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
 * RPR.SG45 — registro de sujetos de riesgo ya reportados a la Superintendencia en la G45 (D-035, 29-oct-2026).
 *
 * <p>La Superintendencia mantiene una base acumulada de sujetos: el control de la G46 exige que el sujeto
 * "conste en la base de datos de la estructura G45" (Manual de Estructuras de Operaciones de Crédito FCPC
 * v2.4, §5.2). La G45 del mes solo trae a los sujetos de las operaciones del mes que todavía no están en esa
 * base; este registro es la copia local de esa base. Origen: {@code SEMILLA} (reportados antes de que el SAA
 * generara la G45, sql 336), {@code HM45} (G45 presentada en mayo 2025) o {@code EJECUCION} (G45 generada por
 * el SAA; {@link #detalleEjecucion} = EJRD de esa G45).</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "SG45", schema = "RPR")
public class SujetoReportadoG45 implements Serializable {

    public static final String ORIGEN_SEMILLA   = "SEMILLA";
    public static final String ORIGEN_HM45      = "HM45";
    public static final String ORIGEN_EJECUCION = "EJECUCION";

    @Id
    @Basic
    @Column(name = "SG45CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto (C/E). */
    @Basic
    @Column(name = "SG45TIDS", length = 1)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "SG45IDSJ", length = 13)
    private String identificacion;

    /** Primer período reportado, AAAAMM (la semilla lleva 202504: "antes de mayo 2025"). */
    @Basic
    @Column(name = "SG45PRDO")
    private Long periodo;

    /** SEMILLA | HM45 | EJECUCION. */
    @Basic
    @Column(name = "SG45ORGN", length = 10)
    private String origen;

    /** EJRD de la G45 que lo reportó (solo origen EJECUCION). Id, sin relación, para no atar la limpieza de EJRD. */
    @Basic
    @Column(name = "SG45EJRD")
    private Long detalleEjecucion;

    /** Fecha en que se registró. */
    @Basic
    @Column(name = "SG45FCRG")
    private LocalDate fechaRegistro;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public Long getPeriodo() { return periodo; }
    public void setPeriodo(Long periodo) { this.periodo = periodo; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public Long getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(Long detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
