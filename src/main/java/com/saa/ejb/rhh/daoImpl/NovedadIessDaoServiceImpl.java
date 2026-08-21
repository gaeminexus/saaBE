package com.saa.ejb.rhh.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.NovedadIessDaoService;
import com.saa.model.rhh.NovedadIess;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion NovedadIessDaoService.
 */
@Stateless
public class NovedadIessDaoServiceImpl extends EntityDaoImpl<NovedadIess> implements NovedadIessDaoService {

	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NovedadIessDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) NovedadIess");
		return new String[]{"codigo",
							"empleado",
							"contrato",
							"tipoNovedad",
							"fechaHecho",
							"fechaLimite",
							"fechaReporte",
							"sueldoAnterior",
							"sueldoNuevo",
							"modalidadFondosReserva",
							"causalTerminacion",
							"estado",
							"observacion",
							"fechaRegistro",
							"usuarioRegistro",
							"diasDeclarados",
							"sueldoReferencial",
							"valorVariacion",
							"causaIess",
							"fechaFallecimiento",
							"fechaFin",
							"periodoDesde",
							"periodoHasta",
							"mesesLaborados",
							"respuestaIess",
							"lote"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NovedadIessDaoService#selectByVentana(java.lang.Long, java.time.LocalDate, java.time.LocalDate, java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<NovedadIess> selectByVentana(Long idEmpresa, LocalDate desde, LocalDate hasta,
			List<Long> estados) throws Throwable {
		System.out.println("Ingresa al metodo selectByVentana de NovedadIess, empresa: " + idEmpresa);
		// La empresa se alcanza por el empleado: NVIS no la lleva.
		StringBuilder jpql = new StringBuilder(" select   t "
				+ " from     NovedadIess t "
				+ " where    t.empleado.empresa.codigo = :idEmpresa "
				+ "          and t.fechaHecho between :desde and :hasta ");
		boolean filtraEstado = estados != null && !estados.isEmpty();
		if (filtraEstado) {
			jpql.append(" and t.estado in :estados ");
		}
		jpql.append(" order by t.fechaLimite, t.tipoNovedad ");
		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		if (filtraEstado) {
			query.setParameter("estados", estados);
		}
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NovedadIessDaoService#selectByTipoEnVentana(java.lang.Long, java.lang.Long, java.time.LocalDate, java.time.LocalDate, java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<NovedadIess> selectByTipoEnVentana(Long idEmpresa, Long tipoNovedad, LocalDate desde,
			LocalDate hasta, List<Long> estados) throws Throwable {
		System.out.println("Ingresa al metodo selectByTipoEnVentana de NovedadIess, tipo: " + tipoNovedad);
		StringBuilder jpql = new StringBuilder(" select   t "
				+ " from     NovedadIess t "
				+ " where    t.empleado.empresa.codigo = :idEmpresa "
				+ "          and t.tipoNovedad = :tipoNovedad "
				+ "          and t.fechaHecho between :desde and :hasta ");
		boolean filtraEstado = estados != null && !estados.isEmpty();
		if (filtraEstado) {
			jpql.append(" and t.estado in :estados ");
		}
		jpql.append(" order by t.empleado.identificacion ");
		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("tipoNovedad", tipoNovedad);
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		if (filtraEstado) {
			query.setParameter("estados", estados);
		}
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NovedadIessDaoService#selectByContratoTipoEnVentana(java.lang.Long, java.lang.Long, java.time.LocalDate, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<NovedadIess> selectByContratoTipoEnVentana(Long idContrato, Long tipoNovedad,
			LocalDate desde, LocalDate hasta) throws Throwable {
		System.out.println("Ingresa al metodo selectByContratoTipoEnVentana de NovedadIess, contrato: " + idContrato);
		Query query = em.createQuery(" select   t "
				+ " from     NovedadIess t "
				+ " where    t.contrato.codigo = :idContrato "
				+ "          and t.tipoNovedad = :tipoNovedad "
				+ "          and t.fechaHecho between :desde and :hasta "
				+ " order by t.codigo ");
		query.setParameter("idContrato", idContrato);
		query.setParameter("tipoNovedad", tipoNovedad);
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		return query.getResultList();
	}

}
