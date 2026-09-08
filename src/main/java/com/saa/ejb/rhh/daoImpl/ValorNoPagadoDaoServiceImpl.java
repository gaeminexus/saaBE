package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ValorNoPagadoDaoService;
import com.saa.model.rhh.ValorNoPagado;
import com.saa.rubros.RhhEstadoValorNoPagado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ValorNoPagadoDaoService.
 */
@Stateless
public class ValorNoPagadoDaoServiceImpl extends EntityDaoImpl<ValorNoPagado>
		implements ValorNoPagadoDaoService {

	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ValorNoPagadoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ValorNoPagado");
		return new String[]{"codigo",
							"empresa",
							"empleado",
							"periodoNomina",
							"valor",
							"motivo",
							"estado",
							"periodoRecuperacion",
							"ordenRetencion",
							"ordenPago",
							"liquidacion",
							"fechaRegistro",
							"usuarioRegistro",
							"motivoAnulacion",
							"fechaAnulacion",
							"usuarioAnulacion"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ValorNoPagadoDaoService#selectVivoByEmpleadoPeriodo(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ValorNoPagado selectVivoByEmpleadoPeriodo(Long idEmpleado, Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectVivoByEmpleadoPeriodo de ValorNoPagado, empleado: "
				+ idEmpleado + ", periodo: " + idPeriodo);
		Query query = em.createQuery(
				" select   t "
				+ " from     ValorNoPagado t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.periodoNomina.codigo = :idPeriodo "
				+ "          and t.estado in (:registrado, :retenido) ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("registrado", Long.valueOf(RhhEstadoValorNoPagado.REGISTRADO));
		query.setParameter("retenido", Long.valueOf(RhhEstadoValorNoPagado.RETENIDO));
		List<ValorNoPagado> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ValorNoPagadoDaoService#selectRetenidosByEmpleado(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ValorNoPagado> selectRetenidosByEmpleado(Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo selectRetenidosByEmpleado de ValorNoPagado, empleado: " + idEmpleado);
		Query query = em.createQuery(
				" select   t "
				+ " from     ValorNoPagado t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.estado = :retenido "
				+ " order by t.periodoNomina.anio, t.periodoNomina.mes ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("retenido", Long.valueOf(RhhEstadoValorNoPagado.RETENIDO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ValorNoPagadoDaoService#selectListado(java.lang.Long, java.lang.Long, java.lang.Long, java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ValorNoPagado> selectListado(Long idEmpresa, Long idPeriodo, Long idEmpleado,
			List<Long> estados) throws Throwable {
		System.out.println("Ingresa al metodo selectListado de ValorNoPagado, empresa: " + idEmpresa
				+ ", periodo: " + idPeriodo + ", empleado: " + idEmpleado + ", estados: " + estados);

		StringBuilder jpql = new StringBuilder(
				" select   t "
				+ " from     ValorNoPagado t "
				+ " where    t.empresa.codigo = :idEmpresa ");
		if (idPeriodo != null) {
			jpql.append(" and t.periodoNomina.codigo = :idPeriodo ");
		}
		if (idEmpleado != null) {
			jpql.append(" and t.empleado.codigo = :idEmpleado ");
		}
		if (estados != null && !estados.isEmpty()) {
			jpql.append(" and t.estado in (:estados) ");
		}
		jpql.append(" order by t.fechaRegistro desc, t.codigo desc ");

		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		if (idPeriodo != null) {
			query.setParameter("idPeriodo", idPeriodo);
		}
		if (idEmpleado != null) {
			query.setParameter("idEmpleado", idEmpleado);
		}
		if (estados != null && !estados.isEmpty()) {
			query.setParameter("estados", estados);
		}
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ValorNoPagadoDaoService#countByOrden(java.lang.Long)
	 */
	@Override
	public long countByOrden(Long idOrdenPago) throws Throwable {
		System.out.println("Ingresa al metodo countByOrden de ValorNoPagado, orden: " + idOrdenPago);
		Query query = em.createQuery(
				" select count(t) "
				+ " from   ValorNoPagado t "
				+ " where  t.ordenRetencion.codigo = :idOrdenPago "
				+ "        or t.ordenPago.codigo = :idOrdenPago ");
		query.setParameter("idOrdenPago", idOrdenPago);
		Object resultado = query.getSingleResult();
		return resultado != null ? ((Number) resultado).longValue() : 0L;
	}

}
