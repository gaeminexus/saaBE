package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro RhhConceptoPlanillaIess (331). Concepto
 *         normalizado de un renglon de planilla del IESS (DLISCNCT): decide
 *         contra que total de {@code PlanillaControlIess} se compara el
 *         renglon al conciliar, en vez de adivinarlo por el texto libre que
 *         trae el comprobante del portal (docs/logica-negocio/rhh/
 *         API-PLANILLA-IESS.md #5.2 y #4.d).
 */
public interface RhhConceptoPlanillaIess {

	public static final int APORTE_PERSONAL = 1;
	public static final int APORTE_PATRONAL = 2;
	public static final int CONTRIBUCION_CCC = 3;
	public static final int SEGURO_SALUD_TIEMPO_PARCIAL = 4;
	public static final int OTRO = 5;

}
