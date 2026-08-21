package com.saa.rubros;

/** Origen de un registro de reembolso de gastos (PGS.RMBF.RMBFORGN). */
public interface OrigenReembolso {
    /** Leido del XML SRI (tag reembolsoDetalle). */
    int XML    = 1;
    /** Ingresado manualmente desde la interfaz. */
    int MANUAL = 2;
}
