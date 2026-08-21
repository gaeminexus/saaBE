package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DetalleFormatoMarcacionDaoService;
import com.saa.model.rhh.DetalleFormatoMarcacion;

import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DetalleFormatoMarcacionDaoService.
 */
@Stateless
public class DetalleFormatoMarcacionDaoServiceImpl extends EntityDaoImpl<DetalleFormatoMarcacion> implements DetalleFormatoMarcacionDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleFormatoMarcacionDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleFormatoMarcacion");
		return new String[]{"codigo",
							"formato",
							"campo",
							"orden",
							"posicion",
							"indiceInicio",
							"longitud",
							"mapeo",
							"obligatorio",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleFormatoMarcacionDaoService#selectByFormato(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleFormatoMarcacion> selectByFormato(Long idFormato) throws Throwable {
		System.out.println("Ingresa al metodo selectByFormato de DetalleFormatoMarcacion, formato: "
				+ idFormato);
		Query query = em.createQuery(" select   t "
				+ " from     DetalleFormatoMarcacion t "
				+ " where    t.formato.codigo = :idFormato "
				+ "          and (t.estado is null or t.estado = :activo) "
				+ " order by t.orden, t.codigo ");
		query.setParameter("idFormato", idFormato);
		query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}
}
