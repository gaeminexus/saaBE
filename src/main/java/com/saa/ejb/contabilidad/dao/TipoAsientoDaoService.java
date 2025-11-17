/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.TipoAsiento;
import jakarta.ejb.Remote;
 
/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad TipoAsiento.
 */
@Remote
public interface TipoAsientoDaoService  extends EntityDao<TipoAsiento>  {

	/**
	 * Recupera una instancia por el codigo alterno y la empresa
	 * @param alterno	: Codigo alterno
	 * @param empresa	: Id de la empresa
	 * @return			: Registro
	 * @throws Throwable: Excepcion
	 */
	List<TipoAsiento> selectByAlterno(int alterno, Long empresa) throws Throwable;
	
	/**
	 * Recupera los tipos de asientos que no tienen codigo alterno
	 * @param empresa	: Id de la empresa
	 * @return			: Listado de tipos de asiento sin alterno
	 * @throws Throwable: Excepcion
	 */
	List<TipoAsiento> selectByEmpresaSinAlterno(Long empresa) throws Throwable;
}