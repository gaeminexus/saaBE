package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.scp.Usuario;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Entity PagoNegociacion.
 * Registra los pagos (anticipos o pagos contra factura) realizados sobre
 * una cuota específica de una negociacion con proveedor.
 * Permite conocer cuánto se ha pagado de cada cuota, si fue con factura o como anticipo,
 * y si la factura asociada ya fue cancelada.
 * Tabla: PGS.PGNG
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PGNG", schema = "PGS")
@SequenceGenerator(name = "SQ_PGNGCDGO", sequenceName = "PGS.SQ_PGNGCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "PagoNegociacionAll", query = "select e from PagoNegociacion e"),
    @NamedQuery(name = "PagoNegociacionId", query = "select e from PagoNegociacion e where e.id = :id")
})
public class PagoNegociacion implements Serializable {

    /** Id de tabla. */
    @Basic
    @Id
    @Column(name = "ID", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PGNGCDGO")
    private Long id;

    /** Cuota de la negociacion a la que corresponde este pago. */
    @ManyToOne
    @JoinColumn(name = "FORMAPAGO", referencedColumnName = "ID")
    private FormaPagoNegociacion formaPago;

    /** Fecha en la que se realizó o registró el pago. */
    @Basic
    @Column(name = "FECHAPAGO")
    private LocalDate fechaPago;

    /** Valor pagado en este registro. */
    @Basic
    @Column(name = "VALORPAGO")
    private Double valorPago;

    /** Descripcion u observacion del pago realizado. */
    @Basic
    @Column(name = "DESCRIPCION", length = 1000)
    private String descripcion;

    /**
     * Tipo de pago.
     * ANTICIPO: se entrego dinero anticipado sin factura aún.
     * FACTURA: el pago corresponde a una factura emitida por el proveedor.
     */
    @Basic
    @Column(name = "TIPOPAGO", length = 50)
    private String tipoPago;

    /**
     * Factura de compra asociada a este pago (opcional).
     * Se relaciona si el proveedor ya emitió la factura por este valor.
     */
    @ManyToOne
    @JoinColumn(name = "FACTURACOMPRA", referencedColumnName = "ID")
    private FacturaCompra facturaCompra;

    /**
     * Indica si el valor fue facturado por el proveedor.
     * 1 = Facturado, 0 = Sin factura (anticipo puro).
     */
    @Basic
    @Column(name = "FACTURADO")
    private Long facturado;

    /**
     * Indica si el pago se considera cancelado/liquidado.
     * 1 = Pagado, 0 = Pendiente.
     * Si hay factura asociada, se marca como pagado cuando la factura esté cancelada.
     */
    @Basic
    @Column(name = "PAGADO")
    private Long pagado;

    /** Referencia del comprobante de pago (número de transferencia, cheque, etc.). */
    @Basic
    @Column(name = "REFCOMPROBANTE", length = 200)
    private String refComprobante;

    /**
     * Estado del registro.
     * 1 = Activo, 0 = Anulado.
     */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    /** Usuario que registra el pago. */
    @ManyToOne
    @JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /** Fecha y hora en la que se registra el pago en el sistema. */
    @Basic
    @Column(name = "FECHAREGISTRO")
    private LocalDateTime fechaRegistro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FormaPagoNegociacion getFormaPago() { return formaPago; }
    public void setFormaPago(FormaPagoNegociacion formaPago) { this.formaPago = formaPago; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public Double getValorPago() { return valorPago; }
    public void setValorPago(Double valorPago) { this.valorPago = valorPago; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipoPago() { return tipoPago; }
    public void setTipoPago(String tipoPago) { this.tipoPago = tipoPago; }

    public FacturaCompra getFacturaCompra() { return facturaCompra; }
    public void setFacturaCompra(FacturaCompra facturaCompra) { this.facturaCompra = facturaCompra; }

    public Long getFacturado() { return facturado; }
    public void setFacturado(Long facturado) { this.facturado = facturado; }

    public Long getPagado() { return pagado; }
    public void setPagado(Long pagado) { this.pagado = pagado; }

    public String getRefComprobante() { return refComprobante; }
    public void setRefComprobante(String refComprobante) { this.refComprobante = refComprobante; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
