/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.ProductoPagoDaoService;
import com.saa.model.cxp.ProductoPago;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ProductoPagoDaoService. 
 */
@Stateless
public class ProductoPagoDaoServiceImpl extends EntityDaoImpl<ProductoPago>  implements ProductoPagoDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.ProductoPagoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ProductoPago");
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
							"grupoProductoPago",
							"porcentajeBaseRetencion",
							"fechaAnulacion",
							"numero"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoPagoDaoService#selectByGrupo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked") 
	public List<ProductoPago> selectByGrupo(Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo selectByGrupo de idGrupo: " + idGrupo);
		Query query = em.createQuery(" from   ProductoPago b " +
								     " where  b.grupoProductoPago.codigo = :idGrupo " +
								     " order  by b.codigo");
		query.setParameter("idGrupo", idGrupo);	
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoPagoDaoService#selectRaizByGrupo(java.lang.Long)
	 */
	public ProductoPago selectRaizByGrupo(Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo selectRaizByGrupo de idGrupo: " + idGrupo);
		Query query = em.createQuery(" from   ProductoPago b " +
									 " where  b.grupoProductoPago.codigo = :idGrupo" +
									 "        and   b.nivel = :raiz ");
		query.setParameter("idGrupo", idGrupo);		
		query.setParameter("raiz", Long.valueOf(Estado.RAIZ));
		return (ProductoPago)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoPagoDaoService#selectByNumeroGrupo(java.lang.String, java.lang.Long)
	 */
	public ProductoPago selectByNumeroGrupo(String numero, Long idGrupo)
			throws Throwable {
		System.out.println("Ingresa al metodo selectByNumeroGrupo de idGrupo: " + idGrupo);
		Query query = em.createQuery(" from   ProductoPago b " +
									 " where  b.grupoProductoPago.codigo = :idGrupo " +
									 " 		  and   b.numero = :numero");
		query.setParameter("idGrupo", idGrupo);
		query.setParameter("numero", numero);
		return (ProductoPago)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoPagoDaoService#numeroRegActivosByIdPadre(java.lang.Long)
	 */
	public int numeroRegActivosByIdPadre(Long idPadre) throws Throwable {
		System.out.println("Ingresa al metodo numeroRegActivosByIdPadre con idPadre: " + idPadre);
		Query query = em.createQuery(" from   ProductoPago b " +
									 " where  b.codigoPadre = :idPadre" +
									 "        and   b.estado = :estado ");
		query.setParameter("idPadre", idPadre);		
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));

		return query.getResultList().size();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoPagoDaoService#selectHijosByNumeroProducto(java.lang.String, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<ProductoPago> selectHijosByNumeroProducto(
			String numeroProducto, Long idGrupo) throws Throwable {
		System.out.println("Ingresa al Metodo selectHijosByNumeroProducto con numeroProducto: " + numeroProducto + ", idGrupo: " + idGrupo);
		Query query = em.createQuery(" from     ProductoPago c " +
									 " where    c.grupoProductoPago.codigo = :idGrupo " +
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
	 * @see com.compuseg.income.cxp.ejb.dao.ProductoPagoDaoService#selectByIdPadre(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<ProductoPago> selectByIdPadre(Long idPadre) throws Throwable {
		System.out.println("Dao selectByIdPadre de idPadre: " + idPadre);
		Query query = em.createQuery(" from   ProductoPago b " +
								     " where  b.idPadre = :idPadre ");
		query.setParameter("idPadre", idPadre);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.cxp.dao.ProductoPagoDaoService#selectDocumentosQueUsanProducto(java.lang.Long)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<String> selectDocumentosQueUsanProducto(Long idProducto) throws Throwable {
		System.out.println("Ingresa al metodo selectDocumentosQueUsanProducto con idProducto: " + idProducto);
		List<String> resultado = new java.util.ArrayList<String>();
		if (idProducto == null) {
			return resultado;
		}

		List<Object[]> facturas = em.createQuery(
				"select distinct f.id, f.numero from DetalleFacturaCompra d join d.factura f "
						+ "where d.producto = :idProducto")
				.setParameter("idProducto", idProducto)
				.getResultList();
		for (Object[] fila : facturas) {
			resultado.add("Factura de compra N° " + descripcionDocumentoUso(fila));
		}

		List<Object[]> notas = em.createQuery(
				"select distinct n.id, n.numero from DetalleNotaCreditoCompra d join d.notaCredito n "
						+ "where d.producto = :idProducto")
				.setParameter("idProducto", idProducto)
				.getResultList();
		for (Object[] fila : notas) {
			resultado.add("Nota de crédito de compra N° " + descripcionDocumentoUso(fila));
		}

		List<Object[]> liquidaciones = em.createQuery(
				"select distinct l.id, l.numero from DetalleLiquidacionCompraCompra d join d.liquidacion l "
						+ "where d.producto.id = :idProducto")
				.setParameter("idProducto", idProducto)
				.getResultList();
		for (Object[] fila : liquidaciones) {
			resultado.add("Liquidación de compra N° " + descripcionDocumentoUso(fila));
		}

		return resultado;
	}

	/**
	 * Arma "numero (id X)", o solo "(id X)" si el documento no tiene número grabado.
	 */
	private String descripcionDocumentoUso(Object[] fila) {
		Long id = (Long) fila[0];
		String numero = (String) fila[1];
		return (numero != null && !numero.trim().isEmpty() ? numero + " " : "") + "(id " + id + ")";
	}

}