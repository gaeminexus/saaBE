/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;
import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.MayorAnalitico;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad MayorAnalitico.
 */
@Local
public interface MayorAnaliticoDaoService extends EntityDao<MayorAnalitico> {

	/**
	 * Obtiene una Secuencia del Reporte
	 * @return			: Numero se Secuencia
	 * @throws Throwable: Excepcions
	 */
	Long obtieneSecuenciaReporte()throws Throwable;
	
	/**
	 * Recupera los registros por secuencial de ejecucion del reporte
	 * @param secuencia	: Id del secuencial de ejecucion del reporte
	 * @return			: Listado de registros de la entidad de mayor analitico
	 * @throws Throwable: Excepcion
	 */
	List<MayorAnalitico> selectBySecuencia(Long secuencia) throws Throwable;
	
	/**
	 * Recupera un conteo de del periodo por empresa fechaInicio 
	 * @param empresa		: Id de la empresa
	 * @param fechaInicio	: Fecha Inicio
	 * @param periodoInicio	: Periodo Inicio
	 * @param periodoFin	: Periodo Fin
	 * @param estado		: Estado
	 * @return				: Conteo 
	 * @throws Throwable	: Excepciones
	 */
	Long selectPeriodosMayorizadoNoMayorizado(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, 
											  Long estado1, Long estado2, 
											  Long estado3)throws Throwable;
	
}
