package com.saa.ejb.crd.service.dto;

/**
 * Lote 18: lo que el paso ⑦ provisiona en UNA cuota (interés y mora). Es el único cálculo del ⑦: el asiento por tipo de
 * préstamo es la suma de esta lista y el libro por cuota (MVIC tipo 1) se graba de ella, sin volver a calcular.
 */
public class ProvisionCuotaCalculada implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final Long idCuota;
    private final Long idPrestamo;
    private final Long idTipoPrestamo;
    private final double interes;
    private final double mora;

    public ProvisionCuotaCalculada(Long idCuota, Long idPrestamo, Long idTipoPrestamo, double interes, double mora) {
        this.idCuota = idCuota;
        this.idPrestamo = idPrestamo;
        this.idTipoPrestamo = idTipoPrestamo;
        this.interes = interes;
        this.mora = mora;
    }

    public Long getIdCuota() { return idCuota; }
    public Long getIdPrestamo() { return idPrestamo; }
    public Long getIdTipoPrestamo() { return idTipoPrestamo; }
    public double getInteres() { return interes; }
    public double getMora() { return mora; }
}
