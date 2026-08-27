package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.cxp.ProductoPago;

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
 * Entity MovimientoCajaChica.
 * Apertura, gasto, reposición o ajuste de una caja chica. Tabla: TSR.MVCH.
 *
 * El tipo (ver {@link com.saa.rubros.TipoMovimientoCajaChica}) determina si el
 * valor suma o resta al saldo de la caja: 1 Apertura, 3 Reposición y
 * 4 Ajuste positivo suman; 2 Gasto y 5 Ajuste negativo restan.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MVCH", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "MovimientoCajaChicaAll", query = "select e from MovimientoCajaChica e"),
    @NamedQuery(name = "MovimientoCajaChicaId", query = "select e from MovimientoCajaChica e where e.codigo = :id")
})
public class MovimientoCajaChica implements Serializable {

    @Id
    @Basic
    @Column(name = "MVCHCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Caja chica a la que pertenece el movimiento. FK a TSR.CJCH. */
    @ManyToOne
    @JoinColumn(name = "CJCHCDGO", referencedColumnName = "CJCHCDGO")
    private CajaChica cajaChica;

    /** Tipo (rubro 232): 1=Apertura, 2=Gasto, 3=Reposición, 4=Ajuste+, 5=Ajuste-. */
    @Basic
    @Column(name = "MVCHTPOO")
    private Long tipo;

    /** Fecha del movimiento. */
    @Basic
    @Column(name = "MVCHFCHA")
    private LocalDate fecha;

    /** Valor positivo; el tipo determina si suma o resta al saldo. */
    @Basic
    @Column(name = "MVCHVLOR")
    private Double valor;

    /** Concepto. */
    @Basic
    @Column(name = "MVCHDSCR", length = 500)
    private String descripcion;

    /** Observación; obligatoria en gastos (se valida en el servicio). */
    @Basic
    @Column(name = "MVCHOBSR", length = 2000)
    private String observacion;

    /** Producto de pago (PGS.PRDP) que clasifica el gasto y da la cuenta contable vía su grupo. */
    @ManyToOne
    @JoinColumn(name = "MVCHPRDP", referencedColumnName = "ID")
    private ProductoPago producto;

    /** Beneficiario o proveedor (TSR.TTLR), opcional. */
    @ManyToOne
    @JoinColumn(name = "TTLRCDGO", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /** Número del comprobante pagado (factura, recibo, vale). */
    @Basic
    @Column(name = "MVCHNDOC", length = 50)
    private String numeroDocumento;

    /** Asiento contable generado. FK a CNT.ASNT. */
    @ManyToOne
    @JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /** Pago programado (PGS.PGTR) con el que se pagó la apertura o reposición desde el banco. */
    @ManyToOne
    @JoinColumn(name = "PGTRCDGO", referencedColumnName = "PGTRCDGO")
    private PagoProgramado pagoProgramado;

    /** Cierre de caja chica (TSR.CRCH) en el que quedó incluido. */
    @ManyToOne
    @JoinColumn(name = "CRCHCDGO", referencedColumnName = "CRCHCDGO")
    private CierreCajaChica cierre;

    /** Estado: 1=Activo, 2=Anulado. Ver {@link com.saa.rubros.EstadoMovimientoCajaChica}. */
    @Basic
    @Column(name = "MVCHESTD")
    private Long estado;

    /** Motivo de anulación. */
    @Basic
    @Column(name = "MVCHMTAN", length = 500)
    private String motivoAnulacion;

    /** Fecha de registro. */
    @Basic
    @Column(name = "MVCHFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registra. */
    @Basic
    @Column(name = "MVCHUSAR")
    private Long usuario;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public CajaChica getCajaChica() { return cajaChica; }
    public void setCajaChica(CajaChica cajaChica) { this.cajaChica = cajaChica; }

    public Long getTipo() { return tipo; }
    public void setTipo(Long tipo) { this.tipo = tipo; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public ProductoPago getProducto() { return producto; }
    public void setProducto(ProductoPago producto) { this.producto = producto; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public PagoProgramado getPagoProgramado() { return pagoProgramado; }
    public void setPagoProgramado(PagoProgramado pagoProgramado) { this.pagoProgramado = pagoProgramado; }

    public CierreCajaChica getCierre() { return cierre; }
    public void setCierre(CierreCajaChica cierre) { this.cierre = cierre; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Long getUsuario() { return usuario; }
    public void setUsuario(Long usuario) { this.usuario = usuario; }
}
