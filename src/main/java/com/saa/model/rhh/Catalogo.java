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
 * Catálogo de tipos de permisos y licencias.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CTLG", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "CatalogoId", query = "select e from Catalogo e where e.codigo=:id"),
    @NamedQuery(name = "CatalogoAll", query = "select e from Catalogo e")
})
public class Catalogo implements Serializable, EntidadAuditableFecha {

    /**
     * Código único del tipo de permiso.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CTLGCDGO")
    private Long codigo;

    /**
     * Nombre del permiso o licencia.
     */
    @Basic
    @Column(name = "CTLGNMBR")
    private String nombre;

    /**
     * Indica si requiere documento (S/N).
     */
    @Basic
    @Column(name = "CTLGRQDC")
    private String requiereDocumento;

    /**
     * Indica si es con goce (S/N).
     */
    @Basic
    @Column(name = "CTLGGCEE")
    private String conGoce;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "CTLGESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CTLGFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "CTLGUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Empresa propietaria del catalogo (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Tipo de ausencia al que corresponde: detalle del rubro RHH_TIPO_AUSENCIA.
     */
    @Basic
    @Column(name = "CTLGTPAS")
    private Long tipoAusencia;

    /**
     * Maximo de dias permitidos para este tipo de permiso.
     */
    @Basic
    @Column(name = "CTLGMXDI")
    private Integer maximoDias;

    /**
     * El permiso se descuenta de la nomina (S/N).
     */
    @Basic
    @Column(name = "CTLGDSNM", length = 1)
    private String descuentaNomina;

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

    public String getRequiereDocumento() {
        return requiereDocumento;
    }

    public void setRequiereDocumento(String requiereDocumento) {
        this.requiereDocumento = requiereDocumento;
    }

    public String getConGoce() {
        return conGoce;
    }

    public void setConGoce(String conGoce) {
        this.conGoce = conGoce;
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

    public Long getTipoAusencia() {
        return tipoAusencia;
    }

    public void setTipoAusencia(Long tipoAusencia) {
        this.tipoAusencia = tipoAusencia;
    }

    public Integer getMaximoDias() {
        return maximoDias;
    }

    public void setMaximoDias(Integer maximoDias) {
        this.maximoDias = maximoDias;
    }

    public String getDescuentaNomina() {
        return descuentaNomina;
    }

    public void setDescuentaNomina(String descuentaNomina) {
        this.descuentaNomina = descuentaNomina;
    }
}
