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
 *         Estados REALES de {@code CRD.CRAR.CRARESTD} (carga del archivo Petro).
 *
 *         <p>
 *         <b>Existe porque el rubro 166 {@link ASPEstadoCargaArchivoPetro} NO describe esta
 *         columna.</b> Aquel catálogo define {@code 1 CARGADO, 2 VALIDADO,
 *         3 APROBADO_CONTABILIDAD, 4 PROCESADO}, pero:
 *         </p>
 *         <ul>
 *         <li>ninguna clase del proyecto lo referencia — es una interfaz muerta;</li>
 *         <li>{@code CargaArchivoPetroServiceImpl} escribe el literal {@code 3L} al terminar
 *         la fase 3 y su propio comentario lo llama "PROCESADO", que en el rubro 166 es el
 *         {@code 4};</li>
 *         <li>el {@code 4} no se escribe nunca;</li>
 *         <li>al 2026-08-25 las 14 cargas de la BD están todas en {@code 3}.</li>
 *         </ul>
 *
 *         <p>
 *         La discrepancia ya estaba documentada en
 *         {@code docs/logica-negocio/petro/REGLAS-GENERALES-PETRO.md} §Estados de
 *         CargaArchivo. Esta interfaz solo la lleva al código para que nadie vuelva a
 *         escribir un 3 suelto ni a referenciar el rubro equivocado.
 *         </p>
 *
 *         <p>
 *         <b>No renumerar sin migrar los datos.</b> El orden cronológico de las cargas
 *         ({@code validarOrdenProcesamiento}) y el control de archivo del cierre de cartera
 *         ({@code CierreCarteraService}) dependen los dos del {@code 3}.
 *         </p>
 */
public interface CrdEstadoCargaArchivo {

	/**
	 * Carga creada. Lo escribe {@code CargaArchivoServiceImpl} con
	 * {@link Estado#ACTIVO}: el archivo está subido pero sus pagos NO se han aplicado, así
	 * que los aportes del mes todavía no existen en {@code CRD.APRT}.
	 */
	public static final int CARGADO = 1;

	/**
	 * Pagos aplicados (la fase 3 terminó). Es el estado en el que la carga ya generó los
	 * aportes y los pagos de cuotas, y por tanto el ÚNICO que permite cerrar contablemente
	 * ese mes.
	 */
	public static final int PROCESADO = 3;

}
