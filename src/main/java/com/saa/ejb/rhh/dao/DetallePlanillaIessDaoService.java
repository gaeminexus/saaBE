package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DetallePlanillaIess;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DetallePlanillaIess. Sin capa de Service ni de REST propia: se
 * accede siempre a traves de PlanillaIessService, igual que DetalleTransito.
 */
@Local
public interface DetallePlanillaIessDaoService extends EntityDao<DetallePlanillaIess> {

	/**
	 * Renglones de una planilla, en el orden en que se capturaron.
	 *
	 * @param idPlanilla	: Id de la planilla
	 * @return				: Los renglones de la planilla, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<DetallePlanillaIess> selectByPlanilla(Long idPlanilla) throws Throwable;

}
