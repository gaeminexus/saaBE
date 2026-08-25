package com.saa.model.cxp;

import java.io.Serializable;

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
 * Entity DetallePagoOrigenExterno.
 * Desglose contable de un pago cuyo documento de origen vive en otro módulo del sistema.
 * Tabla: PGS.DPGT
 *
 * Una fila por producto de pago. Al confirmarse el pago, CXP arma el asiento con
 * <b>una línea DEBE por cada detalle</b> (cuenta contable del grupo de su producto) y
 * <b>una sola línea HABER</b> a la cuenta contable del banco, por el total del pago.
 *
 * Existe porque un documento de origen puede cubrir varios conceptos, cada uno con su
 * cuenta contable: sin este desglose habría que emitir una orden de pago por concepto,
 * es decir N transferencias y N comisiones bancarias al mismo beneficiario.
 *
 * CXP no sabe qué representa cada producto: el mapeo concepto → producto de pago lo hace
 * el módulo que origina el pago, que sí puede conocer a CXP.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DPGT", schema = "PGS")
@NamedQueries({
    @NamedQuery(name = "DetallePagoOrigenExternoAll",
        query = "select e from DetallePagoOrigenExterno e"),
    @NamedQuery(name = "DetallePagoOrigenExternoId",
        query = "select e from DetallePagoOrigenExterno e where e.codigo = :id")
})
public class DetallePagoOrigenExterno implements Serializable {

    /**
     * Identificador único del detalle.
     */
    @Id
    @Basic
    @Column(name = "DPGTCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Pago programado al que pertenece el detalle. FK a PGS.PGTR.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRCDGO", referencedColumnName = "PGTRCDGO")
    private PagoProgramado pago;

    /**
     * Producto de pago que clasifica contablemente la línea. FK a PGS.PRDP.
     * La cuenta del asiento sale de {@code producto.grupoProducto.planCuenta}.
     *
     * OJO: la PK de PGS.PRDP se llama ID, no PRDPCDGO.
     */
    @ManyToOne
    @JoinColumn(name = "DPGTPRDP", referencedColumnName = "ID")
    private ProductoPago producto;

    /**
     * Valor imputado a este producto. La suma de los detalles debe igualar el valor del
     * pago, con tolerancia de 0.01.
     */
    @Basic
    @Column(name = "DPGTVLRR")
    private Double valor;

    /**
     * Concepto que se escribe en la descripción de la línea del asiento.
     */
    @Basic
    @Column(name = "DPGTCNCP", length = 500)
    private String concepto;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public PagoProgramado getPago() { return pago; }
    public void setPago(PagoProgramado pago) { this.pago = pago; }

    public ProductoPago getProducto() { return producto; }
    public void setProducto(ProductoPago producto) { this.producto = producto; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
}
