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
import jakarta.persistence.Table;

/**
 * Representa la tabla ADTR (Auditoría / Trazabilidad).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ADTR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "AuditoriaAll", query = "select e from Auditoria e"),
    @NamedQuery(name = "AuditoriaId",  query = "select e from Auditoria e where e.codigo = :id")
})
public class Auditoria implements Serializable {

    /**
     * Código del registro de auditoría (PK).
     */
    @Id
    @Basic
    @Column(name = "ADTRCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Fecha y hora del evento de auditoría.
     */
    @Basic
    @Column(name = "ADTRFCHA")
    private LocalDateTime fechaEvento;

    /**
     * Sistema / aplicación (SAA, SIGCAL, etc.).
     */
    @Basic
    @Column(name = "ADTRSSTM", length = 50, nullable = false)
    private String sistema;

    /**
     * Módulo funcional (CONTABILIDAD, EMISION, etc.).
     */
    @Basic
    @Column(name = "ADTRMDLO", length = 100, nullable = false)
    private String modulo;

    /**
     * Acción realizada (CREATE, UPDATE, DELETE, ANULAR, APROBAR, etc.).
     */
    @Basic
    @Column(name = "ADTRACCN", length = 50, nullable = false)
    private String accion;

    /**
     * Entidad lógica o tabla afectada (ASIENTO, PREDIO, USUARIO, etc.).
     */
    @Basic
    @Column(name = "ADTRNTDD", length = 100, nullable = false)
    private String entidad;

    /**
     * Identificador del registro afectado (PK de la entidad).
     */
    @Basic
    @Column(name = "ADTRNTID", length = 100)
    private String idEntidad;

    /**
     * Usuario que ejecuta la acción.
     */
    @Basic
    @Column(name = "ADTRUSRO", length = 100)
    private String usuario;

    /**
     * Rol del usuario al ejecutar la acción.
     */
    @Basic
    @Column(name = "ADTRRLL", length = 100)
    private String rol;

    /**
     * Dirección IP del cliente.
     */
    @Basic
    @Column(name = "ADTRIPCL", length = 45)
    private String ipCliente;

    /**
     * Agente de usuario (navegador, cliente, etc.).
     */
    @Basic
    @Column(name = "ADTRAGNT", length = 255)
    private String userAgent;

    /**
     * Motivo u observación obligatoria.
     */
    @Basic
    @Column(name = "ADTRRSN", length = 1000, nullable = false)
    private String motivo;

    /**
     * Nombre del campo con valor anterior.
     */
    @Basic
    @Column(name = "ADTRNMNA", length = 100)
    private String nombreCampoAnterior;

    /**
     * Valor numérico anterior del campo.
     */
    @Basic
    @Column(name = "ADTRVLNA")
    private Long valorAnterior;

    /**
     * Nombre del campo con valor nuevo.
     */
    @Basic
    @Column(name = "ADTRNMNN", length = 100)
    private String nombreCampoNuevo;

    /**
     * Valor numérico nuevo del campo.
     */
    @Basic
    @Column(name = "ADTRVLNN")
    private Double valorNuevo;

    /**
     * Fecha de inserción del registro de auditoría.
     */
    @Basic
    @Column(name = "ADTRFCIN")
    private LocalDateTime fechaCreacion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDateTime fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public String getSistema() {
        return sistema;
    }

    public void setSistema(String sistema) {
        this.sistema = sistema;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(String idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getIpCliente() {
        return ipCliente;
    }

    public void setIpCliente(String ipCliente) {
        this.ipCliente = ipCliente;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getNombreCampoAnterior() {
        return nombreCampoAnterior;
    }

    public void setNombreCampoAnterior(String nombreCampoAnterior) {
        this.nombreCampoAnterior = nombreCampoAnterior;
    }

    public Long getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(Long valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getNombreCampoNuevo() {
        return nombreCampoNuevo;
    }

    public void setNombreCampoNuevo(String nombreCampoNuevo) {
        this.nombreCampoNuevo = nombreCampoNuevo;
    }

    public Double getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(Double valorNuevo) {
        this.valorNuevo = valorNuevo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
