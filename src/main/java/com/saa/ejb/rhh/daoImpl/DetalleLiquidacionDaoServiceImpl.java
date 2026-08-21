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
import com.saa.ejb.rhh.dao.DetalleLiquidacionDaoService;
import com.saa.model.rhh.DetalleLiquidacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DetalleLiquidacionDaoService. 
 */
@Stateless
public class DetalleLiquidacionDaoServiceImpl extends EntityDaoImpl<DetalleLiquidacion>  implements DetalleLiquidacionDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleLiquidacionDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleLiquidacion");
		return new String[]{"codigo",
							"liquidacion",
							"valor",
							"descripcion",
							"fechaRegistro",
							"conceptoNomina",
							"tipoConcepto",
							"baseCalculo",
							"dias",
							"orden",
							"usuarioRegistro"};
	}
	
	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleLiquidacionDaoService#eliminaByLiquidacion(java.lang.Long)
	 */
	@Override
	public int eliminaByLiquidacion(Long idLiquidacion) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByLiquidacion de DetalleLiquidacion, liquidacion: "
				+ idLiquidacion);
		Query query = em.createQuery(" delete from DetalleLiquidacion t "
				+ " where  t.liquidacion.codigo = :idLiquidacion ");
		query.setParameter("idLiquidacion", idLiquidacion);
		return query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleLiquidacionDaoService#selectByLiquidacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleLiquidacion> selectByLiquidacion(Long idLiquidacion) throws Throwable {
		System.out.println("Ingresa al metodo selectByLiquidacion de DetalleLiquidacion, liquidacion: "
				+ idLiquidacion);
		Query query = em.createQuery(" select   t "
				+ " from     DetalleLiquidacion t "
				+ " where    t.liquidacion.codigo = :idLiquidacion "
				+ " order by t.orden, t.codigo ");
		query.setParameter("idLiquidacion", idLiquidacion);
		return query.getResultList();
	}
}
