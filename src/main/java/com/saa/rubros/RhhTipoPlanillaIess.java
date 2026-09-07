package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro RhhTipoPlanillaIess (330). Tipo de planilla del
 *         IESS (PLISTIPO): cada una tiene su propio comprobante y su propio
 *         pago (docs/logica-negocio/rhh/API-PLANILLA-IESS.md #1).
 */
public interface RhhTipoPlanillaIess {

	public static final int ROL_NORMAL = 1;
	public static final int PRESTAMOS_QUIROGRAFARIOS = 2;
	public static final int PRESTAMOS_HIPOTECARIOS = 3;
	public static final int FONDOS_DE_RESERVA = 4;

}
