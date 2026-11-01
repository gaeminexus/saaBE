package com.saa.basico.ejbImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;

import com.saa.basico.ejb.ContextoPersistenciaService;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @see ContextoPersistenciaService
 */
@Stateless
public class ContextoPersistenciaServiceImpl implements ContextoPersistenciaService {

	@PersistenceContext
	EntityManager em;

	private Session sesion() {
		return em.unwrap(Session.class);
	}

	@Override
	public int tamanio() {
		return sesion().getStatistics().getEntityCount();
	}

	@Override
	public String histograma(int top) {
		Map<String, Integer> porClase = new HashMap<>();
		SessionImplementor sesion = em.unwrap(SessionImplementor.class);
		for (Map.Entry<Object, EntityEntry> entrada : sesion.getPersistenceContextInternal().reentrantSafeEntityEntries()) {
			porClase.merge(Hibernate.getClass(entrada.getKey()).getSimpleName(), 1, Integer::sum);
		}
		List<Map.Entry<String, Integer>> orden = new ArrayList<>(porClase.entrySet());
		orden.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		StringBuilder texto = new StringBuilder();
		for (int i = 0; i < orden.size() && i < top; i++) {
			if (i > 0) {
				texto.append(", ");
			}
			texto.append(orden.get(i).getKey()).append('=').append(orden.get(i).getValue());
		}
		return texto.toString();
	}

	@Override
	public int volcarYDesprender(Collection<Class<?>> clases) {
		em.flush();
		Set<Class<?>> objetivo = new HashSet<>(clases);
		SessionImplementor sesion = em.unwrap(SessionImplementor.class);
		// reentrantSafeEntityEntries devuelve una copia: se puede desprender mientras se recorre.
		int desprendidas = 0;
		for (Map.Entry<Object, EntityEntry> entrada : sesion.getPersistenceContextInternal().reentrantSafeEntityEntries()) {
			Object entidad = entrada.getKey();
			if (objetivo.contains(Hibernate.getClass(entidad))) {
				em.detach(entidad);
				desprendidas++;
			}
		}
		return desprendidas;
	}

	@Override
	public int volcarYDesprenderSiExcede(int umbral, Collection<Class<?>> clases) {
		if (tamanio() <= umbral) {
			return -1;
		}
		return volcarYDesprender(clases);
	}

	@Override
	public FlushMode suspenderAutoFlush() {
		Session sesion = sesion();
		FlushMode anterior = sesion.getHibernateFlushMode();
		em.flush();
		sesion.setHibernateFlushMode(FlushMode.MANUAL);
		return anterior;
	}

	@Override
	public boolean restaurarAutoFlush(FlushMode anterior, String fase) {
		Session sesion = sesion();
		sesion.setHibernateFlushMode(anterior != null ? anterior : FlushMode.AUTO);
		if (sesion.isDirty()) {
			System.err.println("ADVERTENCIA [contexto de persistencia]: la fase de solo lectura '" + fase
				+ "' dejó cambios pendientes; se vuelcan ahora. Revisar el supuesto de solo lectura"
				+ " (una consulta de esa fase pudo no ver esa escritura).");
			em.flush();
			return true;
		}
		return false;
	}
}
