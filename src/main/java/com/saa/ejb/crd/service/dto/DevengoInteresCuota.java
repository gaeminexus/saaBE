package com.saa.ejb.crd.service.dto;

/**
 * Lote 20: el interés que el ④ o el ④b devenga en UNA cuota. El asiento por tipo de préstamo es la suma de esta lista (redondeada por cuota) y el
 * libro por cuota (MVIC tipo 11) se graba de ella, con el mismo asiento.
 */
public class DevengoInteresCuota implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final Long idCuota;
    private final Long idPrestamo;
    private final Long idTipoPrestamo;
    private final double valor;

    public DevengoInteresCuota(Long idCuota, Long idPrestamo, Long idTipoPrestamo, double valor) {
        this.idCuota = idCuota;
        this.idPrestamo = idPrestamo;
        this.idTipoPrestamo = idTipoPrestamo;
        this.valor = valor;
    }

    public Long getIdCuota() { return idCuota; }
    public Long getIdPrestamo() { return idPrestamo; }
    public Long getIdTipoPrestamo() { return idTipoPrestamo; }
    public double getValor() { return valor; }
}
