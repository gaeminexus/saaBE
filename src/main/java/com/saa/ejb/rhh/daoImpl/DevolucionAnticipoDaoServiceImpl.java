package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DevolucionAnticipoDaoService;
import com.saa.model.rhh.DevolucionAnticipo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DevolucionAnticipoDaoService.
 */
@Stateless
public class DevolucionAnticipoDaoServiceImpl extends EntityDaoImpl<DevolucionAnticipo>
		implements DevolucionAnticipoDaoService {

	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DevolucionAnticipoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DevolucionAnticipo");
		return new String[]{"codigo",
							"anticipo",
							"fecha",
							"valor",
							"cuentaBancaria",
							"referencia",
							"observacion",
							"idIngreso",
							"asiento",
							"estado",
							"motivoAnulacion",
							"fechaRegistro",
							"usuario"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DevolucionAnticipoDaoService#selectByAnticipo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DevolucionAnticipo> selectByAnticipo(Long idAnticipo) throws Throwable {
		System.out.println("Ingresa al metodo selectByAnticipo de DevolucionAnticipo, anticipo: " + idAnticipo);
		Query query = em.createQuery(" select   d "
				+ " from     DevolucionAnticipo d "
				+ " where    d.anticipo.codigo = :idAnticipo "
				+ " order by d.fecha desc, d.codigo desc ");
		query.setParameter("idAnticipo", idAnticipo);
		return query.getResultList();
	}

}
