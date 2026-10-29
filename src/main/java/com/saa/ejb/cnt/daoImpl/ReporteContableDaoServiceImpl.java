package com.saa.ejb.cnt.daoImpl;


import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.ReporteContableDaoService;
import com.saa.model.cnt.ReporteContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Stateless
public class ReporteContableDaoServiceImpl extends EntityDaoImpl<ReporteContable>  implements ReporteContableDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"empresa",
							"nombreReporte",
							"estado",
							"codigoAlterno",
							"detalleReporteContables",
							"tipoReporte",
							"codigoEstructura",
							"periodicidad",
							"codigoEntidad",
							"baseValor",
							"mapeoAutomatico",
							"acumulaPorPrefijo",
							"formatoArchivo",
							"sumaControl",
							"titulo",
							"nivelMaximo"};
	}

	@Override
	public ReporteContable selectByEmpresaAlterno(Long empresa, Long codigoAlterno) throws Throwable {
		TypedQuery<ReporteContable> q = em.createQuery("select r from ReporteContable r where r.empresa.codigo = :empresa"
				+ " and r.codigoAlterno = :alterno", ReporteContable.class);
		q.setParameter("empresa", empresa);
		q.setParameter("alterno", codigoAlterno);
		List<ReporteContable> l = q.getResultList();
		return l.isEmpty() ? null : l.get(0);
	}

	@Override
	public ReporteContable selectByEmpresaEstructura(Long empresa, String codigoEstructura) throws Throwable {
		TypedQuery<ReporteContable> q = em.createQuery("select r from ReporteContable r where r.empresa.codigo = :empresa"
				+ " and r.codigoEstructura = :estructura", ReporteContable.class);
		q.setParameter("empresa", empresa);
		q.setParameter("estructura", codigoEstructura);
		List<ReporteContable> l = q.getResultList();
		return l.isEmpty() ? null : l.get(0);
	}

}
