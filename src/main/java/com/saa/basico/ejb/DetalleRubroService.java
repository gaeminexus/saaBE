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
package com.saa.basico.ejb;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.scp.DetalleRubro;

/**
 * @author GaemiSoft.
 *
 *         <p>
 *         Servicio para la entidad Rubro.
 *         Accede a los metodos DAO y procesa los datos para el cliente.
 *         </p>
 */
public interface DetalleRubroService extends EntityService<DetalleRubro> {
	/**
	 * Recupera entidad con el id
	 * 
	 * @param id : Id de la entidad
	 * @return : Recupera entidad
	 * @throws Throwable: Excepcion
	 */
	DetalleRubro selectById(Long id) throws Throwable;
	
	
	 /**
	 * Recupera los módulos que no tiene el cliente excepto el modulo de contabilidad. Esto
	 *  para el combo de módulos de la pantalla de ingresos de asientos manual.
	 * @return						: Arreglo de objetos con los codigos alternos y los valores
	 * @throws Throwable			: Excepcion
	 */
	List<DetalleRubro> comboModulos() throws Throwable;

	/**
	 * Recupera el valor del detalle de rubro por el codigo alterno del rubro y del
	 * detalle de rubro
	 * 
	 * @param codigoAlternoRubro   : Codigo alterno de rubro
	 * @param codigoAlternoDetalle : Codigo alterno de detalle de rubro
	 * @return : Valor del campo de valor alfanumerico del detalle de rubro
	 * @throws Throwable : Excepcion
	 */
	String selectValorStringByRubAltDetAlt(int codigoAlternoRubro, int codigoAlternoDetalle) throws Throwable;

	/**
	 * Recupera el listado de detalles de rubros por codigo alterno de rubro
	 * 
	 * @param codigoAlternoRubro : Codigo alterno de rubro
	 * @return : Valor del campo de valor alfanumerico del detalle de rubro
	 * @throws Throwable : Excepcion
	 */
	List<DetalleRubro> selectByCodigoAlternoRubro(int codigoAlternoRubro) throws Throwable;

	/**
	 * Recupera los registros con los campos que se indiquen en el arreglo campos
	 * filtrados por el codigo alterno del rubro
	 * 
	 * @param campos              : Arreglo que contiene el listado de campos a
	 *                            recuperar
	 * @param codigoAlternoRubro: Codigo alterno del rubro
	 * @return : Arreglo de objetos con el resultado del select
	 * @throws Throwable : Excepcion
	 */
	List<DetalleRubro> selectCamposByRubroAlterno(Object[] campos, int codigoAlternoRubro) throws Throwable;

}
