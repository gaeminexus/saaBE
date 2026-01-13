/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cxp.ProductoPago;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ProductoPago.
 *  Accede a los metodos DAO y procesa los datos para el ProductoPago.</p>
 */
@Local
public interface ProductoPagoService extends EntityService<ProductoPago>{

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	 */
	 ProductoPago selectById(Long id) throws Throwable;
	 
	 /**
	 * Método que verifica el numero de registros en un grupo de productos
	 * @param idGrupo		: Id del grupo de producto de pago
	 * @return numero de productos en un grupo de producto
	 * @throws Throwable	: excepcion
	 */
	 int numeroRegistrosGrupo(Long idGrupo) throws Throwable;
	 
	 /**
	 * Metodo que crea el nodo raiz del arbol de productos para cuando no se ingresan registros
	 * @param idGrupo 	: Codigo del grupo de producto en el que se desea crear el nodo
	 * @return Mensaje que indica si hubo errores o no.
	 */
	 String creaNodoArbolCero(Long idGrupo) throws Throwable;
	  
	 /**
	  * Metodo para crear la numeracion del producto dependiendo de donde se lo ingrese
	  * @param object		: Arreglo de objetos que contiene los campos enviados desde el cliente
	  * @param campos		: Arreglo que contiene el nombre de los campos enviados por el cliente
	  * @param idGrupo		: Id del grupo al que pertenece el producto
	  * @return				: Mensaje
	  * @throws Throwable	: Excepcion
	 */
	 String saveProducto(Object[][] object, Object[] campos, Long idGrupo) throws Throwable;
	 
	 /**
	 * Metodo para verificar si el producto es de tipo acumulacion o movimiento
	 * @param id			:Codigo del producto
	 * @return				:Tipo de producto 1 = ACUMULACION, 2 = MOVIMIENTO 
	 * @throws Throwable	: Excepcion
	 */
	 Long recuperaTipo(Long id) throws Throwable;
	 
	 /**
	 * Elimina un producto de movimiento
	 * @param id			: Id del producto a eliminar
	 * @throws Throwable	: Excepcion
	 */
	 void removeMovimiento(Long id) throws Throwable; 
	
	/**
	 * Elimina un producto de acumulacion
	 * @param id			: Id del producto a eliminar
	 * @param actualiza		: Indica si debe actualiza o eliminar. true = actualiza.
	 * @throws Throwable	: Excepcion
	 */
	 void removeAcumulacion(Long id, boolean actualiza) throws Throwable;
	 
	 /**
	 * Metodo que recupera el numero de padre del producto
	 * @param id			: Id del producto a buscar el padre
	 * @return				: Id del padre
	 * @throws Throwable	: Excepcion
	 */
	 Long recuperaIdPadre(Long id) throws Throwable;
	 
	 /**
	 * Cambia de tipo el producto
	 * @param id			: Id del producto a cambiar el tipo 
	 * @param tipo			: Tipo que se cambiara
	 * @throws Throwable	: Excepcion
	 */
	 void actualizaTipoProducto(Long id, int tipo) throws Throwable;
	 
	 /**
	 * Cambia de estado del producto
	 * @param id			: Id del producto a cambiar el tipo 
	 * @param tipo			: Tipo que se cambiara
	 * @throws Throwable	: Excepcion
	 */
	 void actualizaEstadoProducto(Long id, int tipo) throws Throwable;
	 
	 /**
	 * Recupera el listado de productos hijo de una producto
	 * @param id			: Id del padre
	 * @return				: Listado de producto hijo
	 * @throws Throwable	: Excepcion
	 */
	 List<ProductoPago> recuperaProductosHijo(Long id) throws Throwable;
	 
	 /**
	 * Metodo que recupera el siguiente numero de producto a ingresar
	 * @param id			: Id del producto en la que se va a insertar
	 * @return				: String de la siguiente cuenta hija
	 * @throws Throwable	: Excepcion
	 */
	 String recuperaSiguienteHijo(Long id) throws Throwable;
	 
	 /**
	 * Busca el objeto que contiene el mayor numero de producto
	 * @param listadoCuenta	: Listado de los productos a buscar
	 * @return				: Objeto con la mayor cuenta
	 * @throws Throwable	: Excepcion
	 */
	 ProductoPago buscaMayorNumeroProducto(List<ProductoPago> listadoProducto) throws Throwable;
	 
}