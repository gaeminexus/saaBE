package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.CuentaBancariaTitular;
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
 * Entity PagoProgramado.
 * Pago a un proveedor por transferencia bancaria, sobre una factura de compra.
 * Tabla: PGS.PGTR
 *
 * Ciclo de vida (ESTADO), ver {@link com.saa.rubros.EstadoPagoProgramado}:
 *
 *   1 REGISTRADO --selección--> 2 EN_ARCHIVO --respuesta banco--> 3 CONFIRMADO
 *        |                           |                                |
 *        |                           +--rechazado--> 4 RECHAZADO      +--> recién aquí:
 *        +--> 5 ANULADO (motivo)          (seguimiento)                    AplicacionPagoCxp
 *                                                                          + asiento
 *                                                                          + MovimientoBanco
 *
 * Mientras el pago no esté CONFIRMADO no se registra nada en contabilidad ni en
 * TSR.MVCB: un pago que el banco no ejecutó no debe afectar el saldo bancario
 * ni la conciliación.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PGTR", schema = "PGS")
@SequenceGenerator(name = "SQ_PGTRCDGO", sequenceName = "PGS.SQ_PGTRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "PagoProgramadoAll", query = "select e from PagoProgramado e"),
    @NamedQuery(name = "PagoProgramadoId",  query = "select e from PagoProgramado e where e.id = :id"),
    @NamedQuery(name = "PagoProgramadoByFactura",
        query = "select e from PagoProgramado e where e.facturaCompra.id = :facturaId"),
    @NamedQuery(name = "PagoProgramadoByLote",
        query = "select e from PagoProgramado e where e.lote.id = :idLote order by e.id"),
    @NamedQuery(name = "PagoProgramadoByEstado",
        query = "select e from PagoProgramado e where e.empresa.codigo = :idEmpresa "
              + "and e.estado = :estado order by e.id")
})
public class PagoProgramado implements Serializable {

    /**
     * Identificador único del pago programado.
     */
    @Basic
    @Id
    @Column(name = "PGTRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PGTRCDGO")
    private Long id;

    /**
     * Empresa a la que pertenece el pago. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Factura de compra que se está pagando. FK a PGS.FCTC.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRFCTC", referencedColumnName = "ID")
    private FacturaCompra facturaCompra;

    /**
     * Proveedor al que se paga. FK a TSR.TTLR.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRTTLR", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /**
     * Cuenta bancaria propia desde la que sale el dinero. FK a TSR.CNBC.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRCNBC", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /**
     * Cuenta bancaria del proveedor a la que se transfiere. FK a TSR.CTBN.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRCTBN", referencedColumnName = "CTBNCDGO")
    private CuentaBancariaTitular cuentaDestino;

    /**
     * Valor a transferir.
     */
    @Basic
    @Column(name = "PGTRVLOR")
    private Double valor;

    /**
     * Fecha programada para el pago.
     */
    @Basic
    @Column(name = "PGTRFPRG")
    private LocalDate fechaProgramada;

    /**
     * Lote/archivo en el que se envió al banco. FK a PGS.LTPG.
     * Nulo mientras el pago está en estado REGISTRADO.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRLTPG", referencedColumnName = "LTPGCDGO")
    private LotePago lote;

    /**
     * Estado del pago.
     * 1=Registrado, 2=En archivo, 3=Confirmado, 4=Rechazado, 5=Anulado
     */
    @Basic
    @Column(name = "PGTRESTD")
    private Long estado;

    /**
     * Número de transferencia o referencia que devuelve el banco al confirmar.
     */
    @Basic
    @Column(name = "PGTRRFBN", length = 200)
    private String referenciaBanco;

    /**
     * Fecha de la respuesta del banco.
     */
    @Basic
    @Column(name = "PGTRFRSP")
    private LocalDate fechaRespuesta;

    /**
     * Motivo del rechazo del banco o de la anulación del usuario.
     */
    @Basic
    @Column(name = "PGTRMTVO", length = 2000)
    private String motivo;

    /**
     * Aplicación de pago generada cuando el banco confirma la transferencia.
     * FK a PGS.APLP.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRAPLP", referencedColumnName = "APLPCDGO")
    private AplicacionPagoCxp aplicacion;

    /**
     * Observaciones del pago.
     */
    @Basic
    @Column(name = "PGTROBSR", length = 2000)
    private String observacion;

    /**
     * Usuario que registra el pago. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRUSAR", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "PGTRFCRG")
    private LocalDateTime fechaRegistro;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public FacturaCompra getFacturaCompra() { return facturaCompra; }
    public void setFacturaCompra(FacturaCompra facturaCompra) { this.facturaCompra = facturaCompra; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public CuentaBancaria getCuentaBancaria() { return cuentaBancaria; }
    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) { this.cuentaBancaria = cuentaBancaria; }

    public CuentaBancariaTitular getCuentaDestino() { return cuentaDestino; }
    public void setCuentaDestino(CuentaBancariaTitular cuentaDestino) { this.cuentaDestino = cuentaDestino; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getFechaProgramada() { return fechaProgramada; }
    public void setFechaProgramada(LocalDate fechaProgramada) { this.fechaProgramada = fechaProgramada; }

    public LotePago getLote() { return lote; }
    public void setLote(LotePago lote) { this.lote = lote; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getReferenciaBanco() { return referenciaBanco; }
    public void setReferenciaBanco(String referenciaBanco) { this.referenciaBanco = referenciaBanco; }

    public LocalDate getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDate fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public AplicacionPagoCxp getAplicacion() { return aplicacion; }
    public void setAplicacion(AplicacionPagoCxp aplicacion) { this.aplicacion = aplicacion; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
