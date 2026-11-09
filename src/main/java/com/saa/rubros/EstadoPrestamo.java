/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * José Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa está protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducción o la distribución no autorizadas de este programa, o de cualquier parte del mismo, 
 * está penada por la ley y con severas sanciones civiles y penales, y será objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad  
 * con los términos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro EstadoPrestamo
 *         Estados que puede tener un préstamo
 */
public interface EstadoPrestamo {

	// Ids de los elementos hijos
	public static final int RAIZ = 0;
	public static final int GENERADO = 1;
	public static final int VIGENTE = 2;
	public static final int CANCELADO = 3;
	public static final int CANCELADO_ANTICIPADO = 4;
	public static final int CANCELADO_POR_NOVACION = 5;
	public static final int PENDIENTE_DE_APROBACION = 6;
	public static final int RECHAZADO = 7;
	public static final int DE_PLAZO_VENCIDO = 8;
	public static final int CANCELADO_POR_REVISAR = 9;
	public static final int VIGENTE_POR_REVISAR = 10;
	public static final int EN_MORA = 11;
	/** Préstamo otorgado y anulado (DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md). Nuevo 2026-10-13. */
	public static final int ANULADO = 12;
	/** Original cancelado por una reestructuración (DISENO-REESTRUCTURACION-NOVACION.md RN11). Terminal. Nuevo 2026-10-13. */
	public static final int CANCELADO_POR_REESTRUCTURACION = 13;
	/**
	 * Aprobado por todos los niveles, con número Asoprep y orden de pago emitida, pero SIN dinero entregado
	 * (DISENO-ESTADO-APROBADO-VIGENTE-AL-DESEMBOLSO.md). Pasa a VIGENTE (2) cuando se confirma el pago del desembolso.
	 * No cuenta en la cartera, el archivo Petro, la mora, los pagos ni la deuda. Nuevo 2026-11-08.
	 */
	public static final int APROBADO = 14;

	/**
	 * Estados «vivos»: el dinero ya se entregó y el préstamo se cobra y se cuenta (2 VIGENTE, 8 DE PLAZO VENCIDO,
	 * 10 VIGENTE POR REVISAR, 11 EN MORA). Lista blanca de BARRIDO-ESTADO-GENERADO-COMO-VIGENTE.md.
	 */
	public static boolean esVivo(Long idEstado) {
		return idEstado != null && (idEstado == VIGENTE || idEstado == DE_PLAZO_VENCIDO
			|| idEstado == VIGENTE_POR_REVISAR || idEstado == EN_MORA);
	}

	/** Aún sin desembolsar: 1 GENERADO, 6 PENDIENTE DE APROBACIÓN, 7 RECHAZADO o 14 APROBADO. No admite pagos ni cobros. */
	public static boolean esNoDesembolsado(Long idEstado) {
		return idEstado != null && (idEstado == GENERADO || idEstado == PENDIENTE_DE_APROBACION
			|| idEstado == RECHAZADO || idEstado == APROBADO);
	}

}
