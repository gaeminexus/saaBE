package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.CorridaAcreditacionVacaciones;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CorridaAcreditacionVacaciones (bitacora RHH.CRVC).
 *  Accede a los metodos DAO y procesa los datos para la bitacora de acreditacion.</p>
 */
@Local
public interface CorridaAcreditacionVacacionesService extends EntityService<CorridaAcreditacionVacaciones> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	CorridaAcreditacionVacaciones selectById(Long id) throws Throwable;

	/**
	 * Graba una fila de la bitacora en una transaccion PROPIA. Existe para que la fila de ERROR
	 * sobreviva al rollback de la acreditacion que fallo: si se grabara en la misma transaccion,
	 * se iria con ella y nadie sabria que la corrida se intento. Va en un bean aparte porque la
	 * llamada a un metodo de la misma clase no pasa por el contenedor y el REQUIRES_NEW no
	 * tendria efecto.
	 *
	 * @param corrida		: Fila a grabar
	 * @return				: La fila grabada
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	CorridaAcreditacionVacaciones registrarEnTransaccionAparte(CorridaAcreditacionVacaciones corrida)
			throws Throwable;
}
