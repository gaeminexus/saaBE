package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.ProductoCobro;
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
 * Entity Ingreso.
 * Ingreso de tesorería sin respaldo de un documento físico: intereses
 * ganados, créditos bancarios, devoluciones, etc.
 * Tabla: TSR.INGR
 *
 * La cuenta contable del ingreso NO se configura aquí: sale del grupo del
 * producto CXC referenciado (ProductoCobro.grupoProducto.planCuenta).
 *
 * A diferencia de los egresos, el ingreso se registra cuando el dinero YA
 * entró a la cuenta bancaria: en el mismo paso se genera el asiento
 * (DEBE cuenta del banco / HABER cuenta del grupo del producto) y el
 * movimiento bancario para conciliación.
 *
 * Estados (ver {@link com.saa.rubros.EstadoIngresoTesoreria}):
 *   1 = Activo (contabilizado)
 *   2 = Anulado
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "INGR", schema = "TSR")
@SequenceGenerator(name = "SQ_INGRCDGO", sequenceName = "TSR.SQ_INGRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "IngresoAll", query = "select e from Ingreso e"),
    @NamedQuery(name = "IngresoId",  query = "select e from Ingreso e where e.id = :id")
})
public class Ingreso implements Serializable {

    /**
     * Identificador único del ingreso.
     */
    @Basic
    @Id
    @Column(name = "INGRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_INGRCDGO")
    private Long id;

    /**
     * Empresa a la que pertenece el ingreso. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "INGRPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Titular que origina el ingreso. FK a TSR.TTLR. Opcional.
     */
    @ManyToOne
    @JoinColumn(name = "INGRTTLR", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /**
     * Producto CXC que clasifica el ingreso. FK a CBR.PRDC.
     * La cuenta contable sale del grupo del producto (GRPC.planCuenta).
     */
    @ManyToOne
    @JoinColumn(name = "INGRPRDC", referencedColumnName = "ID")
    private ProductoCobro producto;

    /**
     * Concepto del ingreso.
     */
    @Basic
    @Column(name = "INGRDSCR", length = 500)
    private String descripcion;

    /**
     * Valor del ingreso.
     */
    @Basic
    @Column(name = "INGRVLOR")
    private Double valor;

    /**
     * Fecha en que entró el dinero.
     */
    @Basic
    @Column(name = "INGRFCHA")
    private LocalDate fecha;

    /**
     * Cuenta bancaria propia que recibió el dinero. FK a TSR.CNBC.
     */
    @ManyToOne
    @JoinColumn(name = "INGRCNBC", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /**
     * Referencia del ingreso (número de crédito, nota bancaria, etc.).
     */
    @Basic
    @Column(name = "INGRREFR", length = 200)
    private String referencia;

    /**
     * Estado del ingreso.
     * 1=Activo (contabilizado), 2=Anulado
     */
    @Basic
    @Column(name = "INGRESTD")
    private Long estado;

    /**
     * Asiento contable generado al registrar. FK a CNT.ASNT.
     */
    @ManyToOne
    @JoinColumn(name = "INGRASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Observaciones del ingreso.
     */
    @Basic
    @Column(name = "INGROBSR", length = 2000)
    private String observacion;

    /**
     * Usuario que registra el ingreso. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "INGRUSAR", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "INGRFCRG")
    private LocalDateTime fechaRegistro;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public ProductoCobro getProducto() { return producto; }
    public void setProducto(ProductoCobro producto) { this.producto = producto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public CuentaBancaria getCuentaBancaria() { return cuentaBancaria; }
    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) { this.cuentaBancaria = cuentaBancaria; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
