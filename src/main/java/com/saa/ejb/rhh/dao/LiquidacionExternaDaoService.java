package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.LiquidacionExterna;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService LiquidacionExterna.
 */
@Local
public interface LiquidacionExternaDaoService extends EntityDao<LiquidacionExterna> {

	/**
	 * Recupera las liquidaciones NO anuladas de una identificacion, excluyendo
	 * opcionalmente una liquidacion (para no chocar consigo misma al actualizar).
	 *
	 * <p>Usada por la validacion de <code>registrar</code>/<code>actualizar</code>:
	 * "Ya existe la liquidacion N.º &lt;codigo&gt; para esta identificacion" (§6).</p>
	 *
	 * @param identificacion	: Identificacion a buscar, ya trimeada
	 * @param excluirCodigo		: Codigo de liquidacion a excluir, o null para no excluir ninguna
	 * @return					: Liquidaciones vigentes (estado distinto de ANULADA) con esa identificacion
	 * @throws Throwable		: Excepcion
	 */
	List<LiquidacionExterna> selectVigentesByIdentificacion(String identificacion, Long excluirCodigo)
			throws Throwable;

	/**
	 * Recupera las liquidaciones en estado EN_TESORERIA, para sincronizarlas contra el
	 * pago antes de devolver un listado (getAll/selectByCriteria, §6).
	 *
	 * @return				: Liquidaciones EN_TESORERIA
	 * @throws Throwable	: Excepcion
	 */
	List<LiquidacionExterna> selectEnTesoreria() throws Throwable;

	/**
	 * Recupera las liquidaciones PAGADA cuya fecha de pago cae en el anio indicado, para
	 * el RDEP (§8).
	 *
	 * @param anio			: Anio a filtrar, sobre LQEXFCPG
	 * @return				: Liquidaciones PAGADA de ese anio
	 * @throws Throwable	: Excepcion
	 */
	List<LiquidacionExterna> selectPagadasByAnio(Integer anio) throws Throwable;

}
