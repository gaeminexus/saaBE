package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DetalleUtilidadDaoService;
import com.saa.model.rhh.DetalleUtilidad;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DetalleUtilidadDaoService.
 */
@Stateless
public class DetalleUtilidadDaoServiceImpl extends EntityDaoImpl<DetalleUtilidad>
		implements DetalleUtilidadDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleUtilidadDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleUtilidad");
		return new String[]{"codigo",
							"utilidad",
							"empleado",
							"dias",
							"numeroCargas",
							"valorPorDias",
							"valorPorCargas",
							"total",
							"excedente",
							"valorPagar",
							"retencionIr",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleUtilidadDaoService#selectByUtilidad(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleUtilidad> selectByUtilidad(Long idUtilidad) throws Throwable {
		System.out.println("Ingresa al metodo selectByUtilidad de DetalleUtilidad, reparto: " + idUtilidad);
		Query query = em.createQuery(" select   t "
				+ " from     DetalleUtilidad t "
				+ " where    t.utilidad.codigo = :idUtilidad "
				+ " order by t.empleado.apellidos, t.empleado.nombres, t.codigo ");
		query.setParameter("idUtilidad", idUtilidad);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleUtilidadDaoService#eliminaByUtilidad(java.lang.Long)
	 */
	@Override
	public int eliminaByUtilidad(Long idUtilidad) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByUtilidad de DetalleUtilidad, reparto: " + idUtilidad);
		Query query = em.createQuery(" delete from DetalleUtilidad t "
				+ " where  t.utilidad.codigo = :idUtilidad ");
		query.setParameter("idUtilidad", idUtilidad);
		return query.executeUpdate();
	}
}
