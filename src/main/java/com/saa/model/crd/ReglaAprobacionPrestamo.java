package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.RGAP (ReglaAprobacionPrestamo); DDL en {@code crd/sql/370}.
 * Es la regla de aprobación de préstamos: a qué préstamos aplica (tipo, rango de monto, rango de plazo) y con qué prioridad. Sus niveles son NVAP.
 * Las llaves foráneas van como código simple (no {@code @ManyToOne}): la entidad se serializa directo a JSON y los EAGER
 * arrastrarían el grafo completo. Diseño: DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B4.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RGAP", schema = "CRD")
@SequenceGenerator(name = "SQ_RGAPCDGO", sequenceName = "CRD.SQ_RGAPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "ReglaAprobacionPrestamoAll", query = "select e from ReglaAprobacionPrestamo e"),
    @NamedQuery(name = "ReglaAprobacionPrestamoId",  query = "select e from ReglaAprobacionPrestamo e where e.codigo = :id")
})
public class ReglaAprobacionPrestamo implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "RGAPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RGAPCDGO")
    private Long codigo;

    /** Tipo de préstamo (CRD.TPPR); nulo = cualquier tipo. */
    @Basic
    @Column(name = "TPPRCDGO")
    private Long idTipoPrestamo;

    /** Nombre de la regla. */
    @Basic
    @Column(name = "RGAPNMBR")
    private String nombre;

    /** Monto desde (inclusive); nulo = sin mínimo. */
    @Basic
    @Column(name = "RGAPMNMN")
    private Double montoMinimo;

    /** Monto hasta (inclusive); nulo = sin máximo. */
    @Basic
    @Column(name = "RGAPMNMX")
    private Double montoMaximo;

    /** Plazo en cuotas desde (inclusive); nulo = sin mínimo. */
    @Basic
    @Column(name = "RGAPPLMN")
    private Long plazoMinimo;

    /** Plazo en cuotas hasta (inclusive); nulo = sin máximo. */
    @Basic
    @Column(name = "RGAPPLMX")
    private Long plazoMaximo;

    /** Prioridad: gana la de número menor; empate = error de configuración. */
    @Basic
    @Column(name = "RGAPPRRD")
    private Long prioridad;

    /** 1 activa, 0 inactiva. */
    @Basic
    @Column(name = "RGAPESTD")
    private Long estado;

    /** Usuario que registró. */
    @Basic
    @Column(name = "RGAPUSRG")
    private String usuarioRegistro;

    /** Fecha de registro. */
    @Basic
    @Column(name = "RGAPFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que modificó. */
    @Basic
    @Column(name = "RGAPUSMD")
    private String usuarioModificacion;

    /** Fecha de modificación. */
    @Basic
    @Column(name = "RGAPFCMD")
    private LocalDateTime fechaModificacion;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Long getIdTipoPrestamo() { return idTipoPrestamo; }
    public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Double getMontoMinimo() { return montoMinimo; }
    public void setMontoMinimo(Double montoMinimo) { this.montoMinimo = montoMinimo; }
    public Double getMontoMaximo() { return montoMaximo; }
    public void setMontoMaximo(Double montoMaximo) { this.montoMaximo = montoMaximo; }
    public Long getPlazoMinimo() { return plazoMinimo; }
    public void setPlazoMinimo(Long plazoMinimo) { this.plazoMinimo = plazoMinimo; }
    public Long getPlazoMaximo() { return plazoMaximo; }
    public void setPlazoMaximo(Long plazoMaximo) { this.plazoMaximo = plazoMaximo; }
    public Long getPrioridad() { return prioridad; }
    public void setPrioridad(Long prioridad) { this.prioridad = prioridad; }
    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }
    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }
}
