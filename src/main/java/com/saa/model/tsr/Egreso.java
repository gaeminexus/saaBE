package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.ProductoPago;
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
 * Entity Egreso.
 * Egreso de tesorería sin respaldo de un documento físico: comisiones y
 * débitos por administración de cuentas bancarias, servicios bancarios, etc.
 * Tabla: TSR.EGRS
 *
 * La cuenta contable del gasto NO se configura aquí: sale del grupo del
 * producto CXP referenciado (ProductoPago.grupoProducto.planCuenta), igual
 * que en las facturas de compra.
 *
 * El egreso se paga a través del circuito de PagoProgramado (PGS.PGTR):
 * registrarlo crea su pago, que aparece en el listado de pagos a realizar y
 * sigue el flujo lote → archivo → confirmación (o se contabiliza de inmediato
 * si es débito automático). El asiento del pago queda vinculado aquí
 * (EGRSASNT): DEBE cuenta del grupo del producto / HABER cuenta del banco.
 *
 * La modalidad se guarda además en el propio egreso (EGRSDBAT, espejo de
 * PGS.PGTR.PGTRDBAT) para poder listarla y filtrarla sin cruzar con el pago.
 *
 * Estados (ver {@link com.saa.rubros.EstadoEgresoTesoreria}):
 *   1 = Pendiente de pago
 *   2 = Pagado (asiento y movimiento bancario generados)
 *   3 = Anulado
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "EGRS", schema = "TSR")
@SequenceGenerator(name = "SQ_EGRSCDGO", sequenceName = "TSR.SQ_EGRSCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "EgresoAll", query = "select e from Egreso e"),
    @NamedQuery(name = "EgresoId",  query = "select e from Egreso e where e.id = :id")
})
public class Egreso implements Serializable {

    /**
     * Identificador único del egreso.
     */
    @Basic
    @Id
    @Column(name = "EGRSCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_EGRSCDGO")
    private Long id;

    /**
     * Empresa a la que pertenece el egreso. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "EGRSPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Beneficiario del egreso. FK a TSR.TTLR.
     * Opcional; obligatorio cuando el pago va por transferencia (el archivo
     * del banco necesita la cuenta de destino del titular).
     */
    @ManyToOne
    @JoinColumn(name = "EGRSTTLR", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /**
     * Producto CXP que clasifica el gasto. FK a PGS.PRDP.
     * La cuenta contable sale del grupo del producto (GRPP.planCuenta).
     */
    @ManyToOne
    @JoinColumn(name = "EGRSPRDP", referencedColumnName = "ID")
    private ProductoPago producto;

    /**
     * Concepto del egreso.
     */
    @Basic
    @Column(name = "EGRSDSCR", length = 500)
    private String descripcion;

    /**
     * Marca de débito automático: 0=No (transferencia normal), 1=Sí.
     * Copia de la modalidad con la que se registró el egreso, igual que
     * PGTRDBAT en el pago: un egreso por débito automático no necesita
     * beneficiario ni cuenta de destino y no pasa por el archivo del banco,
     * se contabiliza al registrarlo. La marca conserva la modalidad de
     * origen aunque después se reverse el pago.
     */
    @Basic
    @Column(name = "EGRSDBAT")
    private Long debitoAutomatico;

    /**
     * Valor del egreso.
     */
    @Basic
    @Column(name = "EGRSVLOR")
    private Double valor;

    /**
     * Fecha del egreso.
     */
    @Basic
    @Column(name = "EGRSFCHA")
    private LocalDate fecha;

    /**
     * Estado del egreso.
     * 1=Pendiente de pago, 2=Pagado, 3=Anulado
     */
    @Basic
    @Column(name = "EGRSESTD")
    private Long estado;

    /**
     * Asiento contable generado al confirmarse el pago. FK a CNT.ASNT.
     * Nulo mientras el egreso está pendiente.
     */
    @ManyToOne
    @JoinColumn(name = "EGRSASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Observaciones del egreso.
     */
    @Basic
    @Column(name = "EGRSOBSR", length = 2000)
    private String observacion;

    /**
     * Usuario que registra el egreso. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "EGRSUSAR", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "EGRSFCRG")
    private LocalDateTime fechaRegistro;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public ProductoPago getProducto() { return producto; }
    public void setProducto(ProductoPago producto) { this.producto = producto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Long getDebitoAutomatico() { return debitoAutomatico; }
    public void setDebitoAutomatico(Long debitoAutomatico) { this.debitoAutomatico = debitoAutomatico; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

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
