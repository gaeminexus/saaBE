package com.saa.ejb.cxc.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.EstablecimientoDaoService;
import com.saa.model.cxc.Establecimiento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para la entidad Establecimiento
 */
@Stateless
public class EstablecimientoDaoServiceImpl extends EntityDaoImpl<Establecimiento> implements EstablecimientoDaoService {

	@PersistenceContext
	EntityManager em;
	
	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id", "facturador", "codigo", "nombre", "descripcion", "direccion",
			"telefono", "mail", "logo", "creacion", "matriz", "estado"
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Establecimiento> selectByFacturador(Long idFacturador) throws Throwable {
		System.out.println("EstablecimientoDaoServiceImpl.selectByFacturador - idFacturador: " + idFacturador);
		Query query = em.createQuery(
			"SELECT e FROM Establecimiento e WHERE e.facturador.id = :idFacturador ORDER BY e.codigo"
		);
		query.setParameter("idFacturador", idFacturador);
		return query.getResultList();
	}

	@Override
	public Establecimiento selectMatriz(Long idFacturador) throws Throwable {
		System.out.println("EstablecimientoDaoServiceImpl.selectMatriz - idFacturador: " + idFacturador);
		try {
			Query query = em.createQuery(
				"SELECT e FROM Establecimiento e WHERE e.facturador.id = :idFacturador AND e.matriz = 1"
			);
			query.setParameter("idFacturador", idFacturador);
			return (Establecimiento) query.getSingleResult();
		} catch (Exception e) {
			System.err.println("Error al buscar establecimiento matriz: " + e.getMessage());
			return null;
		}
	}
}
