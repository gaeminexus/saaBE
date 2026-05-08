package com.saa.model.crd;

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
 * Representa la tabla VPPC (Valores de Pago de Pensión Complementaria).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "VPPC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ValorPagoPensionComplementariaAll", query = "select e from ValorPagoPensionComplementaria e"),
    @NamedQuery(name = "ValorPagoPensionComplementariaId", query = "select e from ValorPagoPensionComplementaria e where e.codigo = :id")
})
public class ValorPagoPensionComplementaria implements Serializable {

    /**
     * Código único.
     */
    @Id
    @Basic
    @Column(name = "VPPCCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK - Código Entidad.
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * Valor a pagar (Valor Real o esperado del pago).
     */
    @Basic
    @Column(name = "VPPCVLRR")
    private Double valorPagar;

    /**
     * Número de cuotas (opcional).
     */
    @Basic
    @Column(name = "VPPCNMCT")
    private Long numeroCuotas;

    /**
     * Indica si tiene préstamo (1: Sí, 0: No).
     */
    @Basic
    @Column(name = "VPPCTNPR")
    private Long tienePrestamo;

    /**
     * Valor del seguro aplicado.
     */
    @Basic
    @Column(name = "VPPCVLSR")
    private Double valorSeguro;

    /**
     * ID Estado del registro.
     */
    @Basic
    @Column(name = "VPPCIDST")
    private Long estado;

    /**
     * Usuario de ingreso.
     */
    @Basic
    @Column(name = "VPPCUSRG", length = 50)
    private String usuarioIngreso;

    /**
     * Fecha de ingreso.
     */
    @Basic
    @Column(name = "VPPCFCRG")
    private LocalDateTime fechaIngreso;

    /**
     * Usuario de modificación.
     */
    @Basic
    @Column(name = "VPPCUSMD", length = 50)
    private String usuarioModificacion;

    /**
     * Fecha de modificación.
     */
    @Basic
    @Column(name = "VPPCCFMD")
    private LocalDateTime fechaModificacion;

    // ======================
    // Getters y Setters
    // ======================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Double getValorPagar() {
        return valorPagar;
    }

    public void setValorPagar(Double valorPagar) {
        this.valorPagar = valorPagar;
    }

    public Long getNumeroCuotas() {
        return numeroCuotas;
    }

    public void setNumeroCuotas(Long numeroCuotas) {
        this.numeroCuotas = numeroCuotas;
    }

    public Long getTienePrestamo() {
        return tienePrestamo;
    }

    public void setTienePrestamo(Long tienePrestamo) {
        this.tienePrestamo = tienePrestamo;
    }

    public Double getValorSeguro() {
        return valorSeguro;
    }

    public void setValorSeguro(Double valorSeguro) {
        this.valorSeguro = valorSeguro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    @Override
    public String toString() {
        return "ValorPagoPensionComplementaria [codigo=" + codigo + ", entidad=" + 
               (entidad != null ? entidad.getCodigo() : null) + ", valorPagar=" + valorPagar + 
               ", numeroCuotas=" + numeroCuotas + ", tienePrestamo=" + tienePrestamo + 
               ", valorSeguro=" + valorSeguro + ", estado=" + estado + "]";
    }
}
