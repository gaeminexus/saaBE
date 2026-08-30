package com.saa.ejb.crd.service.dto;

/**
 * Una fila del comprobante de cobro múltiple: lo aplicado a UN préstamo de la operación,
 * desglosado por los 5 conceptos (capital, interés, mora, desgravamen, seguro de incendio —
 * mismo criterio de esta semana: el interés vencido se suma dentro de "interés", no es un
 * concepto aparte).
 *
 * Reconstruida desde CRD.PGPR (los PagoPrestamo vigentes del evento), nunca desde datos que
 * mande el cliente: es un comprobante financiero, no un eco de lo que la pantalla mostró.
 */
public class LineaComprobantePagoMultiple {

    private Long idPrestamo;

    /** PRSTIDAS (número visible al socio); si es null, el código interno como respaldo */
    private Long numeroPrestamo;

    private String nombreProducto;

    private double capital;
    private double interes;
    private double mora;
    private double desgravamen;
    private double seguroIncendio;

    /** Suma de los 5 componentes de esta fila */
    private double total;

    public LineaComprobantePagoMultiple() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getNumeroPrestamo() {
        return numeroPrestamo;
    }

    public void setNumeroPrestamo(Long numeroPrestamo) {
        this.numeroPrestamo = numeroPrestamo;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public double getCapital() {
        return capital;
    }

    public void setCapital(double capital) {
        this.capital = capital;
    }

    public double getInteres() {
        return interes;
    }

    public void setInteres(double interes) {
        this.interes = interes;
    }

    public double getMora() {
        return mora;
    }

    public void setMora(double mora) {
        this.mora = mora;
    }

    public double getDesgravamen() {
        return desgravamen;
    }

    public void setDesgravamen(double desgravamen) {
        this.desgravamen = desgravamen;
    }

    public double getSeguroIncendio() {
        return seguroIncendio;
    }

    public void setSeguroIncendio(double seguroIncendio) {
        this.seguroIncendio = seguroIncendio;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
