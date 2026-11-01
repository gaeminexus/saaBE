package com.saa.basico.ejbImpl;

import java.util.List;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.UsuarioInvalidoException;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación de {@link UsuarioValidacionService}. Consulta de solo lectura por nombre (la misma clave que
 * {@code UsuarioDaoService#selectByNombre}), sin lanzar {@code NoResultException} desde dentro del EJB.
 */
@Stateless
public class UsuarioValidacionServiceImpl implements UsuarioValidacionService {

	@PersistenceContext
	private EntityManager em;

	@Override
	@SuppressWarnings("unchecked")
	public String validar(String nombre) throws UsuarioInvalidoException {
		if (nombre == null || nombre.trim().isEmpty()) {
			throw new UsuarioInvalidoException("falta el usuario");
		}
		String limpio = nombre.trim();
		Query q = em.createQuery("select u.codigo from Usuario u where u.nombre = :nombre");
		q.setParameter("nombre", limpio);
		q.setMaxResults(1);
		List<Long> r = q.getResultList();
		if (r.isEmpty()) {
			throw new UsuarioInvalidoException("el usuario '" + limpio + "' no existe en el sistema");
		}
		return limpio;
	}
}
