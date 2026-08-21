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
 *         Interfaz del rubro RhhTipoNovedadIess (204)
 *         Tipo de novedad IESS (NVISTPNV). El PDTRVLRN lleva el plazo legal en dias
 */
public interface RhhTipoNovedadIess {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int AVISO_DE_ENTRADA = 1;
	public static final int AVISO_DE_SALIDA = 2;
	public static final int MODIFICACION_DE_SUELDO = 3;
	public static final int NOVEDAD_FONDOS_DE_RESERVA = 4;
	public static final int CAMBIO_DE_MODALIDAD = 5;

	// Los que anadio el script 41 para cubrir el catalogo completo del IESS.
	//
	// El codigo del archivo batch de cada uno --ENT, SAL, MSU, INS, PFM-- vive en
	// PDTRVLRV del detalle, y el plazo legal en dias en PDTRVLRN. Aqui solo el nombre:
	// un cambio de plazo o de codigo es un UPDATE, no un despliegue.

	/** Ingreso imponible no permanente del mes --extras, subrogacion, encargo--, que sube el imponible sin cambiar el sueldo. */
	public static final int VARIACION_POR_EXTRAS = 6;
	/** Cambia el codigo de relacion de trabajo o el codigo sectorial del contrato. */
	public static final int CAMBIO_DE_RELACION_DE_TRABAJO = 7;
	/** Licencia sin remuneracion por maternidad o paternidad: suspende los aportes sin romper la relacion. */
	public static final int LICENCIA_SIN_REMUNERACION = 8;
	/** Cierra la licencia anterior antes de su fecha de fin. */
	public static final int REINTEGRO_ANTICIPADO_DE_LICENCIA = 9;
	/**
	 * Paso de jornada parcial a completa o al reves. Se registra en el portal como un
	 * nuevo sueldo, pero se modela aparte porque mueve tambien los dias declarados y el
	 * seguro de salud de tiempo parcial, no solo el sueldo.
	 */
	public static final int CAMBIO_DE_JORNADA = 10;
	/** Retroactivo por contrato colectivo. Nace inactivo: ASOPREP no lo usa. */
	public static final int RETROACTIVO_CONTRATO_COLECTIVO = 11;

}
