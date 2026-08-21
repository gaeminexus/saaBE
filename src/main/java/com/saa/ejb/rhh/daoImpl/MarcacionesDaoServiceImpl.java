/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.daoImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.MarcacionesDaoService;
import com.saa.model.rhh.Marcaciones;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion MarcacionesDaoService. 
 */
@Stateless
public class MarcacionesDaoServiceImpl extends EntityDaoImpl<Marcaciones>  implements MarcacionesDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.MarcacionesDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Marcaciones");
		return new String[]{"codigo",
							"empleado",
							"fechaHora",
							"tipo",
							"origen",
							"cargaMarcaciones",
							"dispositivo",
							"lineaArchivo",
							"procesado",
							"observacion",
							"fechaRegistro",
							"usuarioRegistro"};
	}
	
	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.MarcacionesDaoService#existeMarcacion(java.lang.Long, java.time.LocalDateTime)
	 */
	@Override
	public boolean existeMarcacion(Long idEmpleado, LocalDateTime fechaHora) throws Throwable {
		Query query = em.createQuery(" select count(t) "
				+ " from   Marcaciones t "
				+ " where  t.empleado.codigo = :idEmpleado "
				+ "        and t.fechaHora = :fechaHora ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("fechaHora", fechaHora);
		Long cuantas = (Long) query.getSingleResult();
		return cuantas != null && cuantas.longValue() > 0L;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.MarcacionesDaoService#selectByEmpleadoYDia(java.lang.Long, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Marcaciones> selectByEmpleadoYDia(Long idEmpleado, LocalDate dia) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpleadoYDia de Marcaciones, empleado: "
				+ idEmpleado + ", dia: " + dia);
		Query query = em.createQuery(" select   t "
				+ " from     Marcaciones t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.fechaHora >= :desde and t.fechaHora < :hasta "
				+ " order by t.fechaHora ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("desde", dia.atStartOfDay());
		query.setParameter("hasta", dia.plusDays(1).atStartOfDay());
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.MarcacionesDaoService#selectPendientesConsolidar(java.time.LocalDate, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Marcaciones> selectPendientesConsolidar(LocalDate desde, LocalDate hasta) throws Throwable {
		System.out.println("Ingresa al metodo selectPendientesConsolidar de Marcaciones, rango: "
				+ desde + " a " + hasta);
		Query query = em.createQuery(" select   t "
				+ " from     Marcaciones t "
				+ " where    t.fechaHora >= :desde and t.fechaHora < :hasta "
				+ "          and (t.procesado is null or t.procesado <> 'S') "
				+ " order by t.empleado.codigo, t.fechaHora ");
		query.setParameter("desde", desde.atStartOfDay());
		query.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.MarcacionesDaoService#eliminaByCarga(java.lang.Long)
	 */
	@Override
	public int eliminaByCarga(Long idCarga) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByCarga de Marcaciones, carga: " + idCarga);
		Query query = em.createQuery(" delete from Marcaciones t "
				+ " where  t.cargaMarcaciones.codigo = :idCarga ");
		query.setParameter("idCarga", idCarga);
		return query.executeUpdate();
	}
}
