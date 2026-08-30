package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.model.crd.PagoAporte;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class PagoAporteDaoServiceImpl extends EntityDaoImpl<PagoAporte> implements PagoAporteDaoService{

	// Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	@SuppressWarnings("unchecked")
	@Override
	public List<PagoAporte> selectByPagoPrestamo(Long codigoPagoPrestamo) throws Throwable {
		System.out.println("PagoAporteDaoService.selectByPagoPrestamo - PagoPrestamo: " + codigoPagoPrestamo);

		try {
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoAporte p " +
				"WHERE p.pagoPrestamo.codigo = :codigoPagoPrestamo " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("codigoPagoPrestamo", codigoPagoPrestamo);

			List<PagoAporte> resultados = query.getResultList();
			System.out.println("  Pagos de aporte encontrados: " + resultados.size());
			return resultados;

		} catch (Exception e) {
			System.err.println("ERROR en selectByPagoPrestamo: " + e.getMessage());
			e.printStackTrace();
			// Retornar lista vacía en lugar de lanzar excepción
			return new ArrayList<>();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PagoAporte> selectByAporte(Long idAporte) throws Throwable {
		System.out.println("PagoAporteDaoService.selectByAporte - Aporte: " + idAporte);
		Query query = em.createQuery(
				" select p from PagoAporte p " +
				" where  p.aporte.codigo = :idAporte " +
				" order by p.codigo asc");
		query.setParameter("idAporte", idAporte);
		return query.getResultList();
	}

}
