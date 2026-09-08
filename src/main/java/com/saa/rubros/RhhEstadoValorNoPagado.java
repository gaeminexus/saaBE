/**
 * Copyright Gaemi Soft Cia. Ltda. , 2011 Reservados todos los derechos
 * Jose Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa esta protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproduccion o la distribucion no autorizadas de este programa, o de cualquier parte del mismo,
 * esta penada por la ley y con severas sanciones civiles y penales, y sera objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informacion confidencial y se utilizara solo en conformidad
 * con los terminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro RhhEstadoValorNoPagado (311)
 *         Estado del valor no pagado a un empleado en un periodo (RHH.VNPG.VNPGESTD)
 *         Ver docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md §5. Script e2-26.
 */
public interface RhhEstadoValorNoPagado {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)

	/**
	 * Al registrar. El rol del periodo P todavia no se proceso.
	 */
	public static final int REGISTRADO = 1;

	/**
	 * Al generar la orden de pago de P: el empleado cobro N - X. Sigue vivo (cuenta para el
	 * indice de un solo registro por empleado/periodo) hasta que se confirma el pago de P+1.
	 */
	public static final int RETENIDO = 2;

	/**
	 * Al confirmar el pago de la orden de P+1: el empleado cobro M + X, saldo liquidado en
	 * remuneraciones por pagar.
	 */
	public static final int PAGADO = 3;

	/**
	 * Con motivo. Solo permitido desde REGISTRADO -- un RETENIDO ya afecto una orden de
	 * pago, y ahi la via es revertir la orden, no anular el registro.
	 */
	public static final int ANULADO = 4;

	/**
	 * El empleado salio antes de cobrarlo: lo absorbio la liquidacion de haberes (finiquito).
	 */
	public static final int FINIQUITADO = 5;

}
