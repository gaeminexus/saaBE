package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

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
 * Provision mensual de beneficios sociales. Se genera en calcularPeriodo para las modalidades acumuladas y alimenta el asiento de provisiones.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PVNM", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ProvisionNominaId", query = "select e from ProvisionNomina e where e.codigo=:id"),
    @NamedQuery(name = "ProvisionNominaAll", query = "select e from ProvisionNomina e")
})
public class ProvisionNomina implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la provision.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "PVNMCDGO")
    private Long codigo;

    /**
     * Periodo que la genero.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Empleado al que corresponde.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Concepto de provision asociado; da la cuenta contable.
     */
    @ManyToOne
    @JoinColumn(name = "CPNMCDGO", referencedColumnName = "CPNMCDGO")
    private ConceptoNomina conceptoNomina;

    /**
     * Tipo de provision: detalle del rubro RHH_TIPO_PROVISION.
     */
    @Basic
    @Column(name = "PVNMTPPR")
    private Long tipoProvision;

    /**
     * Base sobre la que se calculo la provision.
     */
    @Basic
    @Column(name = "PVNMBSCL")
    private Double baseCalculo;

    /**
     * Valor provisionado en el mes.
     */
    @Basic
    @Column(name = "PVNMVLOR")
    private Double valor;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "PVNMESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PVNMFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "PVNMUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public ConceptoNomina getConceptoNomina() {
        return conceptoNomina;
    }

    public void setConceptoNomina(ConceptoNomina conceptoNomina) {
        this.conceptoNomina = conceptoNomina;
    }

    public Long getTipoProvision() {
        return tipoProvision;
    }

    public void setTipoProvision(Long tipoProvision) {
        this.tipoProvision = tipoProvision;
    }

    public Double getBaseCalculo() {
        return baseCalculo;
    }

    public void setBaseCalculo(Double baseCalculo) {
        this.baseCalculo = baseCalculo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
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
