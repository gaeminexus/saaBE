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
 *         Interfaz del rubro RhhLineaAsiento (214)
 *         Papel de la linea dentro del asiento contable. Se graba en CNT.DTPL.DTPLAXL1
 */
public interface RhhLineaAsiento {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int GASTO_SUELDOS_Y_SALARIOS = 1;
	public static final int GASTO_HORAS_EXTRA = 2;
	public static final int GASTO_APORTE_PATRONAL_IESS = 3;
	public static final int GASTO_IECE_Y_SECAP = 4;
	public static final int GASTO_FONDOS_DE_RESERVA = 5;
	public static final int GASTO_DECIMO_TERCERO = 6;
	public static final int GASTO_DECIMO_CUARTO = 7;
	public static final int IESS_POR_PAGAR_APORTE_PERSONAL = 10;
	public static final int IESS_POR_PAGAR_APORTE_PATRONAL = 11;
	public static final int IESS_POR_PAGAR_PRESTAMOS = 12;
	public static final int SRI_RETENCION_EN_LA_FUENTE_RD = 13;
	public static final int CUENTAS_POR_COBRAR_EMPLEADOS = 14;
	public static final int RETENCIONES_JUDICIALES_POR_PAGAR = 15;
	public static final int FONDOS_DE_RESERVA_POR_PAGAR = 16;
	public static final int DECIMOS_POR_PAGAR = 17;
	public static final int SUELDOS_POR_PAGAR = 18;
	public static final int GASTO_PROVISION_DECIMO_TERCERO = 30;
	public static final int GASTO_PROVISION_DECIMO_CUARTO = 31;
	public static final int GASTO_PROVISION_VACACIONES = 32;
	public static final int GASTO_PROVISION_FONDOS_DE_RESERVA = 33;
	public static final int GASTO_PROVISION_JUBILACION_PATRONAL = 34;
	public static final int GASTO_PROVISION_DESAHUCIO = 35;
	public static final int PROVISION_DECIMO_TERCERO_POR_PAGAR = 40;
	public static final int PROVISION_DECIMO_CUARTO_POR_PAGAR = 41;
	public static final int PROVISION_VACACIONES_POR_PAGAR = 42;
	public static final int PROVISION_FONDOS_DE_RESERVA_POR_PAGAR = 43;
	public static final int PROVISION_JUBILACION_PATRONAL = 44;
	public static final int PROVISION_DESAHUCIO = 45;
	public static final int SUELDOS_POR_PAGAR_DEBE = 50;
	public static final int BANCO = 51;
	public static final int GASTO_DESAHUCIO = 60;
	public static final int GASTO_DESPIDO_INTEMPESTIVO = 61;
	public static final int GASTO_JUBILACION_PATRONAL = 62;
	public static final int GASTO_SUELDOS_LIQUIDACION = 63;
	public static final int LIQUIDACIONES_POR_PAGAR = 70;

}
