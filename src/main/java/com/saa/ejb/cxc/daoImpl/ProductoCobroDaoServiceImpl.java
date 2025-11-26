/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.ProductoCobroDaoService;
import com.saa.model.cxc.ProductoCobro;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ProductoCobroDaoService. 
 */
@Stateless
public class ProductoCobroDaoServiceImpl extends EntityDaoImpl<ProductoCobro>  implements ProductoCobroDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.ProductoCobroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ProductoCobro");
		return new String[]{"codigo",
							"empresa",
							"sRTipoProducto",
							"nombre",
							"aplicaIVA",
							"aplicaRetencion",
							"estado",
							"fechaIngreso",
							"nivel",
							"idPadre",
							"grupoProductoCobro",
							"porcentajeBaseRetencion",
							"fechaAnulacion",
							"numero"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoCobroDaoService#selectByGrupo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked") 
	public List<ProductoCobro> selectByGrupo(Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo selectByGrupo de idGrupo: " + idGrupo);
		Query query = em.createQuery(" from   ProductoCobro b " +
								     " where  b.grupoProductoCobro.codigo = :idGrupo " +
								     " order  by b.codigo");
		query.setParameter("idGrupo", idGrupo);	
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoCobroDaoService#selectRaizByGrupo(java.lang.Long)
	 */
	public ProductoCobro selectRaizByGrupo(Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo selectRaizByGrupo de idGrupo: " + idGrupo);
		Query query = em.createQuery(" from   ProductoCobro b " +
									 " where  b.grupoProductoCobro.codigo = :idGrupo" +
									 "        and   b.nivel = :raiz ");
		query.setParameter("idGrupo", idGrupo);		
		query.setParameter("raiz", Long.valueOf(Estado.RAIZ));
		return (ProductoCobro)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoCobroDaoService#selectByNumeroGrupo(java.lang.String, java.lang.Long)
	 */
	public ProductoCobro selectByNumeroGrupo(String numero, Long idGrupo)
			throws Throwable {
		System.out.println("Ingresa al metodo selectByNumeroGrupo de idGrupo: " + idGrupo);
		Query query = em.createQuery(" from   ProductoCobro b " +
									 " where  b.grupoProductoCobro.codigo = :idGrupo " +
									 " 		  and   b.numero = :numero");
		query.setParameter("idGrupo", idGrupo);
		query.setParameter("numero", numero);
		return (ProductoCobro)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoCobroDaoService#numeroRegActivosByIdPadre(java.lang.Long)
	 */
	public int numeroRegActivosByIdPadre(Long idPadre) throws Throwable {
		System.out.println("Ingresa al metodo numeroRegActivosByIdPadre con idPadre: " + idPadre);
		Query query = em.createQuery(" from   ProductoCobro b " +
									 " where  b.codigoPadre = :idPadre" +
									 "        and   b.estado = :estado ");
		query.setParameter("idPadre", idPadre);		
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));

		return query.getResultList().size();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoCobroDaoService#selectHijosByNumeroProducto(java.lang.String, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<ProductoCobro> selectHijosByNumeroProducto(
			String numeroProducto, Long idGrupo) throws Throwable {
		System.out.println("Ingresa al Metodo selectHijosByNumeroProducto con numeroProducto: " + numeroProducto + ", idGrupo: " + idGrupo);
		Query query = em.createQuery(" from     ProductoCobro c " +
									 " where    c.grupoProductoCobro.codigo = :idGrupo " +
									 "			and   c.numero LIKE :numeroProducto " +
									 " order by c.numero");
		query.setParameter("idGrupo", idGrupo);
		if("0".equals(numeroProducto)){
			query.setParameter("numeroProducto", "%");	
		}else{
			query.setParameter("numeroProducto", numeroProducto + ".%");	
		}		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxc.ejb.dao.ProductoCobroDaoService#selectByIdPadre(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<ProductoCobro> selectByIdPadre(Long idPadre) throws Throwable {
		System.out.println("Dao selectByIdPadre de idPadre: " + idPadre);
		Query query = em.createQuery(" from   ProductoCobro b " +
								     " where  b.idPadre = :idPadre ");
		query.setParameter("idPadre", idPadre);	
		return query.getResultList();
	}	

	
}