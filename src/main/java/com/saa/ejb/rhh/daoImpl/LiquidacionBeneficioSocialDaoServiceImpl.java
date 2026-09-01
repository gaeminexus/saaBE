package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService;
import com.saa.model.rhh.LiquidacionBeneficioSocial;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion LiquidacionBeneficioSocialDaoService.
 */
@Stateless
public class LiquidacionBeneficioSocialDaoServiceImpl extends EntityDaoImpl<LiquidacionBeneficioSocial> implements LiquidacionBeneficioSocialDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) LiquidacionBeneficioSocial");
		return new String[]{"codigo",
							"empleado",
							"tipoBeneficio",
							"anio",
							"fechaInicio",
							"fechaFin",
							"baseCalculo",
							"dias",
							"valor",
							"valorMensualizado",
							"valorPagado",
							"periodoNomina",
							"fechaPago",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService#selectByEmpleadoTipoAnio(java.lang.Long, java.lang.Long, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public LiquidacionBeneficioSocial selectByEmpleadoTipoAnio(Long idEmpleado, Long tipoBeneficio,
			Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpleadoTipoAnio de LiquidacionBeneficioSocial, empleado: "
				+ idEmpleado + ", tipo: " + tipoBeneficio + ", anio: " + anio);
		Query query = em.createQuery(" select   t "
				+ " from     LiquidacionBeneficioSocial t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.tipoBeneficio = :tipoBeneficio "
				+ "          and t.anio = :anio ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("tipoBeneficio", tipoBeneficio);
		query.setParameter("anio", anio);
		List<LiquidacionBeneficioSocial> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService#selectPendientesByCombinacion(java.lang.Long, java.lang.Long, java.lang.Integer, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<LiquidacionBeneficioSocial> selectPendientesByCombinacion(Long idEmpresa,
			Long tipoBeneficio, Integer anio, Long region) throws Throwable {
		System.out.println("Ingresa al metodo selectPendientesByCombinacion de LiquidacionBeneficioSocial,"
				+ " empresa: " + idEmpresa + ", tipo: " + tipoBeneficio + ", anio: " + anio
				+ ", region: " + region);
		// region no vive en LQBS: se filtra por la region del empleado, igual que hace
		// generarDecimoCuarto (BeneficioSocialServiceImpl) al calcular. Solo aplica cuando
		// el llamador la manda (decimo cuarto); para los demas tipos region llega null y no
		// se filtra por ella.
		StringBuilder jpql = new StringBuilder(
				" select   t "
				+ " from     LiquidacionBeneficioSocial t "
				+ " where    t.empleado.empresa.codigo = :idEmpresa "
				+ "          and t.tipoBeneficio = :tipoBeneficio "
				+ "          and t.anio = :anio "
				+ "          and t.ordenBeneficioSocial is null "
				+ "          and t.estado = 1 ");
		if (region != null) {
			jpql.append(" and t.empleado.region = :region ");
		}
		jpql.append(" order by t.empleado.apellidos, t.empleado.nombres ");

		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("tipoBeneficio", tipoBeneficio);
		query.setParameter("anio", anio);
		if (region != null) {
			query.setParameter("region", region);
		}
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService#selectByOrden(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<LiquidacionBeneficioSocial> selectByOrden(Long idOrden) throws Throwable {
		System.out.println("Ingresa al metodo selectByOrden de LiquidacionBeneficioSocial, orden: " + idOrden);
		Query query = em.createQuery(" select   t "
				+ " from     LiquidacionBeneficioSocial t "
				+ " where    t.ordenBeneficioSocial.codigo = :idOrden "
				+ " order by t.empleado.apellidos, t.empleado.nombres ");
		query.setParameter("idOrden", idOrden);
		return query.getResultList();
	}
}
