package com.saa.ejb.contabilidad.service;
import java.time.LocalDate;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.DetalleMayorAnalitico;
import com.saa.model.cnt.MayorAnalitico;

import jakarta.ejb.Local;

@Local
public interface DetalleMayorAnaliticoService extends EntityService <DetalleMayorAnalitico> {
	
	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  DetalleMayorAnalitico selectById(Long id) throws Throwable;
	  
	  /**
	  * Ingresa el detalle del mayor analitico cuando no tiene centro de costo
	  * @param mayor		: Cabecera del reporte de mayor analitico
	  * @param fechaInicio	: Fecha de inicio del reporte
	  * @param fechaFin		: Fecha de fin de reporte
	  * @throws Throwable	: Exception
	  */
	 void insertaDetalleSinCentro(MayorAnalitico mayor, LocalDate fechaInicio, 
			 LocalDate fechaFin) throws Throwable;
	 
	 /**
	  * Ingresa el detalle del mayor analitico de centro de costo por plan de cuenta
	  * @param mayor		: Cabecera del reporte de mayor analitico 
	  * @param fechaInicio		: Fecha de inicio del reporte
	  * @param fechaFin			: Fecha de fin de reporte
	  * @param centroInicio		: Centro de costo inicio del reporte
	  * @param centroFin		: Centro de costo fin del reporte
	  * @throws Throwable		: Exception
	  */
	void insertaDetalleCentroPorPlan(MayorAnalitico mayor, LocalDate fechaInicio, 
			LocalDate fechaFin, String centroInicio, String centroFin) throws Throwable;
	
	/**
	  * Ingresa el detalle del mayor analitico de plan de cuenta por centro de costo
	  * @param mayor		: Cabecera del reporte de mayor analitico 
	  * @param fechaInicio		: Fecha de inicio del reporte
	  * @param fechaFin			: Fecha de fin de reporte
	  * @param cuentaInicio		: Cuenta contable inicio del reporte
	  * @param cuentaFin		: Cuenta contable fin del reporte
	  * @throws Throwable		: Exception
	  */
	void insertaDetallePlanPorCentro(MayorAnalitico mayor, LocalDate fechaInicio, 
			LocalDate fechaFin, String cuentaInicio, String cuentaFin) throws Throwable;
	
	/**
	 * Elimina un registro por id
	 * @param id		: Id del registro a eliminar
	 * @throws Throwable: Excepcion
	 */
	void remove(Long id) throws Throwable;

}
