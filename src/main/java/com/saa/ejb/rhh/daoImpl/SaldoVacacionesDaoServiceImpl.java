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
import com.saa.ejb.rhh.dao.SaldoVacacionesDaoService;
import com.saa.model.rhh.SaldoVacaciones;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion SaldoVacacionesDaoService. 
 */
@Stateless
public class SaldoVacacionesDaoServiceImpl extends EntityDaoImpl<SaldoVacaciones>  implements SaldoVacacionesDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SaldoVacacionesDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) SaldoVacaciones");
		return new String[]{"codigo",
							"empleado",
							"anio",
							"diasAsignados",
							"diasUsados",
							"diasPendientes",
							"fechaRegistro",
							"usuarioRegistro",
							"fechaInicio",
							"fechaFin",
							"diasAdicionales",
							"diasArrastrados",
							"diasPagados",
							"valorDia",
							"caducado",
							"aperturaMigracion",
							"estado"};
	}
	

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SaldoVacacionesDaoService#selectDisponibles(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<SaldoVacaciones> selectDisponibles(Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo selectDisponibles de SaldoVacaciones, empleado: " + idEmpleado);
		// Orden ascendente por anio: el consumo es FIFO sobre los saldos mas antiguos.
		Query query = em.createQuery(" select   t "
				+ " from     SaldoVacaciones t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and (t.caducado is null or t.caducado <> 'S') "
				+ "          and t.diasPendientes > 0 "
				+ " order by t.anio, t.codigo ");
		query.setParameter("idEmpleado", idEmpleado);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SaldoVacacionesDaoService#selectByEmpleadoYAnio(java.lang.Long, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SaldoVacaciones selectByEmpleadoYAnio(Long idEmpleado, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpleadoYAnio de SaldoVacaciones, empleado: "
				+ idEmpleado + ", anio: " + anio);
		Query query = em.createQuery(" select   t "
				+ " from     SaldoVacaciones t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.anio = :anio ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("anio", anio);
		List<SaldoVacaciones> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}
}
