package com.saa.model.cxc;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.RetencionCompra;
import com.saa.model.cxp.RetencionCompraV2;
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
 * Entity AplicacionPagoCxc.
 * Registra la aplicación (imputación) de un pago a una factura de venta
 * o liquidación de compra/servicio emitida por la empresa.
 * Tabla: CBR.APLC
 *
 * Tipos de documento que paga (TIPODOCPAGO), ver {@link com.saa.rubros.TipoDocPagoAplicacion}:
 *   1 = Cobro directo (efectivo / transferencia / cheque / tarjeta)
 *   2 = Nota de Crédito emitida al cliente  (FK → CBR.NTCR)
 *   3 = Retención recibida del cliente      (FK → PGS.RTCM v1 / PGS.RCV2 v2)  ← cruce de módulo
 *   4 = Anticipo del cliente                (cruce por valor contra el saldo de
 *                                            PersonaCuentaContable; la FK a
 *                                            CBR.ANTC queda nula)
 *   5 = Nota de Débito emitida al cliente   (FK → CBR.NTDB) con monto NEGATIVO,
 *                                            porque aumenta el saldo de la factura
 *
 * Formas de pago directo (FORMAPAGO, solo aplica cuando TIPODOCPAGO = 1):
 *   1 = Efectivo
 *   2 = Transferencia
 *   3 = Cheque
 *   4 = Tarjeta
 *
 * Estados (ESTADO):
 *   1 = Activo
 *   2 = Reversado
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "APLC", schema = "CBR")
@SequenceGenerator(name = "SQ_APLCCDGO", sequenceName = "CBR.SQ_APLCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AplicacionPagoCxcAll", query = "select e from AplicacionPagoCxc e"),
    @NamedQuery(name = "AplicacionPagoCxcId",  query = "select e from AplicacionPagoCxc e where e.id = :id"),
    @NamedQuery(name = "AplicacionPagoCxcByFactura",
        query = "select e from AplicacionPagoCxc e where e.factura.id = :facturaId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxcByLiquidacion",
        query = "select e from AplicacionPagoCxc e where e.liquidacion.id = :liquidacionId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxcByRetencion",
        query = "select e from AplicacionPagoCxc e where e.retencion.id = :documentoId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxcByRetencionV2",
        query = "select e from AplicacionPagoCxc e where e.retencionV2.id = :documentoId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxcByNotaCredito",
        query = "select e from AplicacionPagoCxc e where e.notaCredito.id = :documentoId and e.estado = 1"),
    @NamedQuery(name = "AplicacionPagoCxcByNotaDebito",
        query = "select e from AplicacionPagoCxc e where e.notaDebito.id = :documentoId and e.estado = 1")
})
public class AplicacionPagoCxc implements Serializable {

    /**
     * Identificador único de la aplicación.
     */
    @Basic
    @Id
    @Column(name = "APLCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_APLCCDGO")
    private Long id;

    /**
     * Empresa a la que pertenece la aplicación. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "APLCPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    // ── Documento que recibe el pago (XOR: uno solo debe tener valor) ────────

    /**
     * Factura de venta que recibe el pago. FK a CBR.FCTR.
     * Nulo si se aplica a una liquidación.
     */
    @ManyToOne
    @JoinColumn(name = "APLCFCTR", referencedColumnName = "ID")
    private Factura factura;

    /**
     * Liquidación de compra/servicio que recibe el pago. FK a CBR.LQCS.
     * Nulo si se aplica a una factura.
     */
    @ManyToOne
    @JoinColumn(name = "APLCLQCS", referencedColumnName = "ID")
    private LiquidacionCompra liquidacion;

    // ── Tipo de documento que paga ───────────────────────────────────────────

    /**
     * Tipo de documento con el que se realiza el pago.
     * 1=Cobro directo, 2=Nota Crédito, 3=Retención cliente, 4=Anticipo
     */
    @Basic
    @Column(name = "APLCTDPG")
    private Long tipoDocPago;

    // ── FK al documento que paga (solo uno activo según tipoDocPago) ─────────

    /**
     * Nota de Crédito emitida al cliente. FK a CBR.NTCR.
     * Aplica cuando APLCTDPG = 2.
     */
    @ManyToOne
    @JoinColumn(name = "APLCNTCR", referencedColumnName = "ID")
    private NotaCredito notaCredito;

    /**
     * Retención recibida del cliente. FK a PGS.RTCM.
     * Aplica cuando APLCTDPG = 3.
     */
    @ManyToOne
    @JoinColumn(name = "APLCRTCM", referencedColumnName = "ID")
    private RetencionCompra retencion;

    /**
     * Retención V2 recibida del cliente. FK a PGS.RCV2.
     * Aplica cuando APLCTDPG = 3 y la retención es de versión 2.
     */
    @ManyToOne
    @JoinColumn(name = "APLCRCV2", referencedColumnName = "ID")
    private RetencionCompraV2 retencionV2;

    /**
     * Nota de Débito emitida al cliente. FK a CBR.NTDB.
     * Aplica cuando APLCTDPG = 5. El monto aplicado se guarda NEGATIVO porque
     * la nota de débito aumenta el saldo pendiente de la factura.
     */
    @ManyToOne
    @JoinColumn(name = "APLCNTDB", referencedColumnName = "ID")
    private NotaDebito notaDebito;

    /**
     * Anticipo del cliente. FK a CBR.ANTC.
     * Queda nulo en el cruce por valor contra el saldo de anticipos
     * (PersonaCuentaContable), que es el mecanismo estándar.
     */
    @ManyToOne
    @JoinColumn(name = "APLCANTC", referencedColumnName = "ID")
    private AnticipoCliente anticipo;

    // ── Datos del cobro directo (solo cuando APLCTDPG = 1) ────────────────

    /**
     * Forma de pago del cobro directo.
     * 1=Efectivo, 2=Transferencia, 3=Cheque, 4=Tarjeta
     */
    @Basic
    @Column(name = "APLCFPAG")
    private Long formaPago;

    /**
     * Número de referencia del cobro.
     */
    @Basic
    @Column(name = "APLCREFR", length = 200)
    private String referencia;

    /**
     * Banco de origen del cobro.
     */
    @Basic
    @Column(name = "APLCBANC", length = 200)
    private String banco;

    // ── Datos comunes ────────────────────────────────────────────────────────

    /**
     * Monto aplicado a la factura o liquidación.
     */
    @Basic
    @Column(name = "APLCMAPL")
    private Double montoAplicado;

    /**
     * Fecha en que se realizó la aplicación del pago.
     */
    @Basic
    @Column(name = "APLCFAPL")
    private LocalDate fechaAplicacion;

    /**
     * Observaciones de la aplicación.
     */
    @Basic
    @Column(name = "APLCOBSR", length = 2000)
    private String observacion;

    /**
     * Estado de la aplicación.
     * 1=Activo, 2=Reversado
     */
    @Basic
    @Column(name = "APLCESTD")
    private Long estado;

    /**
     * Usuario que registra la aplicación. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "APLCUSAR", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Asiento contable generado por la aplicación. FK a CNT.ASNT.
     */
    @ManyToOne
    @JoinColumn(name = "APLCASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "APLCFCRG")
    private LocalDateTime fechaRegistro;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Factura getFactura() { return factura; }
    public void setFactura(Factura factura) { this.factura = factura; }

    public LiquidacionCompra getLiquidacion() { return liquidacion; }
    public void setLiquidacion(LiquidacionCompra liquidacion) { this.liquidacion = liquidacion; }

    public Long getTipoDocPago() { return tipoDocPago; }
    public void setTipoDocPago(Long tipoDocPago) { this.tipoDocPago = tipoDocPago; }

    public NotaCredito getNotaCredito() { return notaCredito; }
    public void setNotaCredito(NotaCredito notaCredito) { this.notaCredito = notaCredito; }

    public RetencionCompraV2 getRetencionV2() { return retencionV2; }
    public void setRetencionV2(RetencionCompraV2 retencionV2) { this.retencionV2 = retencionV2; }

    public NotaDebito getNotaDebito() { return notaDebito; }
    public void setNotaDebito(NotaDebito notaDebito) { this.notaDebito = notaDebito; }

    public RetencionCompra getRetencion() { return retencion; }
    public void setRetencion(RetencionCompra retencion) { this.retencion = retencion; }

    public AnticipoCliente getAnticipo() { return anticipo; }
    public void setAnticipo(AnticipoCliente anticipo) { this.anticipo = anticipo; }

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