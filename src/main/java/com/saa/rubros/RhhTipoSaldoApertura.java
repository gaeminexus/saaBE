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
 *         Interfaz del rubro RhhTipoSaldoApertura (211)
 *         Tipo de saldo de apertura de la migracion (SLAPTPSL)
 */
public interface RhhTipoSaldoApertura {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int ANTIGUEDAD = 1;
	public static final int VACACIONES_PENDIENTES = 2;
	public static final int DECIMO_TERCERO_ACUMULADO = 3;
	public static final int DECIMO_CUARTO_ACUMULADO = 4;
	public static final int FONDOS_DE_RESERVA_ACUMULADOS = 5;
	public static final int PRESTAMO_IESS = 6;
	public static final int PRESTAMO_INTERNO = 7;
	public static final int IR_RETENIDO_EN_EL_ANIO = 8;
	/**
	 * Anticipo de sueldo pendiente al corte. Script 29.
	 *
	 * <p>Antes no existia y el anticipo habia que migrarlo como PRESTAMO_INTERNO, con lo que
	 * acababa materializado en el concepto equivocado: el rol de ASOPREP tiene una sola columna
	 * ANTIC SUELD, y dos anticipos en conceptos distintos cuadran el total y fallan el
	 * desglose.</p>
	 */
	public static final int ANTICIPO = 9;
	/**
	 * Prestamo hipotecario del IESS pendiente al corte. Script 29.
	 *
	 * <p>{@link #PRESTAMO_IESS} resolvia <b>siempre</b> a quirografario, asi que un hipotecario
	 * migrado acababa en el concepto del quirografario y ni la validacion avisaba. El control de
	 * prestamos de la calibracion compara las dos clases por separado.</p>
	 */
	public static final int PRESTAMO_HIPOTECARIO_IESS = 10;

}
