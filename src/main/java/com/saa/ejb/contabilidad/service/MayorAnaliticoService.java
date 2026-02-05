package com.saa.ejb.contabilidad.service;

import java.time.LocalDate;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.MayorAnalitico;

import jakarta.ejb.Local;

@Local
public interface MayorAnaliticoService extends EntityService<MayorAnalitico> {
	
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	 MayorAnalitico selectById(Long id) throws Throwable;
	  
	  /**
	  * Procesa el reporte de mayor analítico de acuerdo a los parámetros y devuelve el id de ejecución
	  * @param empresa			: Id de empresa
	  * @param fechaInicio		: Fecha de inicio del reporte
	  * @param fechaFin			: Fecha de fin de reporte
	  * @param cuentaInicio		: Cuenta contable inicio del reporte
	  * @param cuentaFin		: Cuenta contable fin del reporte
	  * @param conCentro		: Indica si el reporte es por centro de costo. 0 = sin centro de costo, 1 = con centro de costo
	  * @param tipoDistribucion	: Indica el tipo de distribución. 0 = Sin centroCosto, 1 = Centro de costo por cuenta contable. 2 = Cta contable por centro de costo
	  * @param centroInicio		: Centro de costo inicio del reporte
	  * @param centroFin		: Centro de costo fin del reporte
	  * @param tipoAcumulacion	: Tipo de acumulación. 1 = Acumulado, 0 = Sin acumular. Saldo anterior = 0
	  * @return					: Id de ejecución del reporte con el que se realiza la búsqueda en las entidades
	  */
	 Long generaReporte(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, 
			 			String cuentaInicio, String cuentaFin, int tipoDistribucion, 
			 			String centroInicio, String centroFin, int tipoAcumulacion) throws Throwable;
	  
	/**
	  * Ingresa el detalle del mayor analitico cuando no tiene centro de costo
	  * @param secuenciaReporte	: Id de la secuencia de ejecucion del reporte
	  * @param empresa			: Id de empresa
	  * @param fechaInicio		: Fecha de inicio del reporte
	  * @param fechaFin			: Fecha de fin de reporte
	  * @throws Throwable		: Exception
	  */
	void insertaDetalleSinCentro(Long secuenciaReporte, Long empresa, LocalDate fechaInicio, 
			LocalDate fechaFin) throws Throwable;
	
	/**
	  * Ingresa la cabecera del mayor analitico cuando es centro de costo por plan de cuenta
	  * @param secuenciaReporte	: Id de la secuencia de ejecucion del reporte
	  * @param empresa			: Id de empresa
	  * @param fechaInicio		: Fecha de inicio del reporte
	  * @param fechaFin			: Fecha de fin de reporte
	  * @param cuentaInicio		: Cuenta contable inicio del reporte
	  * @param cuentaFin		: Cuenta contable fin del reporte
	  * @param centroInicio		: Centro de costo inicio del reporte
	  * @param centroFin		: Centro de costo fin del reporte
	  * @param tipoAcumulacion	: Tipo de acumulación. 1 = Acumulado, 0 = Sin acumular. Saldo anterior = 0
	  * @param tipoDistribucion	: Indica el tipo de distribución. 0 = Sin centroCosto, 1 = Centro de costo por cuenta contable. 2 = Cta contable por centro de costo
	  * @throws Throwable		: Exception
	  */
	void insertaCabeceraPorDistribucion(Long secuenciaReporte, Long empresa, LocalDate fechaInicio, 
								  LocalDate fechaFin, String cuentaInicio, String cuentaFin, 
			  					  String centroInicio, String centroFin, int tipoAcumulacion,
			  					  int tipoDistribucion) throws Throwable;
	
	/**
	  * Ingresa el detalle del mayor analitico de centro de costo por plan de cuenta
	  * @param secuenciaReporte	: Id de la secuencia de ejecucion del reporte
	  * @param empresa			: Id de empresa
	  * @param fechaInicio		: Fecha de inicio del reporte
	  * @param fechaFin			: Fecha de fin de reporte
	  * @param centroInicio		: Centro de costo inicio del reporte
	  * @param centroFin		: Centro de costo fin del reporte
	  * @throws Throwable		: Exception
	  */
	void insertaDetalleCentroPorPlan(Long secuenciaReporte, Long empresa, LocalDate fechaInicio, 
			LocalDate fechaFin, String centroInicio, String centroFin) throws Throwable;
	
	/**
	  * Ingresa la cabecera del mayor analitico cuando es centro de costo por plan de cuenta
	  * @param secuenciaReporte	: Id de la secuencia de ejecucion del reporte
	  * @param empresa			: Id de empresa
	  * @param fechaInicio		: Fecha de inicio del reporte
	  * @param fechaFin			: Fecha de fin de reporte
	  * @param cuentaInicio		: Cuenta contable inicio del reporte
	  * @param cuentaFin		: Cuenta contable fin del reporte
	  * @param centroInicio		: Centro de costo inicio del reporte
	  * @param centroFin		: Centro de costo fin del reporte
	  * @param tipoAcumulacion	: Tipo de acumulación. 1 = Acumulado, 0 = Sin acumular. Saldo anterior = 0
	  * @throws Throwable		: Exception
	  */
	void insertaCabeceraPorCentro(Long secuenciaReporte, Long empresa, LocalDate fechaInicio, 
								  LocalDate fechaFin, String cuentaInicio, String cuentaFin, 
			  					  String centroInicio, String centroFin, int tipoAcumulacion) throws Throwable;
	
	/**
	  * Ingresa el detalle del mayor analitico de centro de costo por plan de cuenta
	  * @param secuenciaReporte	: Id de la secuencia de ejecucion del reporte
	  * @param empresa			: Id de empresa
	  * @param fechaInicio		: Fecha de inicio del reporte
	  * @param fechaFin			: Fecha de fin de reporte
	  * @param cuentaInicio		: Cuenta contable inicio del reporte
	  * @param cuentaFin		: Cuenta contable fin del reporte
	  * @throws Throwable		: Exception
	  */
	void insertaDetallePlanPorCentro(Long secuenciaReporte, Long empresa, LocalDate fechaInicio, 
			LocalDate fechaFin, String cuentaInicio, String cuentaFin) throws Throwable;
	
	/**
	 * Elimina los registros de mayor analitico una vez impreso
	 * @param idMayorAnalitico	: Id de mayor analitico
	 * @throws Throwable		: Excepcion
	 */
	void eliminaConsultasAnteriores(Long idMayorAnalitico)throws Throwable;
	
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
	Long selectPeriodosMayorizadoNoMayorizado(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, Long estado1, Long estado2, Long estado3)throws Throwable;

}
