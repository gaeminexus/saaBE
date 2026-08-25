package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.BancoExterno;
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
 *
 * Excepción: los pagos por DÉBITO AUTOMÁTICO (PGTRDBAT=1). El banco los debita
 * por convenio, sin archivo de transferencias, así que no necesitan cuenta del
 * titular y nacen directamente en CONFIRMADO con su asiento y su movimiento
 * bancario generados en el mismo registro.
 *
 *   DÉBITO AUTOMÁTICO: registro --> 3 CONFIRMADO --reversión--> 5 ANULADO
 *
 * No se aprueban ni se seleccionan para ningún lote: el dinero ya salió de la
 * cuenta antes de que el pago llegue al sistema. Por eso tampoco admiten la
 * anulación simple (que es para pagos sin contabilidad): se reversan.
 *
 * ORIGEN EXTERNO: además de factura de compra, egreso de tesorería y anticipo a
 * proveedor, un pago puede tener su documento de origen en OTRO módulo del sistema
 * (PGTRORGN + PGTRIDOR). CXP guarda esa pareja como dato opaco y nunca la resuelve:
 * es lo que permite que otros módulos disparen órdenes de pago sin que CXP tenga
 * que conocerlos. Estos pagos llevan su desglose contable en PGS.DPGT, el asiento
 * colgado del propio PGTR (PGTRASNT) y, cuando el beneficiario no existe en el
 * maestro de titulares, sus datos denormalizados en los campos PGTRBF*.
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
     * Excluyente con {@link #egreso} y {@link #anticipo}: el pago referencia
     * una factura O un egreso de tesorería O un anticipo a proveedor, nunca
     * más de uno.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRFCTC", referencedColumnName = "ID")
    private FacturaCompra facturaCompra;

    /**
     * Egreso de tesorería sin documento físico que se está pagando.
     * FK a TSR.EGRS. Excluyente con {@link #facturaCompra} y {@link #anticipo}.
     */
    @ManyToOne
    @JoinColumn(name = "PGTREGRS", referencedColumnName = "EGRSCDGO")
    private com.saa.model.tsr.Egreso egreso;

    /**
     * Anticipo a proveedor que se está pagando. FK a PGS.ANTP.
     * Excluyente con {@link #facturaCompra} y {@link #egreso}. Al confirmarse
     * el pago se genera el asiento de anticipo (DEBE cuenta de anticipos del
     * proveedor / HABER banco), no el de egreso ni la aplicación de factura.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRANTP", referencedColumnName = "ANTPCDGO")
    private AnticipoProveedor anticipo;

    /**
     * Etiqueta del proceso EXTERNO a CXP que originó el pago, cuando el documento de
     * origen no vive en este módulo. Ver {@link com.saa.rubros.OrigenPagoExterno}.
     * Nulo en los pagos propios de CXP (factura, egreso, anticipo).
     * <p>
     * Para CXP es una cadena OPACA: se guarda y se devuelve, nunca se resuelve. Junto con
     * {@link #idOrigen} identifica el documento en el módulo que lo generó, sin que CXP
     * tenga que conocer ese módulo.
     * <p>
     * Excluyente con {@link #facturaCompra}, {@link #egreso} y {@link #anticipo}.
     */
    @Basic
    @Column(name = "PGTRORGN", length = 30)
    private String origenExterno;

    /**
     * Id del documento en el módulo que originó el pago. <b>Sin FK a propósito</b>: CXP no
     * puede depender de ningún otro módulo del sistema. CXP lo guarda y lo devuelve, pero
     * nunca lo resuelve.
     */
    @Basic
    @Column(name = "PGTRIDOR")
    private Long idOrigen;

    /**
     * Asiento contable generado al confirmarse un pago de ORIGEN EXTERNO. FK a CNT.ASNT.
     * <p>
     * Los demás orígenes cuelgan el asiento de su propio documento (la aplicación de pago,
     * el egreso o el anticipo); el de origen externo no tiene documento CXP donde colgarlo,
     * así que se guarda aquí. Nulo en los pagos propios de CXP.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Beneficiario ocasional: nombre. Se usa cuando {@link #cuentaDestino} es null, es
     * decir cuando hay que pagarle a alguien que NO está en el maestro de titulares de
     * tesorería. Es una capacidad genérica de CXP, no ligada a ningún módulo concreto.
     */
    @Basic
    @Column(name = "PGTRBFNM", length = 2000)
    private String beneficiarioNombre;

    /**
     * Beneficiario ocasional: número de identificación (cédula o RUC).
     */
    @Basic
    @Column(name = "PGTRBFID", length = 20)
    private String beneficiarioIdentificacion;

    /**
     * Beneficiario ocasional: banco externo al que se transfiere. FK a TSR.BEXT.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRBFBC", referencedColumnName = "BEXTCDGO")
    private BancoExterno beneficiarioBanco;

    /**
     * Beneficiario ocasional: tipo de cuenta (codigoAlterno del DetalleRubro del rubro de
     * tipo de cuenta bancaria).
     */
    @Basic
    @Column(name = "PGTRBFTP")
    private Long beneficiarioTipoCuenta;

    /**
     * Beneficiario ocasional: número de cuenta destino.
     */
    @Basic
    @Column(name = "PGTRBFCT", length = 50)
    private String beneficiarioCuenta;

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
     * Marca de débito automático: 0=No (transferencia normal), 1=Sí.
     * Un pago por débito automático no necesita la cuenta del titular ni pasa
     * por el archivo del banco: se contabiliza en el momento de registrarlo.
     */
    @Basic
    @Column(name = "PGTRDBAT")
    private Long debitoAutomatico;

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

    public com.saa.model.tsr.Egreso getEgreso() { return egreso; }
    public void setEgreso(com.saa.model.tsr.Egreso egreso) { this.egreso = egreso; }

    public AnticipoProveedor getAnticipo() { return anticipo; }
    public void setAnticipo(AnticipoProveedor anticipo) { this.anticipo = anticipo; }

    public String getOrigenExterno() { return origenExterno; }
    public void setOrigenExterno(String origenExterno) { this.origenExterno = origenExterno; }

    public Long getIdOrigen() { return idOrigen; }
    public void setIdOrigen(Long idOrigen) { this.idOrigen = idOrigen; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public String getBeneficiarioNombre() { return beneficiarioNombre; }
    public void setBeneficiarioNombre(String beneficiarioNombre) { this.beneficiarioNombre = beneficiarioNombre; }

    public String getBeneficiarioIdentificacion() { return beneficiarioIdentificacion; }
    public void setBeneficiarioIdentificacion(String beneficiarioIdentificacion) {
        this.beneficiarioIdentificacion = beneficiarioIdentificacion;
    }

    public BancoExterno getBeneficiarioBanco() { return beneficiarioBanco; }
    public void setBeneficiarioBanco(BancoExterno beneficiarioBanco) { this.beneficiarioBanco = beneficiarioBanco; }

    public Long getBeneficiarioTipoCuenta() { return beneficiarioTipoCuenta; }
    public void setBeneficiarioTipoCuenta(Long beneficiarioTipoCuenta) {
        this.beneficiarioTipoCuenta = beneficiarioTipoCuenta;
    }

    public String getBeneficiarioCuenta() { return beneficiarioCuenta; }
    public void setBeneficiarioCuenta(String beneficiarioCuenta) { this.beneficiarioCuenta = beneficiarioCuenta; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public CuentaBancaria getCuentaBancaria() { return cuentaBancaria; }
    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) { this.cuentaBancaria = cuentaBancaria; }

    public CuentaBancariaTitular getCuentaDestino() { return cuentaDestino; }
    public void setCuentaDestino(CuentaBancariaTitular cuentaDestino) { this.cuentaDestino = cuentaDestino; }

    public Long getDebitoAutomatico() { return debitoAutomatico; }
    public void setDebitoAutomatico(Long debitoAutomatico) { this.debitoAutomatico = debitoAutomatico; }

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
