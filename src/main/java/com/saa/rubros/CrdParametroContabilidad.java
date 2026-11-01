package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CrdParametroContabilidad (237), parametros
 *         globales de contabilidad de CRD. El valor vive en PDTRVLRN (0/1).
 */
public interface CrdParametroContabilidad {

	int CONTABILIDAD_ACTIVA = 1;

	/**
	 * P9b (2026-11-01): id del producto de pago (PGS.PRDP) de la PENSIÓN complementaria de cada jubilado, en
	 * {@code PDTRVLRN}. El código verifica que la cuenta del grupo del producto sea la de la línea aux1 2 de la plantilla
	 * {@code PAGO_PENSION_COMPLEMENTARIA}. Hoy 517 («Pension Complementaria Jubilados»).
	 */
	int PRODUCTO_PAGO_PENSION_JUBILADOS = 2;

	/**
	 * P9b (2026-11-01): id del producto de pago del SEGURO de los jubilados, en {@code PDTRVLRN}. El código verifica que
	 * la cuenta de su grupo sea la de la línea aux1 4 de la plantilla {@code PAGO_PENSION_COMPLEMENTARIA}. Hoy 516.
	 */
	int PRODUCTO_PAGO_SEGURO_JUBILADOS = 3;

}
