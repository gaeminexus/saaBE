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
 *         Interfaz del rubro RhhCampoArchivoBancario (224)
 *         Dato que va en cada campo de la linea de detalle del archivo bancario (DFMBCMPO)
 */
public interface RhhCampoArchivoBancario {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int SECUENCIAL = 1;
	public static final int IDENTIFICACION_DEL_BENEFICIARIO = 2;
	public static final int NOMBRE_DEL_BENEFICIARIO = 3;
	public static final int NUMERO_DE_CUENTA = 4;
	public static final int TIPO_DE_CUENTA = 5;
	public static final int CODIGO_DEL_BANCO = 6;
	public static final int VALOR = 7;
	public static final int MONEDA = 8;
	public static final int REFERENCIA = 9;
	public static final int FECHA_DE_PROCESO = 10;
	public static final int LITERAL_FIJO = 11;

}
