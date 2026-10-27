package com.saa.ejb.rhh.service;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Acreditacion automatica de vacaciones al cerrar un periodo de nomina.</p>
 *
 * <p>Va en su propio bean, y no dentro de <code>AcreditacionVacacionesService</code>, por la
 * misma razon que el registro de la bitacora: la acreditacion tiene que entrar por el
 * contenedor para abrir SU transaccion, y una llamada dentro de la misma clase no pasa por el.</p>
 */
@Local
public interface CierreNominaVacacionesService {

	/**
	 * Acredita las vacaciones con corte al ultimo dia del mes del periodo que se acaba de
	 * cerrar, con origen CIERRE_NOMINA. <b>Nunca lanza</b>: el cierre ya esta confirmado y una
	 * falla aqui no puede deshacerlo; el error queda en la bitacora RHH.CRVC y se devuelve como
	 * aviso.
	 *
	 * <p>Sin transaccion propia (NOT_SUPPORTED): suspende la que traiga quien llama y deja que
	 * <code>acreditar</code> abra la suya, de modo que una falla no contamine a nadie.</p>
	 *
	 * <p>No acredita, y lo asienta como OMITIDA, si el anio esta en modalidad por aniversario o
	 * si ya hay una corrida OK del mismo anio con corte posterior. Un periodo que no es MENSUAL
	 * se ignora sin dejar fila.</p>
	 *
	 * @param idPeriodoNomina	: Id del periodo recien cerrado
	 * @param usuario			: Usuario que cerro el periodo
	 * @return					: Aviso para mostrar al usuario si la acreditacion fallo; null si
	 *                            salio bien o si no correspondia acreditar
	 */
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	String acreditarAlCerrarPeriodo(Long idPeriodoNomina, String usuario);
}
