/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.daoImpl;

import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.TempDebitoCreditoDaoService;
import com.saa.model.tesoreria.TempDebitoCredito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion TempDebitoCreditoDaoService.
 */
@Stateless
public class TempDebitoCreditoDaoServiceImpl extends EntityDaoImpl<TempDebitoCredito> implements TempDebitoCreditoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"tipo",
							"usuario",
							"detallePlantilla",
							"descripcion",
							"valor",
							"empresa"};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempDebitoCreditoDaoService#selectTempDebitoCreditoByTipo(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<TempDebitoCredito> selectTempDebitoCreditoByTipo( Long tipoMovimiento, Long idUsuarioDebito) throws Throwable {
		System.out.println("Ingresa al Metodo selectTempDetalleCreditoByTipo con tipoMovimiento: " + tipoMovimiento + ", idUsuarioDebito " + idUsuarioDebito);
		Query query = em.createQuery(" select b " +
									 " from   TempDebitoCredito b " +
									 " where  b.tipo = :tipoMovimiento" +
									 "		  and b.usuario.codigo = :idUsuarioDebito");
		query.setParameter("tipoMovimiento", tipoMovimiento);
		query.setParameter("idUsuarioDebito", idUsuarioDebito);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempDebitoCreditoDaoService#eliminarPorUsuarioTipo(java.lang.Long, java.lang.Long)
	 */
	public void eliminarPorUsuarioTipo(Long tipoMovimiento, Long idUsuarioDebito) throws Throwable {
		System.out.println("Ingresa al Metodo eliminarPorUsuarioTipo con tipoMovimiento: " + tipoMovimiento + ", idUsuarioDebito " + idUsuarioDebito);
		Query query = em.createQuery(" delete from   TempDebitoCredito b " +
									 " where  b.tipo = :tipoMovimiento" +
									 "		  and b.usuario.codigo = :idUsuarioDebito");
		query.setParameter("tipoMovimiento", tipoMovimiento);
		query.setParameter("idUsuarioDebito", idUsuarioDebito);
		try {
			query.executeUpdate();
		} catch (Exception e) {
			throw new IncomeException("ERROR AL ELIMINAR DATOS TEMPORALES DE DEBITOS Y CREDITOS");
		}
	}

}