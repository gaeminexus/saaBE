/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.util;

import java.util.List;

/**
 * @author GaemiSoft.
 *
 *         Clase Servicio de la Entidad.
 */
public interface EntityService<Tipo> {

	/**
	 * Metodo que borra registros
	 * 
	 * @param id : Listado de claves de objetos a eliminar
	 * @throws Throwable : Excepcion en caso de error
	 */
	void remove(List<Long> id) throws Throwable;

	/**
	 * Metodo que graba registros
	 * 
	 * @param object :Arreglo de objetos a persistir
	 * @throws Throwable :Excepcion en caso de error
	 */
	void save(List<Tipo> object) throws Throwable;

	/**
	 * Metodo que graba registros
	 * 
	 * @param object :Arreglo de objetos a persistir
	 * @throws Throwable :Excepcion en caso de error
	 */
	Tipo saveSingle(Tipo object) throws Throwable;

	/**
	 * Metodo que recupera todos los registros
	 * 
	 * @return :Arreglo de objetos recuperados para la capa web
	 * @throws Throwable :Excepcion en caso de error
	 */
	List<Tipo> selectAll() throws Throwable;

	/**
	 * Recupera entidad con el id
	 * 
	 * @param id : Id de la entidad
	 * @return : Recupera entidad
	 * @throws Throwable: Excepcion
	 */
	Tipo selectById(Long id) throws Throwable;

	/**
	 * Select que permite realizar busquedas por cualquier campo de la entidad. Se
	 * puede utilizar cualquier
	 * comparador dependiendo de la manera como se parametrice el listado de la
	 * clase DatosBusqueda. Devuelve los
	 * campos listados en el objeto campos.
	 * 
	 * @param campos : Arreglos con campos que se desea recuperar
	 * @param datos  : Arreglos con los datos
	 * @return : Listado de registros recuperados
	 * @throws Throwable : Excepcion
	 */
	List<Tipo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable;

	/**
	 * Elimina uno o varios registros de Exter.
	 */

}
