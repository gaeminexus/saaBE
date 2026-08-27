package com.saa.ejb.tsr.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.MovimientoCajaChicaDaoService;
import com.saa.model.tsr.MovimientoCajaChica;
import com.saa.rubros.EstadoMovimientoCajaChica;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 * Implementacion MovimientoCajaChicaDaoService.
 */
@Stateless
public class MovimientoCajaChicaDaoServiceImpl extends EntityDaoImpl<MovimientoCajaChica>
		implements MovimientoCajaChicaDaoService {

	@PersistenceContext
	EntityManager em;

	public String[] obtieneCampos() {
		return new String[]{"codigo",
							"cajaChica",
							"tipo",
							"fecha",
							"valor",
							"descripcion",
							"observacion",
							"producto",
							"titular",
							"numeroDocumento",
							"asiento",
							"pagoProgramado",
							"cierre",
							"estado",
							"motivoAnulacion",
							"fechaRegistro",
							"usuario"};
	}

	@SuppressWarnings("unchecked")
	public List<MovimientoCajaChica> selectByCaja(Long idCaja, LocalDate desde, LocalDate hasta,
			Long tipo, Long estado) throws Throwable {
		StringBuilder jpql = new StringBuilder(
				" select m from MovimientoCajaChica m where m.cajaChica.codigo = :idCaja ");
		if (desde != null) {
			jpql.append(" and m.fecha >= :desde ");
		}
		if (hasta != null) {
			jpql.append(" and m.fecha <= :hasta ");
		}
		if (tipo != null) {
			jpql.append(" and m.tipo = :tipo ");
		}
		if (estado != null) {
			jpql.append(" and m.estado = :estado ");
		}
		jpql.append(" order by m.fecha, m.codigo ");

		Query query = em.createQuery(jpql.toString());
		query.setParameter("idCaja", idCaja);
		if (desde != null) {
			query.setParameter("desde", desde);
		}
		if (hasta != null) {
			query.setParameter("hasta", hasta);
		}
		if (tipo != null) {
			query.setParameter("tipo", tipo);
		}
		if (estado != null) {
			query.setParameter("estado", estado);
		}
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumasPorTipo(Long idCaja, LocalDate desde, LocalDate hasta) throws Throwable {
		StringBuilder jpql = new StringBuilder(
				" select m.tipo, sum(m.valor) from MovimientoCajaChica m "
				+ " where m.cajaChica.codigo = :idCaja and m.estado = :activo ");
		if (desde != null) {
			jpql.append(" and m.fecha >= :desde ");
		}
		if (hasta != null) {
			jpql.append(" and m.fecha <= :hasta ");
		}
		jpql.append(" group by m.tipo ");

		Query query = em.createQuery(jpql.toString());
		query.setParameter("idCaja", idCaja);
		query.setParameter("activo", Long.valueOf(EstadoMovimientoCajaChica.ACTIVO));
		if (desde != null) {
			query.setParameter("desde", desde);
		}
		if (hasta != null) {
			query.setParameter("hasta", hasta);
		}
		return query.getResultList();
	}

	public LocalDate selectFechaPrimerMovimiento(Long idCaja) throws Throwable {
		Query query = em.createQuery(
				" select min(m.fecha) from MovimientoCajaChica m where m.cajaChica.codigo = :idCaja");
		query.setParameter("idCaja", idCaja);
		return (LocalDate) query.getSingleResult();
	}

	@SuppressWarnings("unchecked")
	public List<MovimientoCajaChica> selectActivosEnRango(Long idCaja, LocalDate desde, LocalDate hasta)
			throws Throwable {
		Query query = em.createQuery(
				" select m from MovimientoCajaChica m "
				+ " where m.cajaChica.codigo = :idCaja and m.estado = :activo "
				+ " and m.fecha >= :desde and m.fecha <= :hasta "
				+ " order by m.fecha, m.codigo");
		query.setParameter("idCaja", idCaja);
		query.setParameter("activo", Long.valueOf(EstadoMovimientoCajaChica.ACTIVO));
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<MovimientoCajaChica> selectByCierre(Long idCierre) throws Throwable {
		Query query = em.createQuery(
				" select m from MovimientoCajaChica m where m.cierre.codigo = :idCierre order by m.fecha, m.codigo");
		query.setParameter("idCierre", idCierre);
		return query.getResultList();
	}

}
