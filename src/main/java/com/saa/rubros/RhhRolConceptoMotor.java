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
 *         Interfaz del rubro RhhRolConceptoMotor (221)
 *         Rol del concepto dentro del motor de calculo (CPNMROLM). Es como el motor localiza cada concepto, sin depender del codigo alterno
 */
public interface RhhRolConceptoMotor {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int APORTE_PERSONAL = 1;
	public static final int APORTE_PATRONAL = 2;
	public static final int IECE = 3;
	public static final int SECAP = 4;
	public static final int FONDOS_DE_RESERVA = 5;
	public static final int DECIMO_TERCERO = 6;
	public static final int DECIMO_CUARTO = 7;
	public static final int IMPUESTO_A_LA_RENTA = 8;
	public static final int HORA_SUPLEMENTARIA = 9;
	public static final int HORA_EXTRAORDINARIA = 10;
	public static final int RECARGO_NOCTURNO = 11;
	public static final int PRESTAMO_QUIROGRAFARIO = 12;
	public static final int PRESTAMO_HIPOTECARIO = 13;
	public static final int ANTICIPO_DE_SUELDO = 14;
	public static final int PRESTAMO_INTERNO = 15;
	public static final int RETENCION_JUDICIAL = 16;

	// Conceptos de provision. Llevan rol propio por dos razones: para que la fila de
	// PVNM apunte al concepto de provision y no al mensualizado, y porque la jubilacion
	// patronal y el desahucio comparten terna (PROVISION / MANUAL / SUELDO_CONTRATO) y
	// sin rol no habria forma de distinguirlos.
	public static final int PROVISION_DECIMO_TERCERO = 17;
	public static final int PROVISION_DECIMO_CUARTO = 18;
	public static final int PROVISION_VACACIONES = 19;
	public static final int PROVISION_FONDOS_DE_RESERVA = 20;
	public static final int PROVISION_JUBILACION_PATRONAL = 21;
	public static final int PROVISION_DESAHUCIO = 22;

	// Rubros del finiquito. Se anadieron el 2026-08-19 (script 17): antes se localizaban
	// por CPNMALTR 60-67, que discriminaba bien pero dejaba valores de catalogo quemados en
	// Java --contra la regla 1-- y era el unico sitio del modulo que no usaba el rol. Trece
	// sitios por rol y uno por codigo alterno acaban en que alguien copia el patron malo.
	public static final int FINIQUITO_DECIMO_TERCERO = 23;
	public static final int FINIQUITO_DECIMO_CUARTO = 24;
	public static final int FINIQUITO_VACACIONES = 25;
	public static final int FINIQUITO_FONDOS_DE_RESERVA = 26;
	public static final int FINIQUITO_DESAHUCIO = 27;
	public static final int FINIQUITO_DESPIDO_INTEMPESTIVO = 28;
	public static final int FINIQUITO_JUBILACION_PATRONAL = 29;
	public static final int FINIQUITO_REMUNERACION_PENDIENTE = 30;
	/**
	 * Aporte personal al IESS del finiquito, unico rubro de tipo EGRESO de los nueve.
	 * Se calcula <b>solo sobre la remuneracion pendiente</b>: indemnizaciones, decimos y
	 * vacaciones no son materia gravada. Script 22.
	 */
	public static final int FINIQUITO_APORTE_PERSONAL = 31;

}
