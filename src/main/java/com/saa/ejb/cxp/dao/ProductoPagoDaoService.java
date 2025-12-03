/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.ProductoPago;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ProductoPago. 
 */
@Local
public interface ProductoPagoDaoService  extends EntityDao<ProductoPago>  {
	
	/**
	 * Recupera los productos de pago de una empresa
	 * @param idGrupo		: Id del grupo de producto de pago del que se recupera los productos
	 * @return				: Lista de productos que contiene el grupo
	 * @throws Throwable	: Excepcion
	 */
	List<ProductoPago> selectByGrupo(Long idGrupo) throws Throwable;
	
	/**
	 * Recupera el nivel RAIZ del grup de producto
	 * @param idGrupo	: Id del grupo de productos
	 * @return			: Nivel raiz del grupo de producto
	 * @throws Throwable: Excepcion
	 */
	ProductoPago selectRaizByGrupo(Long idGrupo) throws Throwable;
	
	/**
	 * Recupera por el numero de producto y el id del grupo de producto
	 * @param numero	: Numero de producto
	 * @param idGrupo	: Id del grupo de producto
	 * @return			: Registro recuperado
	 * @throws Throwable: Excepcion
	 */
	ProductoPago selectByNumeroGrupo(String numero, Long idGrupo) throws Throwable;
	
	/**
	 * Recupera el numero de registros activos que tienen el mismo id Padre
	 * @param idPadre	: Id del producto padre
	 * @return			: Numero de registros activos con el idPadre del parametro
	 * @throws Throwable: Excepcion
	 */
	int numeroRegActivosByIdPadre(Long idPadre) throws Throwable;
	
	/**
	 * Recupera todas los productos que tengan el mismo número de producto seguido de un punto para ver cuáles son hijos
	 * @param numeroProducto	: Número de producto a buscar
	 * @param idGrupo		: Id del grupo de producto
	 * @return				: Listado de productos hijos de un producto
	 * @throws Throwable	: Excepcion
	 */
	List<ProductoPago> selectHijosByNumeroProducto(String numeroProducto, Long idGrupo) throws Throwable;
	
	/**
	 * Recupera el listado de cuentas que tienen el mismo padre
	 * @param idPadre		: Id del la cuenta padre
	 * @return				: Lista de cuentas que tienen el mismo padre
	 * @throws Throwable	: Excepcion
	 */
	List<ProductoPago> selectByIdPadre(Long idPadre) throws Throwable;
	
}