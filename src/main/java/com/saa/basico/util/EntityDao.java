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
 *         <p>
 *         Interfaz que contiene los metodos DAO gen�ricos para cada entidad.
 *         Es la base para todos los servicios DAO de las entidades.
 *         </p>
 */
public interface EntityDao<Tipo> {
	/**
	 * Devuelve el listado de campos que tiene la tabla
	 * 
	 * @return: Arreglo de String con listado de campos que tiene la tabla
	 */
	String[] obtieneCampos();

	/**
	 * @param entidad Nombre de la entidad a recuperar
	 * @return :Listado de objetos encontrados
	 * @throws Throwable :Excepcion en caso de error
	 */
	List<Tipo> selectAll(String entidad) throws Throwable;

	/**
	 * @param tipo :Tipo de clase a almacenar
	 * @param id   :Codigo de la entidad.
	 * @throws Throwable :Excepcion en caso de error
	 */
	Tipo save(Tipo tipo, Long id) throws Throwable;

	/**
	 * @param clase Tipo de clase a buscar
	 * @param id    :Id de la clase a buscar
	 * @return :Objeto encontrado
	 * @throws Throwable :Excepcion en caso de error
	 */
	Tipo find(Tipo clase, Long id) throws Throwable;

	/**
	 * @category Sirve para eliminar un registro
	 * @param :clase Tipo de clase a eliminar
	 * @param id     :id del registro a eliminar
	 * @throws Throwable :Excepcion en caso de que ocurra un error
	 * 
	 */
	void remove(Tipo clase, Long id) throws Throwable;

	/**
	 * Devuelve una instancia recuperada por el id
	 * 
	 * @param id      : Id de la instancia
	 * @param entidad : Nombre de la entidad
	 * @return : Instancia recuperada
	 * @throws Throwable: Excepcion
	 */
	Tipo selectById(Long id, String entidad) throws Throwable;

	/**
	 * Select que permite realizar busquedas por cualquier campo de la entidad. Se
	 * puede utilizar cualquier
	 * comparador dependiendo de la manera como se parametrice el listado de la
	 * clase DatosBusqueda
	 * 
	 * @param datos         : Arreglos con los datos
	 * @param nombreEntidad : Nombre de la entidad en la que se realiza la busqueda
	 * @return : Listado de registros recuperados
	 * @throws Throwable : Excepcion
	 */
	List<Tipo> selectByCriteria(List<DatosBusqueda> datos, String nombreEntidad) throws Throwable;

}
