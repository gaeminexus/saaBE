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
 *         Interfaz del rubro RhhEstadoOrdenPago (208)
 *         Estado de la orden de pago de nomina (RDPGESTD)
 *
 *         <p><b>RECHAZADA_PARCIAL(4) y ANULADA(5) estan definidos en el catalogo y
 *         NADIE LOS ASIGNA</b> en ningun punto del codigo (verificado 2026-09-08, grep de
 *         {@code RhhEstadoOrdenPago.ANULADA}/{@code .RECHAZADA_PARCIAL} sin resultados). No
 *         existe hoy ningun reverso para una {@code OrdenPagoNomina} ya confirmada:
 *         {@code GeneracionOrdenPagoService} y {@code OrdenPagoNominaService} no tienen
 *         metodo revertir/anular. Lo unico que hoy "deshace" una orden es el
 *         {@code DELETE /rdpg/{id}} generico, con un guard minimo agregado en
 *         {@code OrdenPagoNominaServiceImpl.exigeBorrable} (rechaza si ya se acredito o si
 *         algun {@code RHH.VNPG} la referencia) -- no un reverso, solo evita el borrado
 *         silencioso. Diseñar el reverso real (que devolveria estos dos estados a la vida,
 *         mas anular el asiento, reabrir el periodo y devolver los VNPG afectados) es un
 *         frente aparte. Ver docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md §7.4.</p>
 */
public interface RhhEstadoOrdenPago {

	// Codigos alternos de los detalles del rubro (SCP.PDTR.PDTRALTR)
	public static final int GENERADA = 1;
	public static final int ENVIADA_AL_BANCO = 2;
	public static final int CONFIRMADA = 3;
	public static final int RECHAZADA_PARCIAL = 4;
	public static final int ANULADA = 5;

}
