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
 * Representa la tabla PGAP (PagoAporte).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PGAP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "PagoAporteAll", query = "select e from PagoAporte e"),
    @NamedQuery(name = "PagoAporteId", query = "select e from PagoAporte e where e.codigo = :id")
})
public class PagoAporte implements Serializable {

    /**
     * Código del pago.
     */
    @Id
    @Basic
    @Column(name = "PGAPCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Filial (empresa).
     */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /**
     * Aporte.
     */
    @ManyToOne
    @JoinColumn(name = "APRTCDGO", referencedColumnName = "APRTCDGO")
    private Aporte aporte;

    /**
     * Valor.
     */
    @Basic
    @Column(name = "PGAPVLRR")
    private Double valor;

    /**
     * Fecha contable.
     */
    @Basic
    @Column(name = "PGAPFCCN")
    private LocalDateTime fechaContable;

    /**
     * Número de asiento.
     */
    @Basic
    @Column(name = "PGAPNMAS")
    private Long numeroAsiento;

    /**
     * Concepto.
     */
    @Basic
    @Column(name = "PGAPCNCP", length = 2000)
    private String concepto;

    /**
     * Fecha registro.
     */
    @Basic
    @Column(name = "PGAPFCRG")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario registro.
     */
    @Basic
    @Column(name = "PGAPUSRG", length = 200)
    private String usuarioRegistro;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PGAPIDST")
    private Long estado;

    /**
     * FK - Pago de préstamo que consumió este aporte (pago con aportes / precancelación
     * con aportes). NULL en los pagos de aporte del proceso Petro.
     */
    @ManyToOne
    @JoinColumn(name = "PGPRCDGO", referencedColumnName = "PGPRCDGO")
    private PagoPrestamo pagoPrestamo;

    /**
     * Ruta del documento de respaldo digitalizado (comprobante escaneado).
     */
    @Basic
    @Column(name = "PGAPRTRS", length = 2000)
    private String rutaDocumentoRespaldo;

    /**
     * Fecha real del pago (solo devolución de aportes): copiada de
     * {@code PagoProgramado.fechaRespuesta} al reconciliar. Siempre se llena cuando el pago
     * queda CONFIRMADO en Cuentas por Pagar.
     */
    @Basic
    @Column(name = "PGAPFCPG")
    private java.time.LocalDate fechaPagoDevolucion;

    /**
     * Referencia bancaria del pago (solo devolución de aportes): copiada de
     * {@code PagoProgramado.referenciaBanco}. NULLABLE a propósito: en la confirmación manual
     * de Cuentas por Pagar la referencia es opcional, puede quedar sin dato legítimamente.
     */
    @Basic
    @Column(name = "PGAPRFPG", length = 100)
    private String referenciaPagoDevolucion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public Aporte getAporte() {
        return aporte;
    }

    public void setAporte(Aporte aporte) {
        this.aporte = aporte;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDateTime getFechaContable() {
        return fechaContable;
    }

    public void setFechaContable(LocalDateTime fechaContable) {
        this.fechaContable = fechaContable;
    }

    public Long getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(Long numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
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

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public PagoPrestamo getPagoPrestamo() {
        return pagoPrestamo;
    }

    public void setPagoPrestamo(PagoPrestamo pagoPrestamo) {
        this.pagoPrestamo = pagoPrestamo;
    }

    public String getRutaDocumentoRespaldo() {
        return rutaDocumentoRespaldo;
    }

    public void setRutaDocumentoRespaldo(String rutaDocumentoRespaldo) {
        this.rutaDocumentoRespaldo = rutaDocumentoRespaldo;
    }

    public java.time.LocalDate getFechaPagoDevolucion() {
        return fechaPagoDevolucion;
    }

    public void setFechaPagoDevolucion(java.time.LocalDate fechaPagoDevolucion) {
        this.fechaPagoDevolucion = fechaPagoDevolucion;
    }

    public String getReferenciaPagoDevolucion() {
        return referenciaPagoDevolucion;
    }

    public void setReferenciaPagoDevolucion(String referenciaPagoDevolucion) {
        this.referenciaPagoDevolucion = referenciaPagoDevolucion;
    }
}

