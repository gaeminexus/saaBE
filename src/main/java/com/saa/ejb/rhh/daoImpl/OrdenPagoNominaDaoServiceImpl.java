package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService;
import com.saa.model.rhh.OrdenPagoNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion OrdenPagoNominaDaoService.
 */
@Stateless
public class OrdenPagoNominaDaoServiceImpl extends EntityDaoImpl<OrdenPagoNomina>
		implements OrdenPagoNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) OrdenPagoNomina");
		return new String[]{"codigo",
							"empresa",
							"periodoNomina",
							"cuentaBancaria",
							"numero",
							"fechaEmision",
							"fechaAcreditacion",
							"total",
							"numeroEmpleados",
							"rutaArchivo",
							"asientoPago",
							"egreso",
							"estado",
							"observaciones",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService#selectByPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<OrdenPagoNomina> selectByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodo de OrdenPagoNomina, periodo: " + idPeriodo);
		Query query = em.createQuery(" select   t "
				+ " from     OrdenPagoNomina t "
				+ " where    t.periodoNomina.codigo = :idPeriodo "
				+ " order by t.codigo desc ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService#selectPendientesByPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<OrdenPagoNomina> selectPendientesByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectPendientesByPeriodo de OrdenPagoNomina, periodo: "
				+ idPeriodo);
		Query query = em.createQuery(" select   t "
				+ " from     OrdenPagoNomina t "
				+ " where    t.periodoNomina.codigo = :idPeriodo "
				+ "          and t.fechaAcreditacion is null "
				+ " order by t.codigo desc ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}
}
