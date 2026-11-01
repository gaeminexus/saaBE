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

	@Override
	public String validar(com.saa.model.scp.Usuario usuario) throws UsuarioInvalidoException {
		if (usuario == null) {
			throw new UsuarioInvalidoException("falta el usuario");
		}
		if (usuario.getNombre() != null && !usuario.getNombre().trim().isEmpty()) {
			return validar(usuario.getNombre());
		}
		return validarPorId(usuario.getCodigo());
	}

	@Override
	@SuppressWarnings("unchecked")
	public String validarPorId(Long idUsuario) throws UsuarioInvalidoException {
		if (idUsuario == null) {
			throw new UsuarioInvalidoException("falta el usuario");
		}
		Query q = em.createQuery("select u.nombre from Usuario u where u.codigo = :codigo");
		q.setParameter("codigo", idUsuario);
		q.setMaxResults(1);
		List<String> r = q.getResultList();
		if (r.isEmpty() || r.get(0) == null || r.get(0).trim().isEmpty()) {
			throw new UsuarioInvalidoException("el usuario con código " + idUsuario + " no existe en el sistema");
		}
		return r.get(0).trim();
	}
}
