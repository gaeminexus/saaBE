package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.ejb.cnt.service.EstadoPeriodoConsulta;
import com.saa.rubros.EstadoPeriodos;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementacion provisional de {@link EstadoPeriodoConsulta}: lee CNT.PRDO. CERRADO (4) = definitivo; ACTIVO,
 * MAYORIZADO y DESMAYORIZADO = preliminar.
 *
 * <p>INTEGRACION C17: cuando la rama cuadre/c17-cierres-bloqueantes llegue a main, reemplazar el cuerpo de
 * {@link #estadoPeriodo} por la llamada a su {@code estadoPeriodo(idEmpresa, anio, mes)} y traducir su respuesta
 * a {@link EstadoPeriodo} (cerrado = el periodo esta cerrado/bloqueado). No hace falta tocar nada mas.
 */
@Stateless
public class EstadoPeriodoConsultaImpl implements EstadoPeriodoConsulta {

	@PersistenceContext
	private EntityManager em;

	@Override
	@SuppressWarnings("unchecked")
	public EstadoPeriodo estadoPeriodo(Long idEmpresa, int anio, int mes) throws Throwable {
		Query q = em.createNativeQuery("SELECT PRDOESTD, PRDONMBR FROM CNT.PRDO WHERE PJRQCDGO = ?1 AND PRDOANNN = ?2 AND PRDOMSSS = ?3");
		q.setParameter(1, idEmpresa);
		q.setParameter(2, anio);
		q.setParameter(3, mes);
		List<Object[]> filas = q.getResultList();
		if (filas.isEmpty()) {
			return new EstadoPeriodo(false, false, null, "Sin periodo contable para " + mes + "/" + anio);
		}
		Integer estado = filas.get(0)[0] == null ? null : ((Number) filas.get(0)[0]).intValue();
		boolean cerrado = estado != null && estado == EstadoPeriodos.CERRADO;
		return new EstadoPeriodo(true, cerrado, estado, descripcion(estado));
	}

	private static String descripcion(Integer estado) {
		if (estado == null) {
			return "SIN ESTADO";
		}
		switch (estado) {
		case EstadoPeriodos.ACTIVO:
			return "ABIERTO";
		case EstadoPeriodos.MAYORIZADO:
			return "MAYORIZADO";
		case EstadoPeriodos.DESMAYORIZADO:
			return "DESMAYORIZADO";
		case EstadoPeriodos.CERRADO:
			return "CERRADO";
		default:
			return "ESTADO " + estado;
		}
	}
}
