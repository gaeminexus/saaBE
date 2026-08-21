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
 * Utilidades que corresponden a un empleado en un ejercicio.
 *
 * <p><b>Ojo con el par que se cruza:</b> <code>valorPorDias</code> y
 * <code>valorPorCargas</code> —en plural— son los <b>importes</b> de este empleado.
 * <code>Utilidad.valorPorDia</code> y <code>valorPorCarga</code> —en singular— son los
 * <b>coeficientes</b> de la empresa. Expresan la misma reparticion en dos niveles.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTUT", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DetalleUtilidadId",  query = "select e from DetalleUtilidad e where e.codigo=:id"),
    @NamedQuery(name = "DetalleUtilidadAll", query = "select e from DetalleUtilidad e")
})
public class DetalleUtilidad implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo del detalle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DTUTCDGO")
    private Long codigo;

    /**
     * Reparto al que pertenece.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "UTLDCDGO", referencedColumnName = "UTLDCDGO", nullable = false)
    private Utilidad utilidad;

    /**
     * Empleado beneficiario.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Dias trabajados del empleado en el ejercicio.
     */
    @Basic
    @Column(name = "DTUTDIAS")
    private Double dias;

    /**
     * Numero de cargas familiares.
     */
    @Basic
    @Column(name = "DTUTNCRG")
    private Integer numeroCargas;

    /**
     * Importe que le corresponde por dias trabajados.
     */
    @Basic
    @Column(name = "DTUTVL10")
    private Double valorPorDias;

    /**
     * Importe que le corresponde por cargas familiares.
     */
    @Basic
    @Column(name = "DTUTVL05")
    private Double valorPorCargas;

    /**
     * Total antes de aplicar el tope.
     */
    @Basic
    @Column(name = "DTUTTTAL")
    private Double total;

    /**
     * Excedente sobre el tope, que se transfiere al IESS.
     */
    @Basic
    @Column(name = "DTUTEXCD")
    private Double excedente;

    /**
     * Valor a pagar tras aplicar el tope.
     */
    @Basic
    @Column(name = "DTUTVLPG")
    private Double valorPagar;

    /**
     * Retencion de impuesto a la renta sobre las utilidades.
     */
    @Basic
    @Column(name = "DTUTRTIR")
    private Double retencionIr;

    /**
     * Estado del detalle.
     */
    @Basic
    @Column(name = "DTUTESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DTUTFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "DTUTUSRR", length = 60)
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Utilidad getUtilidad() {
        return utilidad;
    }

    public void setUtilidad(Utilidad utilidad) {
        this.utilidad = utilidad;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public Double getDias() {
        return dias;
    }

    public void setDias(Double dias) {
        this.dias = dias;
    }

    public Integer getNumeroCargas() {
        return numeroCargas;
    }

    public void setNumeroCargas(Integer numeroCargas) {
        this.numeroCargas = numeroCargas;
    }

    public Double getValorPorDias() {
        return valorPorDias;
    }

    public void setValorPorDias(Double valorPorDias) {
        this.valorPorDias = valorPorDias;
    }

    public Double getValorPorCargas() {
        return valorPorCargas;
    }

    public void setValorPorCargas(Double valorPorCargas) {
        this.valorPorCargas = valorPorCargas;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getExcedente() {
        return excedente;
    }

    public void setExcedente(Double excedente) {
        this.excedente = excedente;
    }

    public Double getValorPagar() {
        return valorPagar;
    }

    public void setValorPagar(Double valorPagar) {
        this.valorPagar = valorPagar;
    }

    public Double getRetencionIr() {
        return retencionIr;
    }

    public void setRetencionIr(Double retencionIr) {
        this.retencionIr = retencionIr;
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
