package com.saa.model.rhh;

import java.io.Serializable;
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
 * Causal de terminacion de la relacion laboral, con su articulo del Codigo del Trabajo y los efectos que dispara en la liquidacion.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CSTR", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "CausalTerminacionId", query = "select e from CausalTerminacion e where e.codigo=:id"),
    @NamedQuery(name = "CausalTerminacionAll", query = "select e from CausalTerminacion e")
})
public class CausalTerminacion implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la causal.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CSTRCDGO")
    private Long codigo;

    /**
     * Empresa propietaria del registro (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Nombre de la causal.
     */
    @Basic
    @Column(name = "CSTRNMBR", length = 150)
    private String nombre;

    /**
     * Codigo alterno del detalle del rubro RHH_CAUSAL_TERMINACION.
     */
    @Basic
    @Column(name = "CSTRALTR")
    private Long codigoAlterno;

    /**
     * Articulo del Codigo del Trabajo que la respalda.
     */
    @Basic
    @Column(name = "CSTRARTC", length = 50)
    private String articulo;

    /**
     * Genera bonificacion por desahucio (S/N).
     */
    @Basic
    @Column(name = "CSTRDSHC", length = 1)
    private String generaDesahucio;

    /**
     * Genera indemnizacion por despido intempestivo (S/N).
     */
    @Basic
    @Column(name = "CSTRDSPD", length = 1)
    private String generaDespido;

    /**
     * Paga vacaciones proporcionales (S/N).
     */
    @Basic
    @Column(name = "CSTRVCPR", length = 1)
    private String pagaVacacionesProporcionales;

    /**
     * Paga decimos proporcionales (S/N).
     */
    @Basic
    @Column(name = "CSTRDCPR", length = 1)
    private String pagaDecimosProporcionales;

    /**
     * Genera jubilacion patronal (S/N).
     */
    @Basic
    @Column(name = "CSTRJBPT", length = 1)
    private String generaJubilacionPatronal;

    /**
     * Requiere aviso de salida al IESS (S/N).
     */
    @Basic
    @Column(name = "CSTRAVSL", length = 1)
    private String requiereAvisoSalida;

    /**
     * Requiere acta de finiquito en el SUT (S/N).
     */
    @Basic
    @Column(name = "CSTRACSU", length = 1)
    private String requiereActaSut;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "CSTRESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CSTRFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "CSTRUSRR", length = 60)
    private String usuarioRegistro;

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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getCodigoAlterno() {
        return codigoAlterno;
    }

    public void setCodigoAlterno(Long codigoAlterno) {
        this.codigoAlterno = codigoAlterno;
    }

    public String getArticulo() {
        return articulo;
    }

    public void setArticulo(String articulo) {
        this.articulo = articulo;
    }

    public String getGeneraDesahucio() {
        return generaDesahucio;
    }

    public void setGeneraDesahucio(String generaDesahucio) {
        this.generaDesahucio = generaDesahucio;
    }

    public String getGeneraDespido() {
        return generaDespido;
    }

    public void setGeneraDespido(String generaDespido) {
        this.generaDespido = generaDespido;
    }

    public String getPagaVacacionesProporcionales() {
        return pagaVacacionesProporcionales;
    }

    public void setPagaVacacionesProporcionales(String pagaVacacionesProporcionales) {
        this.pagaVacacionesProporcionales = pagaVacacionesProporcionales;
    }

    public String getPagaDecimosProporcionales() {
        return pagaDecimosProporcionales;
    }

    public void setPagaDecimosProporcionales(String pagaDecimosProporcionales) {
        this.pagaDecimosProporcionales = pagaDecimosProporcionales;
    }

    public String getGeneraJubilacionPatronal() {
        return generaJubilacionPatronal;
    }

    public void setGeneraJubilacionPatronal(String generaJubilacionPatronal) {
        this.generaJubilacionPatronal = generaJubilacionPatronal;
    }

    public String getRequiereAvisoSalida() {
        return requiereAvisoSalida;
    }

    public void setRequiereAvisoSalida(String requiereAvisoSalida) {
        this.requiereAvisoSalida = requiereAvisoSalida;
    }

    public String getRequiereActaSut() {
        return requiereActaSut;
    }

    public void setRequiereActaSut(String requiereActaSut) {
        this.requiereActaSut = requiereActaSut;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
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
}
