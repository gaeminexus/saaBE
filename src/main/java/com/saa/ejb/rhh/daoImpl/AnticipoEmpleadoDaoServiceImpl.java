package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.AnticipoEmpleadoDaoService;
import com.saa.model.rhh.AnticipoEmpleado;
import com.saa.rubros.EstadoAnticipoEmpleado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion AnticipoEmpleadoDaoService.
 */
@SuppressWarnings("unchecked")
@Stateless
public class AnticipoEmpleadoDaoServiceImpl extends EntityDaoImpl<AnticipoEmpleado>
		implements AnticipoEmpleadoDaoService {

	@PersistenceContext
	private EntityManager em;

	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"codigo",
			"empleado",
			"fecha",
			"valor",
			"numeroCuotas",
			"valorCuota",
			"saldo",
			"fechaInicioDescuento",
			"motivo",
			"observacion",
			"estado",
			"pagoProgramado",
			"descuentoRecurrente",
			"usuarioAprueba",
			"fechaAprobacion",
			"motivoAnulacion",
			"fechaRegistro",
			"usuarioRegistro"
		};
	}

	@Override
	public AnticipoEmpleado selectVigenteByEmpleado(Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo selectVigenteByEmpleado con idEmpleado: " + idEmpleado);
		Query query = em.createQuery(
				" select a from AnticipoEmpleado a " +
				" where  a.empleado.codigo = :idEmpleado " +
				"        and a.estado in (:solicitado, :aprobado, :pagado, :enDescuento) " +
				" order by a.codigo desc ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("solicitado", Long.valueOf(EstadoAnticipoEmpleado.SOLICITADO));
		query.setParameter("aprobado", Long.valueOf(EstadoAnticipoEmpleado.APROBADO));
		query.setParameter("pagado", Long.valueOf(EstadoAnticipoEmpleado.PAGADO));
		query.setParameter("enDescuento", Long.valueOf(EstadoAnticipoEmpleado.EN_DESCUENTO));
		query.setMaxResults(1);
		List<AnticipoEmpleado> resultado = query.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}

	@Override
	public List<AnticipoEmpleado> selectListado(Long idEmpresa, Long idEmpleado, Long estado) throws Throwable {
		System.out.println("Ingresa al metodo selectListado de AnticipoEmpleado | idEmpresa: " + idEmpresa
				+ " | idEmpleado: " + idEmpleado + " | estado: " + estado);
		StringBuilder jpql = new StringBuilder(
				" select a from AnticipoEmpleado a " +
				" where  1 = 1 ");
		if (idEmpresa != null) {
			jpql.append(" and a.empleado.empresa.codigo = :idEmpresa ");
		}
		if (idEmpleado != null) {
			jpql.append(" and a.empleado.codigo = :idEmpleado ");
		}
		if (estado != null) {
			jpql.append(" and a.estado = :estado ");
		}
		jpql.append(" order by a.codigo desc ");

		Query query = em.createQuery(jpql.toString());
		if (idEmpresa != null) {
			query.setParameter("idEmpresa", idEmpresa);
		}
		if (idEmpleado != null) {
			query.setParameter("idEmpleado", idEmpleado);
		}
		if (estado != null) {
			query.setParameter("estado", estado);
		}
		return query.getResultList();
	}

	@Override
	public AnticipoEmpleado selectByDescuentoRecurrente(Long idDescuentoRecurrente) throws Throwable {
		if (idDescuentoRecurrente == null) {
			return null;
		}
		Query query = em.createQuery(
				" select a from AnticipoEmpleado a where a.descuentoRecurrente.codigo = :id ");
		query.setParameter("id", idDescuentoRecurrente);
		query.setMaxResults(1);
		List<AnticipoEmpleado> resultado = query.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}

}
