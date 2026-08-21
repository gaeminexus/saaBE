package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import com.saa.basico.util.EntidadAuditableFecha;
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
 * Catálogo de tipos de contrato (plazo fijo, indefinido, etc.).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPCE", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "TipoContratoEmpleadoId", query = "select e from TipoContratoEmpleado e where e.codigo=:id"),
    @NamedQuery(name = "TipoContratoEmpleadoAll", query = "select e from TipoContratoEmpleado e")
})
public class TipoContratoEmpleado implements Serializable, EntidadAuditableFecha {

    /**
     * Código único del tipo de contrato.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "TPCECDGO")
    private Long codigo;

    /**
     * Nombre del tipo de contrato.
     */
    @Basic
    @Column(name = "TPCENMBR")
    private String nombre;

    /**
     * Indica si requiere fecha de fin (S/N).
     */
    @Basic
    @Column(name = "TPCERQRE")
    private String requiereFechaFin;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "TPCEESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "TPCEFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "TPCEUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Empresa propietaria del tipo de contrato (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Tipo de relacion laboral asociado: detalle del rubro RHH_TIPO_RELACION_LABORAL.
     */
    @Basic
    @Column(name = "TPCETPRL")
    private Long tipoRelacionLaboral;

    /**
     * Duracion maxima en meses permitida para este tipo de contrato.
     */
    @Basic
    @Column(name = "TPCEMXMS")
    private Integer duracionMaximaMeses;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRequiereFechaFin() {
        return requiereFechaFin;
    }

    public void setRequiereFechaFin(String requiereFechaFin) {
        this.requiereFechaFin = requiereFechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
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

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Long getTipoRelacionLaboral() {
        return tipoRelacionLaboral;
    }

    public void setTipoRelacionLaboral(Long tipoRelacionLaboral) {
        this.tipoRelacionLaboral = tipoRelacionLaboral;
    }

    public Integer getDuracionMaximaMeses() {
        return duracionMaximaMeses;
    }

    public void setDuracionMaximaMeses(Integer duracionMaximaMeses) {
        this.duracionMaximaMeses = duracionMaximaMeses;
    }
}
