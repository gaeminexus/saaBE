package com.saa.ejb.cxp.service.dto;

/**
 * Una forma de pago de una nota de venta de compra ingresada a mano.
 * Espejo de {@code FormaPagoFacturaCompra}. Ver
 * docs/logica-negocio/cxp/API-NOTA-VENTA-COMPRA-MANUAL.md §1.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class SolicitudNotaVentaCompraFormaPago {

    /** Código de forma de pago (catálogo SRI). Obligatorio si el arreglo trae al menos una línea. */
    private String formaPago;

    /** Valor de esta forma de pago. Obligatorio si el arreglo trae al menos una línea. */
    private Double valor;

    /** Plazo. Opcional, por defecto 0. */
    private Long plazo;

    /** Unidad de tiempo del plazo ("dias", etc). Opcional. */
    private String unidadTiempo;

    public SolicitudNotaVentaCompraFormaPago() {
    }

    public String getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getPlazo() {
        return plazo;
    }

    public void setPlazo(Long plazo) {
        this.plazo = plazo;
    }

    public String getUnidadTiempo() {
        return unidadTiempo;
    }

    public void setUnidadTiempo(String unidadTiempo) {
        this.unidadTiempo = unidadTiempo;
    }
}
