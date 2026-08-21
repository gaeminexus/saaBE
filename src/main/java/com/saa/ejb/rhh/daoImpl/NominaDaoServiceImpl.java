/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.model.rhh.Nomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion NominaDaoService. 
 */
@Stateless
public class NominaDaoServiceImpl extends EntityDaoImpl<Nomina>  implements NominaDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Nomina");
		return new String[]{"codigo",
							"periodoNomina",
							"empleado",
							"contratoEmpleado",
							"salarioBase",
							"totalIngresos",
							"totalDescuentos",
							"netoPagar",
							"estado",
							"fechaRegistro",
							"usuarioRegistro",
							"diasTrabajados",
							"horasTrabajadas",
							"baseIess",
							"baseImpuestoRenta",
							"baseFondosReserva",
							"baseDecimoTercero",
							"baseDecimoCuarto",
							"aportePersonal",
							"aportePatronal",
							"aporteIeceSecap",
							"fondosReserva",
							"retencionImpuestoRenta",
							"totalPatronal",
							"observacion"};
	}
	

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NominaDaoService#selectByPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Nomina> selectByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodo de Nomina, periodo: " + idPeriodo);
		Query query = em.createQuery(" select   t "
				+ " from     Nomina t "
				+ " where    t.periodoNomina.codigo = :idPeriodo "
				+ " order by t.empleado.apellidos, t.empleado.nombres ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.NominaDaoService#selectByPeriodoYEmpleado(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Nomina selectByPeriodoYEmpleado(Long idPeriodo, Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodoYEmpleado de Nomina, periodo: " + idPeriodo
				+ ", empleado: " + idEmpleado);
		Query query = em.createQuery(" select   t "
				+ " from     Nomina t "
				+ " where    t.periodoNomina.codigo = :idPeriodo "
				+ "          and t.empleado.codigo = :idEmpleado ");
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("idEmpleado", idEmpleado);
		List<Nomina> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}
}
