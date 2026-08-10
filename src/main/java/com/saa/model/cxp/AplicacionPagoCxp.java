package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.Retencion;
import com.saa.model.cxc.RetencionV2;
import com.saa.model.scp.Empresa;
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
 * Entity AplicacionPagoCxp.
 * Registra la aplicación (imputación) de un pago a una factura de compra
 * recibida de un proveedor.
 * Tabla: PGS.APLP
 *
 * Tipos de documento que paga (TIPODOCPAGO), ver {@link com.saa.rubros.TipoDocPagoAplicacion}:
 *   1 = Pago directo (efectivo / transferencia / cheque)
 *   2 = Nota de Crédito recibida del proveedor  (FK → PGS.NTCC)
 *   3 = Retención emitida al proveedor           (FK → CBR.RTNC v1 / CBR.RTV2 v2)  ← cruce de módulo
 *   4 = Anticipo al proveedor                    (cruce por valor contra el saldo
 *                                                 de PersonaCuentaContable; la FK
 *                                                 a PGS.ANTP queda nula)
 *   5 = Nota de Débito recibida del proveedor    (FK → PGS.NTDC) con monto NEGATIVO,
 *                                                 porque aumenta el saldo de la factura
 *
 * Formas de pago directo (FORMAPAGO, solo aplica cuando TIPODOCPAGO = 1):
 *   1 = Efectivo
 *   2 = Transferencia
 *   3 = Cheque
 *
 * Estados (ESTADO):
 *   1 = Activo
 *   2 = Reversado
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "APLP", schema = "PGS")
@SequenceGenerator(name = "SQ_APLPCDGO", sequenceName = "PGS.SQ_APLPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AplicacionPagoCxpAll", query = "select e from AplicacionPagoCxp e"),
    @NamedQuery(name = "AplicacionPagoCxpId",  query = "select e from AplicacionPagoCxp e where e.id = :id"),
    @NamedQuery(name = "AplicacionPagoCxpByFactura",
        query = "select e from AplicacionPagoCxp e where e.facturaCompra.id = :facturaId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxpByRetencion",
        query = "select e from AplicacionPagoCxp e where e.retencion.id = :documentoId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxpByRetencionV2",
        query = "select e from AplicacionPagoCxp e where e.retencionV2.id = :documentoId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxpByNotaCredito",
        query = "select e from AplicacionPagoCxp e where e.notaCredito.id = :documentoId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxpByNotaDebito",
        query = "select e from AplicacionPagoCxp e where e.notaDebito.id = :documentoId and e.estado = 1")
})
public class AplicacionPagoCxp implements Serializable {

    /**
     * Identificador único de la aplicación.
     */
    @Basic
    @Id
    @Column(name = "APLPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_APLPCDGO")
    private Long id;

    /**
     * Empresa a la que pertenece la aplicación. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "APLPPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Factura de compra que recibe el pago. FK a PGS.FCTC.
     */
    @ManyToOne
    @JoinColumn(name = "APLPFCTC", referencedColumnName = "ID")
    private FacturaCompra facturaCompra;

    // ── Tipo de documento que paga ───────────────────────────────────────────

    /**
     * Tipo de documento con el que se realiza el pago.
     * 1=Pago directo, 2=Nota Crédito proveedor, 3=Retención emitida, 4=Anticipo
     */
    @Basic
    @Column(name = "APLPTDPG")
    private Long tipoDocPago;

    // ── FK al documento que paga (solo uno activo según tipoDocPago) ─────────

    /**
     * Nota de Crédito recibida del proveedor. FK a PGS.NTCC.
     * Aplica cuando APLPTDPG = 2.
     */
    @ManyToOne
    @JoinColumn(name = "APLPNTCC", referencedColumnName = "ID")
    private NotaCreditoCompra notaCredito;

    /**
     * Retención emitida al proveedor. FK a CBR.RTNC.
     * Aplica cuando APLPTDPG = 3.
     */
    @ManyToOne
    @JoinColumn(name = "APLPRTNC", referencedColumnName = "ID")
    private Retencion retencion;

    /**
     * Retención V2 emitida al proveedor. FK a CBR.RTV2.
     * Aplica cuando APLPTDPG = 3 y la retención es de versión 2.
     */
    @ManyToOne
    @JoinColumn(name = "APLPRTV2", referencedColumnName = "ID")
    private RetencionV2 retencionV2;

    /**
     * Nota de Débito recibida del proveedor. FK a PGS.NTDC.
     * Aplica cuando APLPTDPG = 5. El monto aplicado se guarda NEGATIVO porque
     * la nota de débito aumenta el saldo pendiente de la factura.
     */
    @ManyToOne
    @JoinColumn(name = "APLPNTDC", referencedColumnName = "ID")
    private NotaDebitoCompra notaDebito;

    /**
     * Anticipo entregado al proveedor. FK a PGS.ANTP.
     * Queda nulo en el cruce por valor contra el saldo de anticipos
     * (PersonaCuentaContable), que es el mecanismo estándar.
     */
    @ManyToOne
    @JoinColumn(name = "APLPANTP", referencedColumnName = "ANTPCDGO")
    private AnticipoProveedor anticipo;

    // ── Datos del pago directo (solo cuando APLPTDPG = 1) ─────────────────

    /**
     * Forma de pago del pago directo.
     * 1=Efectivo, 2=Transferencia, 3=Cheque
     */
    @Basic
    @Column(name = "APLPFPAG")
    private Long formaPago;

    /**
     * Número de referencia del pago.
     */
    @Basic
    @Column(name = "APLPREFR", length = 200)
    private String referencia;

    /**
     * Banco de destino del pago.
     */
    @Basic
    @Column(name = "APLPBANC", length = 200)
    private String banco;

    // ── Datos comunes ────────────────────────────────────────────────────────

    /**
     * Monto aplicado a la factura de compra.
     */
    @Basic
    @Column(name = "APLPMAPL")
    private Double montoAplicado;

    /**
     * Fecha en que se realizó la aplicación del pago.
     */
    @Basic
    @Column(name = "APLPFAPL")
    private LocalDate fechaAplicacion;

    /**
     * Observaciones de la aplicación.
     */
    @Basic
    @Column(name = "APLPOBSR", length = 2000)
    private String observacion;

    /**
     * Estado de la aplicación.
     * 1=Activo, 2=Reversado
     */
    @Basic
    @Column(name = "APLPESTD")
    private Long estado;

    /**
     * Usuario que registra la aplicación. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "APLPUSAR", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Asiento contable generado por la aplicación. FK a CNT.ASNT.
     */
    @ManyToOne
    @JoinColumn(name = "APLPASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "APLPFCRG")
    private LocalDateTime fechaRegistro;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public FacturaCompra getFacturaCompra() { return facturaCompra; }
    public void setFacturaCompra(FacturaCompra facturaCompra) { this.facturaCompra = facturaCompra; }

    public Long getTipoDocPago() { return tipoDocPago; }
    public void setTipoDocPago(Long tipoDocPago) { this.tipoDocPago = tipoDocPago; }

    public NotaCreditoCompra getNotaCredito() { return notaCredito; }
    public void setNotaCredito(NotaCreditoCompra notaCredito) { this.notaCredito = notaCredito; }

    public Retencion getRetencion() { return retencion; }
    public void setRetencion(Retencion retencion) { this.retencion = retencion; }

    public RetencionV2 getRetencionV2() { return retencionV2; }
    public void setRetencionV2(RetencionV2 retencionV2) { this.retencionV2 = retencionV2; }

    public NotaDebitoCompra getNotaDebito() { return notaDebito; }
    public void setNotaDebito(NotaDebitoCompra notaDebito) { this.notaDebito = notaDebito; }

    public AnticipoProveedor getAnticipo() { return anticipo; }
    public void setAnticipo(AnticipoProveedor anticipo) { this.anticipo = anticipo; }

    public Long getFormaPago() { return formaPago; }
    public void setFormaPago(Long formaPago) { this.formaPago = formaPago; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public Double getMontoAplicado() { return montoAplicado; }
    public void setMontoAplicado(Double montoAplicado) { this.montoAplicado = montoAplicado; }

    public LocalDate getFechaAplicacion() { return fechaAplicacion; }
    public void setFechaAplicacion(LocalDate fechaAplicacion) { this.fechaAplicacion = fechaAplicacion; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}