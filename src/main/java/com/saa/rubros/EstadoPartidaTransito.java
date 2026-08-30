package com.saa.rubros;

/**
 * @author GaemiSoft
 * <p>Interfaz del rubro EstadoPartidaTransito (240), estado de
 * <code>TSR.DTCN.DTCNESTD</code>.</p>
 */
public interface EstadoPartidaTransito {

    /** 1 - Pendiente: declarada en tránsito, todavía no se conoce su contraparte. */
    int PENDIENTE = 1;

    /** 2 - Saldada: se concilió con su contraparte (ver TSR.DTCN.DTCNCNSL). */
    int SALDADA = 2;

}
