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

	/**
	 * (2026-11-01) id del producto de pago de la DEVOLUCIÓN al partícipe del sobrante de la carga Petro/ARCH (B8), en
	 * {@code PDTRVLRN}. Se verifica contra la cuenta de la línea aux1 61 de la plantilla de aplicación. Hoy 1222.
	 */
	int PRODUCTO_PAGO_DEVOLUCION_DESCUENTO = 4;

	/**
	 * (2026-11-01) id del producto de pago del DESEMBOLSO de préstamos, en {@code PDTRVLRN}. Se verifica contra la cuenta
	 * SOCIOS POR PAGAR de la plantilla de entrega del préstamo. Hoy 976.
	 */
	int PRODUCTO_PAGO_DESEMBOLSO_PRESTAMO = 5;

}
