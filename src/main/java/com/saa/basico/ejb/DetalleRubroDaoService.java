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
import com.saa.basico.util.EntityDao;
import com.saa.model.scp.DetalleRubro;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Clase DetalleRubroDao.
 */
@Local
public interface DetalleRubroDaoService extends EntityDao<DetalleRubro> {

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
	 * Recupera los m�dulos que no tiene el cliente excepto el modulo de
	 * contabilidad. Esto
	 * para el combo de m�dulos de la pantalla de ingresos de asientos manual.
	 * 
	 * @param codigoAlternoRubro   : Codigo alterno de rubro
	 * @param codigoAlternoDetalle : Codigo alterno de detalle de rubro
	 * @return : Valor del campo de valor alfanumerico del detalle de rubro
	 * @throws Throwable : Excepcion
	 */
	List<DetalleRubro> selectModulosNoClienteConContabilidad() throws Throwable;

	/**
	 * Recupera el listado de detalles de rubros por codigo alterno de rubro
	 * 
	 * @param codigoAlternoRubro : Codigo alterno de rubro
	 * @return : Valor del campo de valor alfanumerico del detalle de rubro
	 * @throws Throwable : Excepcion
	 */
	List<DetalleRubro> selectByCodigoAlternoRubro(int codigoAlternoRubro, Long estado) throws Throwable;

	/**
	 * Recupera la descripcion del detalle de rubro por el codigo alterno del rubro
	 * y del detalle de rubro
	 * 
	 * @param codigoAlternoRubro   : Codigo alterno de rubro
	 * @param codigoAlternoDetalle : Codigo alterno de detalle de rubro
	 * @return : descripcion del detalle de rubro
	 * @throws Throwable : Excepcion
	 */
	String selectDescripcionByRubAltDetAlt(int codigoAlternoRubro, int codigoAlternoDetalle) throws Throwable;

	/**
	 * Recupera el valor numerico del detalle de rubro por el codigo alterno del
	 * rubro y del detalle de rubro
	 * 
	 * @param codigoAlternoRubro   : Codigo alterno de rubro
	 * @param codigoAlternoDetalle : Codigo alterno de detalle de rubro
	 * @return : Valor del campo de valor alfanumerico del detalle de rubro
	 * @throws Throwable : Excepcion
	 */
	Double selectValorNumericoByRubAltDetAlt(int codigoAlternoRubro, int codigoAlternoDetalle) throws Throwable;

	/**
	 * @param tipo :DetalleRubro de clase a almacenar
	 * @throws Throwable :Excepcion en caso de error
	 */
	DetalleRubro save(DetalleRubro tipo) throws Throwable;

}
