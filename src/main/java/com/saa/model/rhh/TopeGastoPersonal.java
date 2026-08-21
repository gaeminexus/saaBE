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
 * Tope de gastos personales expresado en numero de canastas basicas, segun el numero de cargas familiares.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPGP", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "TopeGastoPersonalId", query = "select e from TopeGastoPersonal e where e.codigo=:id"),
    @NamedQuery(name = "TopeGastoPersonalAll", query = "select e from TopeGastoPersonal e")
})
public class TopeGastoPersonal implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del tope.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "TPGPCDGO")
    private Long codigo;

    /**
     * Empresa propietaria del registro (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Anio fiscal al que corresponde el tope.
     */
    @Basic
    @Column(name = "TPGPANOO")
    private Integer anio;

    /**
     * Numero de cargas familiares.
     */
    @Basic
    @Column(name = "TPGPNCRG")
    private Integer numeroCargas;

    /**
     * Numero de canastas basicas de tope para ese numero de cargas.
     */
    @Basic
    @Column(name = "TPGPNCAN")
    private Double numeroCanastas;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "TPGPESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "TPGPFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "TPGPUSRR", length = 60)
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

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getNumeroCargas() {
        return numeroCargas;
    }

    public void setNumeroCargas(Integer numeroCargas) {
        this.numeroCargas = numeroCargas;
    }

    public Double getNumeroCanastas() {
        return numeroCanastas;
    }

    public void setNumeroCanastas(Double numeroCanastas) {
        this.numeroCanastas = numeroCanastas;
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
