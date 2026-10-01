package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;
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
import jakarta.persistence.Table;

/**
 * Entity DevolucionAnticipoProveedor.
 * Cabecera de la devolución del saldo de anticipos de un proveedor: un depósito del proveedor
 * en una cuenta bancaria propia. Tabla: PGS.DVPR (docs/logica-negocio/cxp/sql/e2-78).
 *
 * El saldo que se devuelve es el de uno o más {@link AnticipoProveedor} — ver
 * {@link DetalleDevolucionAnticipo}. Asiento DEBE banco / HABER anticipos del proveedor,
 * espejo del asiento con que nació el anticipo.
 *
 * Estados, ver {@link com.saa.rubros.EstadoDevolucionAnticipoProveedor}: 1 = ACTIVA, 2 = ANULADA.
 *
 * Sin FK hacia TSR, CNT ni SCP a propósito (una FK entre esquemas exige GRANT REFERENCES, ver el
 * script e2-78).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DVPR", schema = "PGS")
@NamedQueries({
    @NamedQuery(name = "DevolucionAnticipoProveedorAll", query = "select e from DevolucionAnticipoProveedor e"),
    @NamedQuery(name = "DevolucionAnticipoProveedorId",  query = "select e from DevolucionAnticipoProveedor e where e.codigo = :id")
})
public class DevolucionAnticipoProveedor implements Serializable {

    @Id
    @Column(name = "DVPRCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Empresa contable del depósito. FK a SCP.PJRQ. */
    @ManyToOne
    @JoinColumn(name = "DVPRPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /** Proveedor que deposita. FK a TSR.TTLR. */
    @ManyToOne
    @JoinColumn(name = "DVPRTTLR", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /** Cuenta bancaria propia donde entró el depósito. FK a TSR.CNBC. */
    @ManyToOne
    @JoinColumn(name = "DVPRCNBC", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /** Fecha del depósito. */
    @Basic
    @Column(name = "DVPRFCHA")
    private LocalDate fecha;

    /** Total del depósito (suma de los DetalleDevolucionAnticipo). */
    @Basic
    @Column(name = "DVPRVLOR")
    private Double valor;

    /** Referencia del depósito (número de transferencia, comprobante bancario, etc.). */
    @Basic
    @Column(name = "DVPRREFR", length = 200)
    private String referencia;

    /** Observación libre. */
    @Basic
    @Column(name = "DVPROBSR", length = 2000)
    private String observacion;

    /** Asiento contable de la devolución. FK a CNT.ASNT. */
    @ManyToOne
    @JoinColumn(name = "DVPRASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /** Estado: 1 ACTIVA, 2 ANULADA. */
    @Basic
    @Column(name = "DVPRESTD")
    private Long estado;

    /** Motivo de la anulación. */
    @Basic
    @Column(name = "DVPRMTAN", length = 1000)
    private String motivoAnulacion;

    /** Fecha y hora de la anulación. */
    @Basic
    @Column(name = "DVPRFCAN")
    private LocalDateTime fechaAnulacion;

    /** Usuario que registra la devolución. FK a SCP.PJRQ. */
    @ManyToOne
    @JoinColumn(name = "DVPRUSAR", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /** Fecha y hora de registro en el sistema. */
    @Basic
    @Column(name = "DVPRFCRG")
    private LocalDateTime fechaRegistro;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public CuentaBancaria getCuentaBancaria() { return cuentaBancaria; }
    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) { this.cuentaBancaria = cuentaBancaria; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
