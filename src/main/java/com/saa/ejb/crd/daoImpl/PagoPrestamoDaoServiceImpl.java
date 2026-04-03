package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class PagoPrestamoDaoServiceImpl extends EntityDaoImpl<PagoPrestamo> implements PagoPrestamoDaoService {

	// Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/**
	 * Busca todos los pagos asociados a un DetallePrestamo específico
	 * 
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Lista de PagoPrestamo encontrados (vacía si hay error o no se encuentran)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectByIdDetallePrestamo(Long codigoDetallePrestamo) {
		System.out.println("PagoPrestamoDaoService.selectByIdDetallePrestamo - DetallePrestamo: " + codigoDetallePrestamo);
		
		try {
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoPrestamo p " +
				"WHERE p.detallePrestamo.codigo = :codigoDetallePrestamo " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("codigoDetallePrestamo", codigoDetallePrestamo);
			
			List<PagoPrestamo> resultados = query.getResultList();
			System.out.println("  Pagos encontrados: " + resultados.size());
			
			return resultados;
			
		} catch (Exception e) {
			System.err.println("ERROR en selectByIdDetallePrestamo: " + e.getMessage());
			e.printStackTrace();
			// Retornar lista vacía en lugar de lanzar excepción
			return new ArrayList<>();
		}
	}

}
