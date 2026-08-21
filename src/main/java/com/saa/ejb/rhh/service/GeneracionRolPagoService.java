package com.saa.ejb.rhh.service;

import java.util.List;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Generacion del rol de pago: el documento que el empleado firma.</p>
 *
 * <p>Convive con <code>RolPagoService</code>, que es el CRUD de la tabla, igual que
 * <code>ProcesoNominaService</code> convive con <code>NominaService</code>. Aqui vive el
 * proceso; alli, el mantenimiento de la fila.</p>
 *
 * <h3>Cuando se genera</h3>
 *
 * <p>Al final de <code>aprobarPeriodo</code>, no antes. El rol es un documento que se
 * entrega, y no debe existir mientras el calculo todavia se puede recalcular: un rol
 * impreso a partir de un periodo en CALCULADO puede quedar desmentido por el siguiente
 * recalculo sin que nadie se entere.</p>
 *
 * <p><code>generarRoles</code> se expone ademas suelto por REST para regenerar mientras el
 * periodo no este CERRADO, que es el caso de una reapertura seguida de recalculo.</p>
 *
 * <h3>El hash</h3>
 *
 * <p><code>RLPGHASH</code> es un SHA-256 sobre el contenido del rol —empleado, periodo, cada
 * renglon con su concepto y su valor, y los tres totales— en un orden determinista. No es
 * una firma: no prueba quien lo emitio. Sirve para lo unico que hace falta aqui, que es
 * detectar que un rol impreso ya no corresponde a lo que hoy tiene la base.</p>
 */
@Local
public interface GeneracionRolPagoService {

	/**
	 * Genera un rol de pago por cada nomina del periodo.
	 *
	 * <p><b>Idempotente:</b> si la nomina ya tiene rol, lo actualiza en vez de duplicarlo.
	 * Al regenerar <b>no se tocan <code>RLPGFCEN</code> ni <code>RLPGRCBD</code></b>: la
	 * entrega al empleado es un hecho ocurrido y regenerar el documento no lo deshace, del
	 * mismo modo que <code>LQBSVLPG</code> no se toca al regenerar un beneficio.</p>
	 *
	 * <p>Exige que el periodo este APROBADO o mas adelante en el flujo, y menos que
	 * CERRADO.</p>
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param usuario			: Usuario que ejecuta
	 * @return					: Numero de roles generados o actualizados
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int generarRoles(Long idPeriodoNomina, String usuario) throws Throwable;

	/**
	 * Recalcula el hash del rol y lo compara con el que tiene grabado.
	 *
	 * <p>Un <code>false</code> significa que la nomina cambio despues de emitir el rol, o
	 * que alguien edito la fila a mano. Un rol sin hash devuelve <code>false</code>: no se
	 * puede afirmar que sea integro.</p>
	 *
	 * @param idRolPago		: Id del rol de pago
	 * @return				: true si el contenido sigue siendo el que se emitio
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	boolean verificarIntegridad(Long idRolPago) throws Throwable;

	/**
	 * Marca como entregados los roles indicados.
	 *
	 * <p>Recibe una lista porque la entrega se registra por tandas: el operador marca las
	 * firmas que recogio ese dia, no una por una. Pone <code>RLPGRCBD</code> en
	 * <code>'S'</code> y sella <code>RLPGFCEN</code> con la fecha del dia <b>solo si esta
	 * en nulo</b>, para no reescribir la fecha de una entrega ya registrada.</p>
	 *
	 * <p>Un id que no exista aborta la operacion entera: registrar la entrega de una tanda
	 * a medias dejaria al operador sin saber cuales quedaron marcados.</p>
	 *
	 * @param idsRolPago	: Ids de los roles entregados
	 * @param usuario		: Usuario que registra
	 * @return				: Numero de roles marcados
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int registrarRecepcion(List<Long> idsRolPago, String usuario) throws Throwable;

}
