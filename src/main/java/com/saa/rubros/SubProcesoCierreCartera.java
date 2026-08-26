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
 *         Interfaz del rubro SubProcesoCierreCartera.
 *         Los seis sub-procesos del cierre mensual de cartera
 *         ({@code CRD.ANCC.ANCCTPOO}), en el ORDEN en que deben ejecutarse.
 *
 *         <p>
 *         La numeración de este catálogo es la del sistema, no la de la pizarra. La
 *         correspondencia con §3.2 del levantamiento es:
 *         </p>
 *
 *         <table border="1">
 *         <caption>Correspondencia con la pizarra</caption>
 *         <tr><th>Aquí</th><th>Pizarra</th><th>Qué hace</th></tr>
 *         <tr><td>1</td><td>①</td><td>Asiento de vencidos</td></tr>
 *         <tr><td>2</td><td>②</td><td>Cambio de bandas del POR VENCER</td></tr>
 *         <tr><td>3</td><td>①.1</td><td>Reclasificación del VENCIDO</td></tr>
 *         <tr><td>4</td><td>③</td><td>Apertura del período de crédito</td></tr>
 *         <tr><td>5</td><td>④</td><td>Devengo de intereses</td></tr>
 *         <tr><td>6</td><td>⑥</td><td>Neteo de planillas / cierre</td></tr>
 *         </table>
 *
 *         <p>
 *         <b>El orden importa:</b> VENCIDOS mueve capital de la banda 1 de por vencer a la
 *         banda 1 de vencido, y las dos reclasificaciones parten de ese resultado. El
 *         sub-proceso ⑤ Seguros de la pizarra NO está aquí: la factura entra por CxP, no la
 *         genera CRD.
 *         </p>
 */
public interface SubProcesoCierreCartera {

	/** ① Capital no pagado del mes cerrado: sale de por vencer banda 1, entra a vencido banda 1. */
	public static final int VENCIDOS = 1;

	/** ② Reclasificación por diferencias de las bandas de cartera POR VENCER. */
	public static final int CAMBIO_BANDAS_POR_VENCER = 2;

	/** ①.1 Reclasificación por diferencias de las bandas de cartera VENCIDA. */
	public static final int CAMBIO_BANDAS_VENCIDO = 3;

	/** ③ Apertura del período: por cobrar del mes contra las cuentas por aplicar. */
	public static final int APERTURA = 4;

	/** ④ Devengo de intereses ordinarios y de mora contra las cuentas de ingreso. */
	public static final int DEVENGO_INTERESES = 5;

	/** ⑥ Neteo de planillas: reversa lo NO cobrado. Se fecha el último día del mes cerrado. */
	public static final int NETEO = 6;

}
