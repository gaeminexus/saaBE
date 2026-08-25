package com.saa.ejb.cxp.service.dto;

/**
 * Una línea del desglose contable de un pago de origen externo: cuánto del pago se imputa
 * a un producto de pago (PGS.PRDP).
 *
 * Se persiste en PGS.DPGT y, al confirmarse el pago, produce <b>una línea DEBE</b> del
 * asiento con la cuenta contable del grupo de ese producto
 * ({@code producto.grupoProducto.planCuenta}).
 *
 * CXP no sabe qué representa cada producto: el mapeo concepto → producto lo hace el módulo
 * que origina el pago.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class LineaContablePago {

    /** Producto de pago que clasifica la línea: PGS.PRDP.ID. Obligatorio. */
    private Long idProductoPago;

    /** Valor imputado a este producto. La suma de las líneas debe igualar el valor del pago. */
    private Double valor;

    /** Concepto que se escribe en la descripción de la línea del asiento. */
    private String concepto;

    public LineaContablePago() {
    }

    public Long getIdProductoPago() {
        return idProductoPago;
    }

    public void setIdProductoPago(Long idProductoPago) {
        this.idProductoPago = idProductoPago;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }
}
