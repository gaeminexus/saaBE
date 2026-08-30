package com.saa.model.cxc;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;

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
 * Entity AnticipoCliente.
 * Registra los anticipos recibidos de clientes.
 * Tabla: CBR.ANTC
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ANTC", schema = "CBR")
@SequenceGenerator(name = "SQ_ANTCCDGO", sequenceName = "CBR.SQ_ANTCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AnticipoClienteAll", query = "select e from AnticipoCliente e"),
    @NamedQuery(name = "AnticipoClienteId",  query = "select e from AnticipoCliente e where e.id = :id")
})
public class AnticipoCliente implements Serializable {

    /**
     * Identificador único del anticipo.
     */
    @Basic
    @Id
    @Column(name = "ID", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ANTCCDGO")
    private Long id;

    /**
     * Cliente que entrega el anticipo. FK a TSR.TTLR.
     */
    @ManyToOne
    @JoinColumn(name = "TITULAR", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /**
     * Fecha del documento de anticipo.
     */
    @Basic
    @Column(name = "FECHAANTICIPO")
    private LocalDate fechaAnticipo;

    /**
     * Fecha en que se recibió físicamente el anticipo.
     */
    @Basic
    @Column(name = "FECHARECEPCION")
    private LocalDate fechaRecepcion;

    /**
     * Usuario que registra el anticipo. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "FECHAREGISTRO")
    private LocalDateTime fechaRegistro;

    /**
     * Número de documento de referencia del anticipo.
     */
    @Basic
    @Column(name = "NUMERODOC", length = 100)
    private String numeroDoc;

    /**
     * Valor monetario del anticipo.
     */
    @Basic
    @Column(name = "VALOR")
    private Double valor;

    /**
     * Saldo disponible del anticipo (valor - lo ya aplicado a facturas).
     * 1=Activo, 2=Agotado, 3=Anulado
     */
    @Basic
    @Column(name = "ANTCSALD")
    private Double saldo;

    /**
     * Forma de pago con que se recibió el anticipo.
     * 1=Efectivo, 2=Transferencia, 3=Cheque, 4=Tarjeta
     */
    @Basic
    @Column(name = "ANTCFPAG")
    private Long formaPago;

    /**
     * Número de referencia del pago (nro. transferencia, cheque, etc.).
     */
    @Basic
    @Column(name = "ANTCREFR", length = 200)
    private String referencia;

    /**
     * Banco de origen del pago.
     */
    @Basic
    @Column(name = "ANTCBANC", length = 200)
    private String banco;

    /**
     * Asiento contable generado. FK a CNT.ASNT.
     */
    @ManyToOne
    @JoinColumn(name = "ASIENTO", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Estado: 1=Activo, 2=Anulado.
     */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    /**
     * Empresa contable a la que pertenece el anticipo. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Observaciones adicionales.
     */
    @Basic
    @Column(name = "OBSERVACION", length = 2000)
    private String observacion;

    /**
     * FK a PGS.PGTR.PGTRCDGO: el pago de devolución de saldo (origen externo
     * CXC_DEVOLUCION_CLIENTE) vigente/más reciente asociado a este anticipo.
     * <p>
     * <b>Solo trackea LA ÚLTIMA devolución, sin historial</b> (opción A del análisis de
     * idempotencia, 2026-08-28 — ver docs/logica-negocio/cxc/sql/add-anticipo-cliente-devolucion.sql).
     * Si se necesita auditoría de devoluciones pasadas, hace falta una tabla propia
     * (CBR.DVCL, opción B, no elegida).
     */
    @Basic
    @Column(name = "ANTCIDPG")
    private Long idPagoDevolucion;

    /**
     * 0/1: si el descuento de {@link #saldo} por el pago {@link #idPagoDevolucion} ya se
     * aplicó. Lo pone en 1 el reconciliador ({@code AnticipoClienteServiceImpl.sincronizarDevolucion})
     * al ver el pago en estado CONFIRMADO — es el guardián de idempotencia: una segunda
     * corrida sobre el mismo pago ya CONFIRMADO ve este campo en 1 y no descuenta de nuevo.
     */
    @Basic
    @Column(name = "ANTCAPLC")
    private Long aplicado;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public LocalDate getFechaAnticipo() { return fechaAnticipo; }
    public void setFechaAnticipo(LocalDate fechaAnticipo) { this.fechaAnticipo = fechaAnticipo; }

    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDate fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getNumeroDoc() { return numeroDoc; }
    public void setNumeroDoc(String numeroDoc) { this.numeroDoc = numeroDoc; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    public Long getFormaPago() { return formaPago; }
    public void setFormaPago(Long formaPago) { this.formaPago = formaPago; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Long getIdPagoDevolucion() { return idPagoDevolucion; }
    public void setIdPagoDevolucion(Long idPagoDevolucion) { this.idPagoDevolucion = idPagoDevolucion; }

    public Long getAplicado() { return aplicado; }
    public void setAplicado(Long aplicado) { this.aplicado = aplicado; }
}