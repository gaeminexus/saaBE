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
import jakarta.persistence.Table;

/**
 * Representa CRD.CFCR (ConfiguracionCalificacionRiesgo) — DDL en {@code sql/177}.
 * PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md.
 *
 * <b>NO es la misma clasificación que las bandas contables</b> ({@link ConfiguracionBandaProducto}
 * / {@link BandaProducto}): la banda dice a qué CUENTA CONTABLE va el saldo; esta dice cuánta
 * PROVISIÓN se constituye para el reporte regulatorio G48. Ningún corte coincide entre las dos.
 *
 * <b>{@code empresa} es un número plano, sin FK a propósito</b> — igual criterio que el resto de
 * columnas de trazabilidad sin FK del módulo. {@code null} = aplica a cualquier empresa, que es
 * como quedó cargada la parametrización inicial (idéntica a lo cableado).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CFCR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ConfiguracionCalificacionRiesgoAll", query = "select e from ConfiguracionCalificacionRiesgo e"),
    @NamedQuery(name = "ConfiguracionCalificacionRiesgoId",  query = "select e from ConfiguracionCalificacionRiesgo e where e.codigo = :id")
})
public class ConfiguracionCalificacionRiesgo implements Serializable {

    @Id
    @Basic
    @Column(name = "CFCRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @ManyToOne
    @JoinColumn(name = "PRDCCDGO", referencedColumnName = "PRDCCDGO")
    private Producto producto;

    /** Empresa (SCP.PJRQ). Sin FK — ver el javadoc de la clase. {@code null} = todas. */
    @Basic
    @Column(name = "PJRQCDGO")
    private Long idEmpresa;

    /** Nombre de la escala, p.ej. "HIPOTECARIA" / "GENERAL" — informativo. */
    @Basic
    @Column(name = "CFCRNMBR", length = 100)
    private String nombre;

    @Basic
    @Column(name = "CFCRFCIN")
    private LocalDate fechaInicio;

    /** {@code null} = vigencia abierta. */
    @Basic
    @Column(name = "CFCRFCFN")
    private LocalDate fechaFin;

    @Basic
    @Column(name = "CFCRFCRG")
    private LocalDateTime fechaRegistro;

    @Basic
    @Column(name = "CFCRUSAR", length = 50)
    private String usuarioRegistro;

    @Basic
    @Column(name = "CFCRESTD")
    private Long estado;

    public ConfiguracionCalificacionRiesgo() {
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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
