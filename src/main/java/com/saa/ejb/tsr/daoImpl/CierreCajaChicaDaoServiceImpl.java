package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.CierreCajaChicaDaoService;
import com.saa.model.tsr.CierreCajaChica;
import com.saa.rubros.EstadoCierreCajaChica;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 * Implementacion CierreCajaChicaDaoService.
 */
@Stateless
public class CierreCajaChicaDaoServiceImpl extends EntityDaoImpl<CierreCajaChica>
		implements CierreCajaChicaDaoService {

	@PersistenceContext
	EntityManager em;

	public String[] obtieneCampos() {
		return new String[]{"codigo",
							"cajaChica",
							"fecha",
							"fechaInicio",
							"fechaFin",
							"saldoInicial",
							"totalGastos",
							"totalReposiciones",
							"totalAjustes",
							"saldoLibros",
							"saldoFisico",
							"diferencia",
							"observacion",
							"estado",
							"asiento",
							"fechaRegistro",
							"usuario"};
	}

	@SuppressWarnings("unchecked")
	public List<CierreCajaChica> selectByCaja(Long idCaja) throws Throwable {
		Query query = em.createQuery(
				" select c from CierreCajaChica c where c.cajaChica.codigo = :idCaja order by c.fecha desc, c.codigo desc");
		query.setParameter("idCaja", idCaja);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public CierreCajaChica selectUltimoCerrado(Long idCaja) throws Throwable {
		Query query = em.createQuery(
				" select c from CierreCajaChica c "
				+ " where c.cajaChica.codigo = :idCaja and c.estado = :cerrado "
				+ " order by c.fechaFin desc, c.codigo desc");
		query.setParameter("idCaja", idCaja);
		query.setParameter("cerrado", Long.valueOf(EstadoCierreCajaChica.CERRADO));
		query.setMaxResults(1);
		List<CierreCajaChica> resultado = query.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}

	public boolean existeBorrador(Long idCaja) throws Throwable {
		Query query = em.createQuery(
				" select count(c) from CierreCajaChica c "
				+ " where c.cajaChica.codigo = :idCaja and c.estado = :borrador");
		query.setParameter("idCaja", idCaja);
		query.setParameter("borrador", Long.valueOf(EstadoCierreCajaChica.BORRADOR));
		Long total = (Long) query.getSingleResult();
		return total != null && total > 0;
	}

}
