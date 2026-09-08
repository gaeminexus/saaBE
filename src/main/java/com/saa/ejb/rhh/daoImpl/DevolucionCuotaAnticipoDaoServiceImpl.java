package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DevolucionCuotaAnticipoDaoService;
import com.saa.model.rhh.DevolucionCuotaAnticipo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DevolucionCuotaAnticipoDaoService.
 */
@Stateless
public class DevolucionCuotaAnticipoDaoServiceImpl extends EntityDaoImpl<DevolucionCuotaAnticipo>
		implements DevolucionCuotaAnticipoDaoService {

	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DevolucionCuotaAnticipoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DevolucionCuotaAnticipo");
		return new String[]{"codigo", "devolucion", "cuota", "tipo", "valorAplicado", "valorAnterior"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DevolucionCuotaAnticipoDaoService#selectByDevolucion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DevolucionCuotaAnticipo> selectByDevolucion(Long idDevolucion) throws Throwable {
		System.out.println("Ingresa al metodo selectByDevolucion de DevolucionCuotaAnticipo, devolucion: "
				+ idDevolucion);
		Query query = em.createQuery(" select   d "
				+ " from     DevolucionCuotaAnticipo d "
				+ " where    d.devolucion.codigo = :idDevolucion "
				+ " order by d.codigo ");
		query.setParameter("idDevolucion", idDevolucion);
		return query.getResultList();
	}

}
