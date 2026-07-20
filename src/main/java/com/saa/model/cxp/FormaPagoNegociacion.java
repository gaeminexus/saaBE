package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Entity FormaPagoNegociacion.
 * Almacena el detalle de las cuotas/hitos/pagos acordados dentro de una negociacion con proveedor.
 * Cada registro representa una cuota pactada con su fecha, valor y porcentaje.
 * Tabla: PGS.FPNG
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FPNG", schema = "PGS")
@SequenceGenerator(name = "SQ_FPNGCDGO", sequenceName = "PGS.SQ_FPNGCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "FormaPagoNegociacionAll", query = "select e from FormaPagoNegociacion e"),
    @NamedQuery(name = "FormaPagoNegociacionId", query = "select e from FormaPagoNegociacion e where e.id = :id")
})
public class FormaPagoNegociacion implements Serializable {

    /** Id de tabla. */
    @Basic
    @Id
    @Column(name = "ID", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_FPNGCDGO")
    private Long id;

    /** Negociacion a la que pertenece esta cuota. */
    @ManyToOne
    @JoinColumn(name = "NEGOCIACION", referencedColumnName = "ID")
    private NegociacionProveedor negociacion;

    /** Numero de cuota/hito dentro de la negociacion (1, 2, 3...). */
    @Basic
    @Column(name = "NUMEROCUOTA")
    private Long numeroCuota;

    /** Descripcion de la cuota o hito (ej: "Anticipo", "Entrega parcial", "Liquidacion final"). */
    @Basic
    @Column(name = "DESCRIPCION", length = 1000)
    private String descripcion;

    /** Fecha acordada para realizar el pago de esta cuota. */
    @Basic
    @Column(name = "FECHAPAGO")
    private LocalDate fechaPago;

    /**
     * Porcentaje que representa esta cuota sobre el valor total.
     * Ej: 40.00, 30.00, 30.00 para una negociacion de tres pagos.
     * Puede ser nulo si el tipo de financiacion no es por porcentaje.
     */
    @Basic
    @Column(name = "PORCENTAJE")
    private Double porcentaje;

    /** Valor monetario pactado para esta cuota. */
    @Basic
    @Column(name = "VALORCUOTA")
    private Double valorCuota;

    /**
     * Estado de la cuota.
     * 1 = Pendiente, 2 = Pagado parcialmente, 3 = Pagado total, 0 = Anulado.
     */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    /** Orden de visualizacion. */
    @Basic
    @Column(name = "ORDEN")
    private Long orden;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NegociacionProveedor getNegociacion() { return negociacion; }
    public void setNegociacion(NegociacionProveedor negociacion) { this.negociacion = negociacion; }

    public Long getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(Long numeroCuota) { this.numeroCuota = numeroCuota; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public Double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(Double porcentaje) { this.porcentaje = porcentaje; }

    public Double getValorCuota() { return valorCuota; }
    public void setValorCuota(Double valorCuota) { this.valorCuota = valorCuota; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Long getOrden() { return orden; }
    public void setOrden(Long orden) { this.orden = orden; }
}
