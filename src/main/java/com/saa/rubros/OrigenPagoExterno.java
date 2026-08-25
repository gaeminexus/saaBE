package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Etiquetas de los procesos externos que pueden originar un pago en CXP
 *         (PGS.PGTR.PGTRORGN).
 *
 *         Para CXP el valor es una CADENA OPACA: la guarda junto al id del documento
 *         origen (PGTRIDOR) y la devuelve en los listados, pero nunca la resuelve ni la
 *         interpreta. Ninguna clase de CXP importa nada del modulo que la produce.
 *
 *         Por eso esta interfaz vive en com.saa.rubros y no en com.saa.ejb.cxp: las
 *         constantes nombran modulos, pero solo como texto. Si manana se retira el modulo
 *         crd del producto comercial, se borra la constante correspondiente y NADA deja de
 *         compilar; las columnas PGTRORGN / PGTRIDOR simplemente quedan siempre nulas.
 */
public interface OrigenPagoExterno {

	/**
	 * Devolucion de aportes a un participe, originada en CRD.DVAP.
	 * PGTRIDOR lleva el CRD.DVAP.DVAPCDGO correspondiente.
	 */
	public static final String CRD_DEVOLUCION_APORTE = "CRD_DEVOLUCION_APORTE";

}
