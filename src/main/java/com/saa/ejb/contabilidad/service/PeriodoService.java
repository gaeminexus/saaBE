package com.saa.ejb.contabilidad.service;

import java.util.Date;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.Periodo;

import jakarta.ejb.Local;

@Local
public interface PeriodoService extends EntityService<Periodo> {

 	
	/**
	 * Método que verifica el numero de registros en una empresa
	 * @param empresa		:Id de la empresa
	 * @return numero de cuentas contables en la empresa
	 * @throws Throwable	:excepcion
	 */
	 int numeroRegistrosEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Genera periodos contables de forma automatica
	 * @param empresa		: Id de la empresa en la que se genera el periodo
	 * @param numeroPeriodo	: Numero de periodos a generar
	 * @param mes			: Numero de mes inicio
	 * @param anio			: Anio inicio
	 * @throws Throwable	: Excepcion
	 */
	 void save(Long empresa, int numeroPeriodo, int mes, int anio) throws Throwable;
	
	/**
	 * Recupera los Periodos de una empresa
	 * @param empresa	: Id de la empresa en la que se realiza la busqueda
	 * @return			: Arreglo de objetos con los usuarios
	 * @throws Throwable: Excepcion
	 */
	 Object[][] selectPeriodo (Long empresa)throws Throwable;
	
	/**
	 * Obtiene el periodo inicial para Mayorizacion y final para Desmayorizacion
	 * @param proceso		: Indica el proceso a realizar. 1 = Mayorizar, 2 = Desmayorizar
	 * @param empresa		: Id de la empresa
	 * @return				: Id del periodo resultado
	 * @throws Throwable	: Excepcion
	 */
	 Long periodoMayorizacionDesmayorizacion(int proceso, Long empresa) throws Throwable;
	
	/**
	 * Verifica si existen periodos de cierre en un rango de periodos de una empresa
	 * @param empresa		: Id de la empresa
	 * @param periodoInicia	: Periodo de inicio del rango
	 * @param periodoFin	: Periodo de fin del rango
	 * @param proceso		: Indica el proceso a realizar. 1 = Mayorizar, 2 = Desmayorizar
	 * @return				: True or false
	 * @throws Throwable	: Excepcion
	 */
	 Boolean periodoCierreRango(Long empresa, Long periodoInicia, Long periodoFin, int proceso) throws Throwable;	
	
	/**
	 * Obtiene el anterior periodo mayorizado a un periodo
	 * @param periodoCodigo	: Id del periodo desde el que se busca
	 * @param empresa		: Id de la empresa
	 * @return				: Periodo mayorizado
	 * @throws Throwable	: Exception
	 */
	 Periodo anteriorMayorizado(Long periodoCodigo, Long empresa) throws Throwable;
	
	/**
	 * Obtiene el anterior periodo a un periodo de una empresa
	 * @param periodoCodigo	: Id del periodo desde el que se busca
	 * @param empresa		: Id de la empresa
	 * @return				: Periodo anterior
	 * @throws Throwable	: Exception
	 */
	 Periodo buscaAnterior(Long periodoCodigo, Long empresa) throws Throwable;
	
	/**
	 * Actualiza el estado de un periodo, y registra la mayorizacion o desmayorizacion segun el proceso realizado
	 * @param periodo		: Periodo a actualizar
	 * @param codigoProceso	: Id del proceso realizado
	 * @param proceso		: Entero que indica el proceso que se realiza
	 * @throws Throwable	: Excepcion
	 */
	 void actualizaEstadoProceso(Periodo periodo, Long codigoProceso, int proceso) throws Throwable;
	
	/**
	 * Recupera los anios de una empresa
	 * @param empresa		: Id de la Empresa	
	 * @return				: Anios 
	 * @throws Throwable	: Excepcion
	 */
	 List<Long> recuperaAnios (Long empresa) throws Throwable;
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	 Periodo selectById(Long id) throws Throwable;
	
	/**
	 * Recupera un periodo con empresa, mes y año
	 * @param empresa	: Id de empresa
	 * @param mes		: Numero de mes
	 * @param anio		: Numero de año
	 * @return			: Estado recuperado
	 * @throws Throwable: Excepcion
	 */
	 Periodo recuperaByMesAnioEmpresa(Long empresa, Long mes, Long anio) throws Throwable;

	/**
	 * Verifica el si un periodo se puede eliminar o no
	 * @param empresa : Id de la empresa
	 * @param idPeriodo : Id de periodo
	 * @return			: True / False  
	 * @throws Throwable: Excepcion
	 */
	 boolean verificaDetallePeriodo(Long empresa ,Long idPeriodo) throws Throwable;
	
	/**
	 * Recupera la informacion del ultimo periodo
	 * @param empresa	: Id de la empresa
	 * @return			: Periodo recuperado
	 * @throws Throwable: Excepcion
	 */
	 Periodo recuperaInformacionUltimoPeriodo(Long empresa) throws Throwable;
	 
	/**
	 * Proceso que devuelve el maximo periodo en cualquier estado anterior a una fecha
	 * @param empresa	: Id de la Empresa 
	 * @param estado	: Estado de periodo a buscar
	 * @param fechaFin	: Fecha a la que se desea buscar el periodo
	 * @return			: Id del Periodo
	 * @throws Throwable: Excepcions
	 */
	Periodo obtieneMaximoPeriodoFechaEstado(Long empresa, int estado, Date fecha)throws Throwable;
	
	/**
	 * Proceso que devuelve el minimo periodo en cualquier estado anterior a una fecha
	 * @param empresa	: Id de la Empresa 
	 * @param estado	: Estado de periodo a buscar
	 * @param fechaFin	: Fecha a la que se desea buscar el periodo
	 * @return			: Id del Periodo
	 * @throws Throwable: Excepcions
	 */
	Periodo obtieneMinimoPeriodoFechaEstado(Long empresa, int estado, Date fecha)throws Throwable;
	
	/**
	 * Valida que un periodo se encuentre mayorizado
	 * @param idPeriodo : Id de periodo
	 * @return			: True / False  
	 * @throws Throwable: Excepcion
	 */
	 boolean verificaPeriodoMayorizado(Long idPeriodo) throws Throwable;
	 
	/**
	 * Valida que un periodo se encuentre mayorizado
	 * @param fechaSistema	: Fecha para encontrar el periodo y verificarlo
	 * @param idEmpresa		: Id de la empresa
	 * @return				: Verdadero o Falso
	 * @throws Throwable	: Excepcion
	 */
	boolean verificaPeriodoMayorizadoByFecha(Date fechaSistema, Long idEmpresa ) throws Throwable;
	
}
