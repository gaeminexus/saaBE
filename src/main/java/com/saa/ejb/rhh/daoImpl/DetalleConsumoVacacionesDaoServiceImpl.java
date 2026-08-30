package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DetalleConsumoVacacionesDaoService;
import com.saa.model.rhh.DetalleConsumoVacaciones;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DetalleConsumoVacacionesDaoService.
 */
@Stateless
public class DetalleConsumoVacacionesDaoServiceImpl extends EntityDaoImpl<DetalleConsumoVacaciones>
        implements DetalleConsumoVacacionesDaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleConsumoVacaciones");
		return new String[]{"codigo", "solicitud", "saldo", "dias", "estado", "fechaRegistro",
				"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleConsumoVacacionesDaoService#selectVigentesPorSolicitud(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleConsumoVacaciones> selectVigentesPorSolicitud(Long idSolicitud) throws Throwable {
		Query query = em.createQuery(" select   t "
				+ " from     DetalleConsumoVacaciones t "
				+ " where    t.solicitud.codigo = :idSolicitud "
				+ "          and t.estado = 1 "
				+ " order by t.saldo.anio ");
		query.setParameter("idSolicitud", idSolicitud);
		return query.getResultList();
	}

}
