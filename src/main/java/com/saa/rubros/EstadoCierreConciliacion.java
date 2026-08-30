package com.saa.rubros;

/**
 * @author GaemiSoft
 * <p>Interfaz del rubro EstadoCierreConciliacion (241), estado de
 * <code>TSR.CNCL.CNCLESTD</code>.</p>
 */
public interface EstadoCierreConciliacion {

    /** 1 - Borrador. No usado por el flujo actual (cerrar() crea el CNCL ya en CERRADO), reservado. */
    int BORRADOR = 1;

    /** 2 - Cerrado: el mes quedó cerrado con esta declaración de partidas en tránsito. */
    int CERRADO = 2;

    /** 3 - Anulado: se deshizo con anularCierre, liberando las partidas que declaró. */
    int ANULADO = 3;

}
