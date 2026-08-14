package com.saa.ejb.crd.service.dto;

/**
 * Movimiento generado en CRD.APRT al pagar un préstamo con aportes: la fila
 * NEGATIVA creada y el PagoAporte que la respalda.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §7.4.
 */
public class MovimientoAporte {

    /** APRTCDGO de la fila creada */
    private Long idAporte;

    private Long idTipoAporte;

    /** Valor del movimiento; NEGATIVO al consumir aportes, positivo en un reverso */
    private Double valor;

    /** PGAPCDGO del PagoAporte asociado */
    private Long idPagoAporte;

    public MovimientoAporte() {
    }

    public Long getIdAporte() {
        return idAporte;
    }

    public void setIdAporte(Long idAporte) {
        this.idAporte = idAporte;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getIdPagoAporte() {
        return idPagoAporte;
    }

    public void setIdPagoAporte(Long idPagoAporte) {
        this.idPagoAporte = idPagoAporte;
    }
}
