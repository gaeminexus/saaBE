package com.saa.ejb.cnt.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.RenglonReporteContableDaoService;
import com.saa.model.cnt.RenglonReporteContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Stateless
public class RenglonReporteContableDaoServiceImpl extends EntityDaoImpl<RenglonReporteContable>
		implements RenglonReporteContableDaoService {

	@PersistenceContext
	EntityManager em;

	public String[] obtieneCampos() {
		return new String[] { "codigo", "reporteContable", "codigoRenglon", "nombre", "nivel", "orden", "tipoCalculo",
				"baseValor", "formula", "vaEnArchivo", "vigenciaDesde", "vigenciaHasta", "estado" };
	}

	@Override
	public List<RenglonReporteContable> selectVigentes(Long codigoReporte, LocalDate corte) throws Throwable {
		TypedQuery<RenglonReporteContable> q = em.createQuery("select r from RenglonReporteContable r"
				+ " where r.reporteContable.codigo = :reporte and r.estado = 1"
				+ " and (r.vigenciaDesde is null or r.vigenciaDesde <= :corte)"
				+ " and (r.vigenciaHasta is null or r.vigenciaHasta >= :corte)"
				+ " order by r.orden, r.codigoRenglon", RenglonReporteContable.class);
		q.setParameter("reporte", codigoReporte);
		q.setParameter("corte", corte);
		return q.getResultList();
	}
}
