package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DetalleLiquidacionExternaDaoService;
import com.saa.model.rhh.DetalleLiquidacionExterna;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DetalleLiquidacionExternaDaoService.
 */
@Stateless
public class DetalleLiquidacionExternaDaoServiceImpl extends EntityDaoImpl<DetalleLiquidacionExterna>
		implements DetalleLiquidacionExternaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleLiquidacionExternaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleLiquidacionExterna");
		return new String[]{"codigo",
							"liquidacion",
							"tipoConcepto",
							"descripcion",
							"valor",
							"orden",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleLiquidacionExternaDaoService#selectByLiquidacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleLiquidacionExterna> selectByLiquidacion(Long idLiquidacion) throws Throwable {
		System.out.println("Ingresa al metodo selectByLiquidacion de DetalleLiquidacionExterna, liquidacion: "
				+ idLiquidacion);
		Query query = em.createQuery(" select   t "
				+ " from     DetalleLiquidacionExterna t "
				+ " where    t.liquidacion.codigo = :idLiquidacion "
				+ " order by t.orden, t.codigo ");
		query.setParameter("idLiquidacion", idLiquidacion);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleLiquidacionExternaDaoService#eliminaByLiquidacion(java.lang.Long)
	 */
	@Override
	public int eliminaByLiquidacion(Long idLiquidacion) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByLiquidacion de DetalleLiquidacionExterna, liquidacion: "
				+ idLiquidacion);
		Query query = em.createQuery(" delete from DetalleLiquidacionExterna t "
				+ " where  t.liquidacion.codigo = :idLiquidacion ");
		query.setParameter("idLiquidacion", idLiquidacion);
		return query.executeUpdate();
	}
}
