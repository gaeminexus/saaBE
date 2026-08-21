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
import com.saa.ejb.rhh.dao.RolPagoDaoService;
import com.saa.model.rhh.RolPago;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion RolPagoDaoService. 
 */
@Stateless
public class RolPagoDaoServiceImpl extends EntityDaoImpl<RolPago>  implements RolPagoDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.RolPagoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) RolPago");
		return new String[]{"codigo",
							"nomina",
							"numero",
							"fechaEmision",
							"rutaPdf",
							"estado",
							"fechaRegistro",
							"usuarioRegistro",
							"totalIngresos",
							"totalDescuentos",
							"neto",
							"hash",
							"fechaEnvio",
							"recibido"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.RolPagoDaoService#selectByNomina(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public RolPago selectByNomina(Long idNomina) throws Throwable {
		System.out.println("Ingresa al metodo selectByNomina de RolPago, nomina: " + idNomina);
		Query query = em.createQuery(" select   t "
				+ " from     RolPago t "
				+ " where    t.nomina.codigo = :idNomina "
				+ " order by t.codigo ");
		query.setParameter("idNomina", idNomina);
		List<RolPago> lista = query.getResultList();
		// Se devuelve null en vez de lanzar: el llamador lo usa para decidir entre crear
		// y actualizar, y "todavia no existe" no es una condicion de error.
		return lista.isEmpty() ? null : lista.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.RolPagoDaoService#selectByPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<RolPago> selectByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodo de RolPago, periodo: " + idPeriodo);
		Query query = em.createQuery(" select   t "
				+ " from     RolPago t "
				+ " where    t.nomina.periodoNomina.codigo = :idPeriodo "
				+ " order by t.nomina.empleado.apellidos, t.nomina.empleado.nombres, t.codigo ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}

}
