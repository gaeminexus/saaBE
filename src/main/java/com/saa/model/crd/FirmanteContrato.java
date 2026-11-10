package com.saa.model.crd;

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
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Representa la tabla FRFR (FirmanteContrato): firmantes de un trámite de firma del contrato (sql/376). Los datos de contacto son una foto de lo enviado.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FRFR", schema = "CRD")
@SequenceGenerator(name = "SQ_FRFRCDGO", sequenceName = "CRD.SQ_FRFRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "FirmanteContratoAll", query = "select e from FirmanteContrato e"),
    @NamedQuery(name = "FirmanteContratoId", query = "select e from FirmanteContrato e where e.codigo = :id")
})
public class FirmanteContrato implements Serializable {

    /** Código. */
    @Id
    @Basic
    @Column(name = "FRFRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_FRFRCDGO")
    private Long codigo;

    /** Trámite (CRD.FRCN). */
    @Basic
    @Column(name = "FRCNCDGO")
    private Long idFirma;

    /** Orden de firma. */
    @Basic
    @Column(name = "FRFRORDN")
    private Long orden;

    /** Rol: ver {@link com.saa.rubros.RolFirmanteContrato}. */
    @Basic
    @Column(name = "FRFRROLL", length = 30)
    private String rol;

    /** Cédula, copiada al enviar. */
    @Basic
    @Column(name = "FRFRIDNT", length = 50)
    private String identificacion;

    /** Nombre, copiado al enviar. */
    @Basic
    @Column(name = "FRFRNMBR", length = 300)
    private String nombre;

    /** Correo, copiado al enviar. */
    @Basic
    @Column(name = "FRFRCRRO", length = 200)
    private String correo;

    /** Celular, copiado al enviar. */
    @Basic
    @Column(name = "FRFRCLLR", length = 50)
    private String celular;

    /** Partícipe (CRD.ENTD), solo si el firmante es el partícipe. */
    @Basic
    @Column(name = "ENTDCDGO")
    private Long idEntidad;

    /** Estado del firmante: ver {@link com.saa.rubros.EstadoFirmanteContrato}. */
    @Basic
    @Column(name = "FRFRESTD")
    private Long estado;

    /** Fecha de firma. */
    @Basic
    @Column(name = "FRFRFCFR")
    private LocalDateTime fechaFirma;

    /** Identificador del firmante en el proveedor. */
    @Basic
    @Column(name = "FRFRIDEX", length = 200)
    private String idExterno;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Long getIdFirma() { return idFirma; }
    public void setIdFirma(Long idFirma) { this.idFirma = idFirma; }
    public Long getOrden() { return orden; }
    public void setOrden(Long orden) { this.orden = orden; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
    public LocalDateTime getFechaFirma() { return fechaFirma; }
    public void setFechaFirma(LocalDateTime fechaFirma) { this.fechaFirma = fechaFirma; }
    public String getIdExterno() { return idExterno; }
    public void setIdExterno(String idExterno) { this.idExterno = idExterno; }
}
