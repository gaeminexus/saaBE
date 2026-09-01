package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.OrdenBeneficioSocialDaoService;
import com.saa.model.rhh.OrdenBeneficioSocial;
import com.saa.model.rhh.OrdenBeneficioSocialResumen;
import com.saa.rubros.RhhEstadoOrdenBeneficio;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion OrdenBeneficioSocialDaoService.
 */
@Stateless
public class OrdenBeneficioSocialDaoServiceImpl extends EntityDaoImpl<OrdenBeneficioSocial>
		implements OrdenBeneficioSocialDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.OrdenBeneficioSocialDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) OrdenBeneficioSocial");
		return new String[]{"codigo",
							"empresa",
							"tipoBeneficio",
							"anio",
							"region",
							"numero",
							"fechaEmision",
							"fechaPago",
							"total",
							"numeroEmpleados",
							"pagoProgramado",
							"asiento",
							"estado",
							"observaciones",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.OrdenBeneficioSocialDaoService#selectOrdenVivaByCombinacion(java.lang.Long, java.lang.Long, java.lang.Integer, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public OrdenBeneficioSocial selectOrdenVivaByCombinacion(Long idEmpresa, Long tipoBeneficio,
			Integer anio, Long region) throws Throwable {
		System.out.println("Ingresa al metodo selectOrdenVivaByCombinacion de OrdenBeneficioSocial, empresa: "
				+ idEmpresa + ", tipo: " + tipoBeneficio + ", anio: " + anio + ", region: " + region);
		// Misma logica que el indice funcional UQ_ODBS_VIVA del DDL: solo cuentan las
		// ordenes en 1 GENERADA, 2 ENVIADA_A_TESORERIA o 3 PAGADA. Una ANULADA no bloquea
		// generar otra. La region se compara con NVL a -1, igual que el indice, porque los
		// tipos que no son decimo cuarto la llevan en null.
		Query query = em.createQuery(" select   t "
				+ " from     OrdenBeneficioSocial t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.tipoBeneficio = :tipoBeneficio "
				+ "          and t.anio = :anio "
				+ "          and nvl(t.region, -1) = nvl(:region, -1) "
				+ "          and t.estado in (:generada, :enviada, :pagada) ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("tipoBeneficio", tipoBeneficio);
		query.setParameter("anio", anio);
		query.setParameter("region", region);
		query.setParameter("generada", Long.valueOf(RhhEstadoOrdenBeneficio.GENERADA));
		query.setParameter("enviada", Long.valueOf(RhhEstadoOrdenBeneficio.ENVIADA_A_TESORERIA));
		query.setParameter("pagada", Long.valueOf(RhhEstadoOrdenBeneficio.PAGADA));
		List<OrdenBeneficioSocial> encontradas = query.getResultList();
		return (encontradas == null || encontradas.isEmpty()) ? null : encontradas.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.OrdenBeneficioSocialDaoService#selectListado(java.lang.Long, java.lang.Integer, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<OrdenBeneficioSocialResumen> selectListado(Long idEmpresa, Integer anio,
			Long tipoBeneficio, Long estado) throws Throwable {
		System.out.println("Ingresa al metodo selectListado de OrdenBeneficioSocial, empresa: "
				+ idEmpresa + ", anio: " + anio + ", tipo: " + tipoBeneficio + ", estado: " + estado);

		StringBuilder jpql = new StringBuilder(
				" select new com.saa.model.rhh.OrdenBeneficioSocialResumen("
						+ "     o.codigo, o.numero, o.tipoBeneficio, o.anio, o.region, o.total,"
						+ "     o.numeroEmpleados, o.fechaEmision, o.fechaPago, o.estado,"
						+ "     p.id, p.estado, o.asiento) "
						+ " from     OrdenBeneficioSocial o "
						+ " left join o.pagoProgramado p "
						+ " where    o.empresa.codigo = :idEmpresa ");
		if (anio != null) {
			jpql.append(" and o.anio = :anio ");
		}
		if (tipoBeneficio != null) {
			jpql.append(" and o.tipoBeneficio = :tipoBeneficio ");
		}
		if (estado != null) {
			jpql.append(" and o.estado = :estado ");
		}
		jpql.append(" order by o.fechaEmision desc, o.codigo desc ");

		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		if (anio != null) {
			query.setParameter("anio", anio);
		}
		if (tipoBeneficio != null) {
			query.setParameter("tipoBeneficio", tipoBeneficio);
		}
		if (estado != null) {
			query.setParameter("estado", estado);
		}
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.OrdenBeneficioSocialDaoService#countByAnio(java.lang.Integer)
	 */
	@Override
	public long countByAnio(Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo countByAnio de OrdenBeneficioSocial, anio: " + anio);
		Query query = em.createQuery(" select count(t) "
				+ " from   OrdenBeneficioSocial t "
				+ " where  t.anio = :anio ");
		query.setParameter("anio", anio);
		Object resultado = query.getSingleResult();
		return resultado != null ? ((Number) resultado).longValue() : 0L;
	}
}
