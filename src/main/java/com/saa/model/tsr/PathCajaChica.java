package com.saa.model.tsr;

import java.io.Serializable;
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
 * Entity PathCajaChica.
 * Archivo digitalizado (comprobante) de un movimiento de caja chica. Tabla: TSR.PTCH.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PTCH", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "PathCajaChicaAll", query = "select e from PathCajaChica e"),
    @NamedQuery(name = "PathCajaChicaId", query = "select e from PathCajaChica e where e.codigo = :id")
})
public class PathCajaChica implements Serializable {

    @Id
    @Basic
    @Column(name = "PTCHCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Movimiento de caja chica al que pertenece el documento. FK a TSR.MVCH. */
    @ManyToOne
    @JoinColumn(name = "MVCHCDGO", referencedColumnName = "MVCHCDGO")
    private MovimientoCajaChica movimiento;

    /** Ruta del archivo devuelta por FileService. */
    @Basic
    @Column(name = "PTCHPATH", length = 1000)
    private String path;

    /** Nombre original del documento. */
    @Basic
    @Column(name = "PTCHNMDC", length = 500)
    private String nombreDoc;

    /** Tipo de documento: FACTURA, RECIBO, VALE, OTRO. */
    @Basic
    @Column(name = "PTCHTPDC", length = 50)
    private String tipoDoc;

    /** Fecha de registro. */
    @Basic
    @Column(name = "PTCHFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registra. */
    @Basic
    @Column(name = "PTCHUSAR")
    private Long usuario;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public MovimientoCajaChica getMovimiento() { return movimiento; }
    public void setMovimiento(MovimientoCajaChica movimiento) { this.movimiento = movimiento; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getNombreDoc() { return nombreDoc; }
    public void setNombreDoc(String nombreDoc) { this.nombreDoc = nombreDoc; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Long getUsuario() { return usuario; }
    public void setUsuario(Long usuario) { this.usuario = usuario; }
}
