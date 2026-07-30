package com.saa.model.cxp;

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
 * Entity AnticipoProveedor.
 * Registra los anticipos entregados a proveedores.
 * Tabla: PGS.ANTP
 *
 * Estados:
 *   1 = Activo (tiene saldo disponible)
 *   2 = Agotado (saldo = 0)
 *   3 = Anulado
 *
 * Formas de pago:
 *   1 = Efectivo
 *   2 = Transferencia
 *   3 = Cheque
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ANTP", schema = "PGS")
@SequenceGenerator(name = "SQ_ANTPCDGO", sequenceName = "PGS.SQ_ANTPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AnticipoProveedorAll", query = "select e from AnticipoProveedor e"),
    @NamedQuery(name = "AnticipoProveedorId",  query = "select e from AnticipoProveedor e where e.id = :id")
})
public class AnticipoProveedor implements Serializable {

    /**
     * Identificador único del anticipo.
     */
    @Basic
    @Id
    @Column(name = "ANTPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ANTPCDGO")
    private Long id;

    /**
     * Proveedor al que se entrega el anticipo. FK a TSR.TTLR.
     */
    @ManyToOne
    @JoinColumn(name = "ANTPTTLR", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /**
     * Empresa contable a la que pertenece el anticipo. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "ANTPPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Fecha del documento de anticipo.
     */
    @Basic
    @Column(name = "ANTPFANT")
    private LocalDate fechaAnticipo;

    /**
     * Fecha en que se realizó físicamente el pago del anticipo.
     */
    @Basic
    @Column(name = "ANTPFRCP")
    private LocalDate fechaRecepcion;

    /**
     * Número de documento de referencia del anticipo.
     */
    @Basic
    @Column(name = "ANTPNDOC", length = 100)
    private String numeroDoc;

    /**
     * Valor monetario total del anticipo.
     */
    @Basic
    @Column(name = "ANTPVLOR")
    private Double valor;

    /**
     * Saldo disponible del anticipo (valor - lo ya aplicado a facturas).
     */
    @Basic
    @Column(name = "ANTPSALD")
    private Double saldo;

    /**
     * Forma de pago con que se realizó el anticipo.
     * 1=Efectivo, 2=Transferencia, 3=Cheque
     */
    @Basic
    @Column(name = "ANTPFPAG")
    private Long formaPago;

    /**
     * Número de referencia del pago (nro. transferencia, cheque, etc.).
     */
    @Basic
    @Column(name = "ANTPREFR", length = 200)
    private String referencia;

    /**
     * Banco de destino del pago.
     */
    @Basic
    @Column(name = "ANTPBANC", length = 200)
    private String banco;

    /**
     * Observaciones adicionales.
     */
    @Basic
    @Column(name = "ANTPOBSR", length = 2000)
    private String observacion;

    /**
     * Estado del anticipo.
     * 1=Activo, 2=Agotado, 3=Anulado
     */
    @Basic
    @Column(name = "ANTPESTD")
    private Long estado;

    /**
     * Usuario que registra el anticipo. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "ANTPUSAR", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Asiento contable generado. FK a CNT.ASNT.
     */
    @ManyToOne
    @JoinColumn(name = "ANTPASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "ANTPFCRG")
    private LocalDateTime fechaRegistro;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public LocalDate getFechaAnticipo() { return fechaAnticipo; }
    public void setFechaAnticipo(LocalDate fechaAnticipo) { this.fechaAnticipo = fechaAnticipo; }

    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDate fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

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