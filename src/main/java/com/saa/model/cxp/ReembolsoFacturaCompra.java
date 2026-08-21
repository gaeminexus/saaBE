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
import jakarta.persistence.Table;

/**
 * Entity ReembolsoFacturaCompra.
 * Detalle de reembolsos de gastos de una factura de compra (tabla PGS.RMBF).
 * Un registro por documento sustento (tag reembolsoDetalle del XML SRI, ANEXO 5 Ficha Tecnica).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RMBF", schema = "PGS")
@NamedQueries({
    @NamedQuery(name = "ReembolsoFacturaCompraAll",
        query = "select e from ReembolsoFacturaCompra e"),
    @NamedQuery(name = "ReembolsoFacturaCompraId",
        query = "select e from ReembolsoFacturaCompra e where e.id = :id"),
    @NamedQuery(name = "ReembolsoFacturaCompraByFactura",
        query = "select e from ReembolsoFacturaCompra e where e.factura.id = :idFactura and e.estado = 1 order by e.id")
})
public class ReembolsoFacturaCompra implements Serializable {

    /** PK identity (RMBFCDGO). */
    @Basic @Id @Column(name = "RMBFCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Factura de compra a la que pertenece el reembolso (FK RMBFFCTC -> PGS.FCTC). */
    @ManyToOne
    @JoinColumn(name = "RMBFFCTC", referencedColumnName = "ID")
    private FacturaCompra factura;

    /** Tipo identificacion del proveedor del gasto (tabla 6 SRI: 04=RUC 05=Cedula ...). */
    @Basic @Column(name = "RMBFTIPR", length = 2)
    private String tipoIdentificacionProveedor;

    /** Identificacion del proveedor del gasto reembolsado. */
    @Basic @Column(name = "RMBFIDPR", length = 20)
    private String identificacionProveedor;

    /** Codigo pais de pago (tabla 25 SRI, 593=Ecuador). */
    @Basic @Column(name = "RMBFCDPS", length = 3)
    private String codPaisPago;

    /** Tipo proveedor reembolso (tabla 26 SRI: 01=Persona natural 02=Sociedad). */
    @Basic @Column(name = "RMBFTPPR", length = 2)
    private String tipoProveedor;

    /** Tipo de documento sustento (tabla 3 SRI: 01=Factura 03=Liquidacion ...). */
    @Basic @Column(name = "RMBFCDDC", length = 2)
    private String codDoc;

    /** Establecimiento del documento sustento (estabDocReembolso). */
    @Basic @Column(name = "RMBFESTB", length = 3)
    private String establecimiento;

    /** Punto de emision del documento sustento (ptoEmiDocReembolso). */
    @Basic @Column(name = "RMBFPTEM", length = 3)
    private String puntoEmision;

    /** Secuencial del documento sustento (secuencialDocReembolso). */
    @Basic @Column(name = "RMBFSCNL", length = 9)
    private String secuencial;

    /** Fecha de emision del documento sustento. */
    @Basic @Column(name = "RMBFFEMS")
    private LocalDate fechaEmision;

    /** Numero de autorizacion / clave de acceso del documento sustento. */
    @Basic @Column(name = "RMBFNAUT", length = 49)
    private String numeroAutorizacion;

    /** Base imponible tarifa 0 / no objeto / exento. */
    @Basic @Column(name = "RMBFBSCR")
    private Double baseImponibleCero;

    /** Base imponible gravada. */
    @Basic @Column(name = "RMBFBSGR")
    private Double baseImponibleGravada;

    /** Tarifa IVA de la base gravada (15/12/8/5). */
    @Basic @Column(name = "RMBFTRIV")
    private Double tarifaIva;

    /** Valor IVA. */
    @Basic @Column(name = "RMBFVLIV")
    private Double valorIva;

    /** Valor ICE. */
    @Basic @Column(name = "RMBFVLIC")
    private Double valorIce;

    /** Total del documento sustento (bases + impuestos). */
    @Basic @Column(name = "RMBFTTAL")
    private Double total;

    /**
     * Id de producto PGS.PRDP para la contabilizacion por grupo
     * (sin FK JPA, igual que DFCC.PRODUCTO).
     */
    @Basic @Column(name = "RMBFPRDC")
    private Long producto;

    /** Origen del registro: 1=Leido del XML 2=Ingresado manualmente (OrigenReembolso). */
    @Basic @Column(name = "RMBFORGN")
    private Long origen;

    /** Estado: 1=Activo 0=Anulado. */
    @Basic @Column(name = "RMBFESTD")
    private Long estado;

    @Basic @Column(name = "RMBFOBSR", length = 500)
    private String observacion;

    // ─── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FacturaCompra getFactura() { return factura; }
    public void setFactura(FacturaCompra factura) { this.factura = factura; }

    public String getTipoIdentificacionProveedor() { return tipoIdentificacionProveedor; }
    public void setTipoIdentificacionProveedor(String tipoIdentificacionProveedor) { this.tipoIdentificacionProveedor = tipoIdentificacionProveedor; }

    public String getIdentificacionProveedor() { return identificacionProveedor; }
    public void setIdentificacionProveedor(String identificacionProveedor) { this.identificacionProveedor = identificacionProveedor; }

    public String getCodPaisPago() { return codPaisPago; }
    public void setCodPaisPago(String codPaisPago) { this.codPaisPago = codPaisPago; }

    public String getTipoProveedor() { return tipoProveedor; }
    public void setTipoProveedor(String tipoProveedor) { this.tipoProveedor = tipoProveedor; }

    public String getCodDoc() { return codDoc; }
    public void setCodDoc(String codDoc) { this.codDoc = codDoc; }

    public String getEstablecimiento() { return establecimiento; }
    public void setEstablecimiento(String establecimiento) { this.establecimiento = establecimiento; }

    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String puntoEmision) { this.puntoEmision = puntoEmision; }

    public String getSecuencial() { return secuencial; }
    public void setSecuencial(String secuencial) { this.secuencial = secuencial; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getNumeroAutorizacion() { return numeroAutorizacion; }
    public void setNumeroAutorizacion(String numeroAutorizacion) { this.numeroAutorizacion = numeroAutorizacion; }

    public Double getBaseImponibleCero() { return baseImponibleCero; }
    public void setBaseImponibleCero(Double baseImponibleCero) { this.baseImponibleCero = baseImponibleCero; }

    public Double getBaseImponibleGravada() { return baseImponibleGravada; }
    public void setBaseImponibleGravada(Double baseImponibleGravada) { this.baseImponibleGravada = baseImponibleGravada; }

    public Double getTarifaIva() { return tarifaIva; }
    public void setTarifaIva(Double tarifaIva) { this.tarifaIva = tarifaIva; }

    public Double getValorIva() { return valorIva; }
    public void setValorIva(Double valorIva) { this.valorIva = valorIva; }

    public Double getValorIce() { return valorIce; }
    public void setValorIce(Double valorIce) { this.valorIce = valorIce; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Long getProducto() { return producto; }
    public void setProducto(Long producto) { this.producto = producto; }

    public Long getOrigen() { return origen; }
    public void setOrigen(Long origen) { this.origen = origen; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
