package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.scp.Empresa;

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
 * Bitacora de las corridas de «Acreditar vacaciones»: una fila por corrida, manual o
 * disparada por el cierre de la nomina, con la fecha de corte que uso. Antes de esta tabla
 * la fecha de corte no se guardaba en ningun lado y nadie podia saber hasta cuando estaba
 * acreditado el anio.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRVC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "CorridaAcreditacionVacacionesId", query = "select e from CorridaAcreditacionVacaciones e where e.codigo=:id"),
    @NamedQuery(name = "CorridaAcreditacionVacacionesAll", query = "select e from CorridaAcreditacionVacaciones e")
})
public class CorridaAcreditacionVacaciones implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la corrida.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CRVCCDGO")
    private Long codigo;

    /**
     * Empresa a la que se acredito.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "PJRQCDGO", nullable = false)
    private Empresa empresa;

    /**
     * Anio del saldo acreditado (el anio de la fecha de corte).
     */
    @Basic
    @Column(name = "CRVCANOO", nullable = false)
    private Integer anio;

    /**
     * Fecha de corte con la que se evaluo la corrida.
     */
    @Basic
    @Column(name = "CRVCFCCR", nullable = false)
    private LocalDate fechaCorte;

    /**
     * Modalidad vigente en el anio (RHH.PRNM.PRNMMDVC): 1 por aniversario, 2 devengo mensual.
     * Nula si el anio no tiene parametros cargados.
     */
    @Basic
    @Column(name = "CRVCMDLD")
    private Long modalidad;

    /**
     * Empleados a los que la corrida les acredito el saldo.
     */
    @Basic
    @Column(name = "CRVCEMPL")
    private Integer empleadosAcreditados;

    /**
     * Origen: MANUAL o CIERRE_NOMINA.
     */
    @Basic
    @Column(name = "CRVCORGN", nullable = false)
    private String origen;

    /**
     * Estado: OK, ERROR u OMITIDA.
     */
    @Basic
    @Column(name = "CRVCESTD", nullable = false)
    private String estado;

    /**
     * Mensaje de error, o aviso de por que se omitio la corrida.
     */
    @Basic
    @Column(name = "CRVCMNSJ")
    private String mensaje;

    /**
     * Usuario que ejecuto la corrida.
     */
    @Basic
    @Column(name = "CRVCUSRR")
    private String usuarioRegistro;

    /**
     * Fecha y hora de registro.
     */
    @Basic
    @Column(name = "CRVCFCHR")
    private LocalDateTime fechaRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public LocalDate getFechaCorte() {
        return fechaCorte;
    }

    public void setFechaCorte(LocalDate fechaCorte) {
        this.fechaCorte = fechaCorte;
    }

    public Long getModalidad() {
        return modalidad;
    }

    public void setModalidad(Long modalidad) {
        this.modalidad = modalidad;
    }

    public Integer getEmpleadosAcreditados() {
        return empleadosAcreditados;
    }

    public void setEmpleadosAcreditados(Integer empleadosAcreditados) {
        this.empleadosAcreditados = empleadosAcreditados;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
