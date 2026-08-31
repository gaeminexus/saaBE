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

	// ========================================================================
	// SERVICIOS DE PAGO DE PRÉSTAMOS (§5.2 ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md)
	// ========================================================================

	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectVigentesByIdDetallePrestamo(Long codigoDetallePrestamo) {
		System.out.println("PagoPrestamoDaoService.selectVigentesByIdDetallePrestamo - DetallePrestamo: " + codigoDetallePrestamo);

		try {
			// anulado IS NULL cubre los pagos históricos anteriores al ALTER de CRD.PGPR
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoPrestamo p " +
				"WHERE p.detallePrestamo.codigo = :codigoDetallePrestamo " +
				"AND (p.anulado IS NULL OR p.anulado = 0) " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("codigoDetallePrestamo", codigoDetallePrestamo);

			List<PagoPrestamo> resultados = query.getResultList();
			System.out.println("  Pagos vigentes encontrados: " + resultados.size());

			return resultados;

		} catch (Exception e) {
			System.err.println("ERROR en selectVigentesByIdDetallePrestamo: " + e.getMessage());
			e.printStackTrace();
			// Retornar lista vacía en lugar de lanzar excepción
			return new ArrayList<>();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectByEvento(Long codigoEvento) {
		System.out.println("PagoPrestamoDaoService.selectByEvento - Evento: " + codigoEvento);

		try {
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoPrestamo p " +
				"WHERE p.eventoPrestamo.codigo = :codigoEvento " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("codigoEvento", codigoEvento);

			List<PagoPrestamo> resultados = query.getResultList();
			System.out.println("  Pagos del evento encontrados: " + resultados.size());

			return resultados;

		} catch (Exception e) {
			System.err.println("ERROR en selectByEvento: " + e.getMessage());
			e.printStackTrace();
			// Retornar lista vacía en lugar de lanzar excepción
			return new ArrayList<>();
		}
	}

	@Override
	public Long contarVigentesByIdDetallePrestamo(Long codigoDetallePrestamo) {
		System.out.println("PagoPrestamoDaoService.contarVigentesByIdDetallePrestamo - DetallePrestamo: " + codigoDetallePrestamo);

		try {
			Query query = em.createQuery(
				"SELECT COUNT(p) " +
				"FROM PagoPrestamo p " +
				"WHERE p.detallePrestamo.codigo = :codigoDetallePrestamo " +
				"AND (p.anulado IS NULL OR p.anulado = 0)"
			);
			query.setParameter("codigoDetallePrestamo", codigoDetallePrestamo);

			Long total = (Long) query.getSingleResult();
			System.out.println("  Pagos vigentes: " + total);
			return total;

		} catch (Exception e) {
			System.err.println("ERROR en contarVigentesByIdDetallePrestamo: " + e.getMessage());
			e.printStackTrace();
			// Retornar 0 en lugar de lanzar excepción
			return 0L;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectVigentesByCargaArchivo(Long idCarga) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectVigentesByCargaArchivo - Carga: " + idCarga);

		try {
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoPrestamo p " +
				"WHERE p.cargaArchivo.codigo = :idCarga " +
				"AND (p.anulado IS NULL OR p.anulado = 0) " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("idCarga", idCarga);

			List<PagoPrestamo> resultados = query.getResultList();
			System.out.println("  Pagos vigentes de la carga: " + resultados.size());
			return resultados;

		} catch (Exception e) {
			System.err.println("ERROR en selectVigentesByCargaArchivo: " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	@Override
	public long countByIdDetallePrestamo(Long codigoDetallePrestamo) throws Throwable {
		System.out.println("PagoPrestamoDaoService.countByIdDetallePrestamo - DetallePrestamo: " + codigoDetallePrestamo);

		// Sin try/catch: a propósito. Ver el javadoc en la interfaz — es una guarda de
		// borrado, y una guarda que ante un error de consulta dice "0 pagos" deja borrar
		// cuotas que en realidad no se pudieron verificar.
		Query query = em.createQuery(
			"SELECT COUNT(p) " +
			"FROM PagoPrestamo p " +
			"WHERE p.detallePrestamo.codigo = :codigoDetallePrestamo"
		);
		query.setParameter("codigoDetallePrestamo", codigoDetallePrestamo);

		long total = (Long) query.getSingleResult();
		System.out.println("  Pagos totales (incluye anulados): " + total);
		return total;
	}

}
