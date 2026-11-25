package com.saa.ejb.cnt.service;

import java.time.LocalDate;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.TempReportes;

import jakarta.ejb.Local;

@Local
public interface TempReportesService extends EntityService<TempReportes>  {
	

	 /**
	 * Elimina todas las cuentas de una secuencia de ejecucion
	 * @param codigo		: Id de ejecucion del reporte 
	 * @throws Throwable	: Excepcion
	 */
	 void removeBySecuencia (Long codigo) throws Throwable;
	
	/**
	 * Recupera entidad con el id
	 * @param id		: Id de la entidad
	 * @return			: Recupera entidad
	 * @throws Throwable: Excepcion
	 */
	 TempReportes selectById(Long id) throws Throwable;	
	
	/**
	 * Inserta cuentas contables de empresa que se encuentran en la parametrizacion
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: Cuenta Fin
	 * @param idEjecucion	: Id de Ejecucion del Reporte
	 * @throws Throwable	: Excepcion
	 */
	void insertaCuentas (Long empresa, String cuentaInicio, String cuentaFin, Long idEjecucion) throws Throwable;
	
	/**
	 * Actualiza Movimientos Debe Haber de cada Cuenta Contable de DTMT
	 * @param empresa		: Id de la empresa
	 * @param fechaInicio	: Fecha Inicio
	 * @param fechaFin		: Fecha Fin
	 * @param idEjecucion	: Id de la Ejecucion
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @throws Throwable	: Excepcions
	 */
	void actualizaDebeHaberMovimiento(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, Long idEjecucion
			  , int acumulacion) throws Throwable;
	
	/**
	 * Actualiza las Cuentas Padres con los valores del Debe Haber Saldos
	 * @param idEjecucion	: Id de la Ejecucion
	 * @throws Throwable	: Excepcions
	 */
	void actualizaSaldosAcumulacion(Long idEjecucion)throws Throwable;
	
	/** 
	 * Procedimiento Reporte de Balances por Rango de Fechas
	 * @param fechaFin		: Fecha Fin 
	 * @param fechaInicio	: Fecha Inicio 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @return				: Id de Ejecucion 
	 * @throws Throwable	: Excepcions
	 */
	Long reporteRangoFecha (LocalDate fechaFin, LocalDate fechaInicio, Long empresa, Long codigoAlterno, int acumulacion) throws Throwable;

	/**
	 * Metodo para copiar Plan desde la Parametrizacion 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno
	 * @param idEjecucion	: Id de la Ejecucion del Reporte
	 * @throws Throwable	: Excepcions
	 */
	void copiaPlanParametrizacion (Long empresa, Long codigoAlterno, Long idEjecucion)throws Throwable;
	
	/** 
	 * Procedimiento Reporte de Balances por Rango de Fechas que incluye los centros de costo
	 * @param fechaFin		: Fecha Fin 
	 * @param fechaInicio	: Fecha Inicio 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @return				: Id de Ejecucion 
	 * @throws Throwable	: Excepcions
	 */
	Long reporteCentroRangoFecha (LocalDate fechaFin, LocalDate fechaInicio, Long empresa, Long codigoAlterno, int acumulacion) throws Throwable;
	
	/**
	 * Metodo para copiar Plan con datos de centro de costo desde la Parametrizacion 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno
	 * @param idEjecucion	: Id de la Ejecucion del Reporte
	 * @throws Throwable	: Excepcions
	 */
	void copiaPlanCentroParametrizacion (Long empresa, Long codigoAlterno, Long idEjecucion)throws Throwable;
	
	/**
	 * Inserta cuentas contables de empresa parametrizadas por centro de costo
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: Cuenta Fin
	 * @param idEjecucion	: Id de Ejecucion del Reporte
	 * @throws Throwable	: Excepcion
	 */
	void insertaCuentasByCentro (Long empresa, String cuentaInicio, String cuentaFin, Long idEjecucion) throws Throwable;
	
	/**
	 * Actualiza Movimientos Debe Haber de cada Cuenta Contable por centro de costo de DTMT 
	 * @param empresa		: Id de la empresa
	 * @param fechaInicio	: Fecha Inicio
	 * @param fechaFin		: Fecha Fin
	 * @param idEjecucion	: Id de la Ejecucion 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @throws Throwable	: Excepcions
	 */
	void actualizaDebeHaberMovimientoCentro(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, Long idEjecucion
			  , int acumulacion) throws Throwable;

	/**
	 * Actualiza las Cuentas Padres con los valores del Debe Haber Saldos
	 * @param idEjecucion	: Id de la Ejecucion
	 * @throws Throwable	: Excepcions
	 */
	void actualizaSaldosAcumulacionCentro(Long idEjecucion)throws Throwable;
	
	/** 
	 * Procedimiento Reporte de Balances por Rango de Fechas distribuido por plan de cuentas por centro de costo
	 * @param fechaFin		: Fecha Fin 
	 * @param fechaInicio	: Fecha Inicio 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @return				: Id de Ejecucion 
	 * @throws Throwable	: Excepcions
	 */
	Long reportePlanCentroRangoFecha (LocalDate fechaFin, LocalDate fechaInicio, Long empresa, Long codigoAlterno, int acumulacion) throws Throwable;
	
	/**
	 * Metodo para copiar centro de costo desde la Parametrizacion para el reporte distribuido de Plan de cuentas por centro de costo
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno
	 * @param idEjecucion	: Id de la Ejecucion del Reporte
	 * @throws Throwable	: Excepcions
	 */
	void copiaPlanByCentro (Long empresa, Long codigoAlterno, Long idEjecucion)throws Throwable;
	
	/**
	 * Inserta centros de costo de empresa parametrizados por cuenta contable
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: Cuenta Fin
	 * @param idEjecucion	: Id de Ejecucion del Reporte
	 * @throws Throwable	: Excepcion
	 */
	void insertaCentroByCuentas(Long empresa, String cuentaInicio, String cuentaFin, Long idEjecucion) throws Throwable;
	
	/**
	 * Actualiza Movimientos Debe Haber de cada Centro de costo por cuenta contable
	 * @param empresa		: Id de la empresa
	 * @param fechaInicio	: Fecha Inicio
	 * @param fechaFin		: Fecha Fin
	 * @param idEjecucion	: Id de la Ejecucion 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @throws Throwable	: Excepcions
	 */
	void actualizaDebeHaberMovimientoPlanByCentro(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, Long idEjecucion
			  , int acumulacion) throws Throwable;
	
	/**
	 * Elimina todos los movimientos del reporte que esten en cero
	 * @param idEjecucion	: Id de ejecución del reporte
	 * @throws Throwable	: Excepcion
	 */
	void eliminaMovimientosEnCero(Long idEjecucion) throws Throwable;
	

	// ============================ C19b (D-031): reportes de renglones propios ============================

	/** true si el tipo de reporte (empresa, alterno) es de renglones propios (RPRTTPRP = 2). */
	boolean esReporteDeRenglones(Long empresa, Long codigoAlterno) throws Throwable;

	/**
	 * Calcula un reporte de renglones propios (B17, flujo de caja): renglones vigentes de CNT.RNRP, reglas vigentes de
	 * CNT.DTRP, saldos por cuenta segun la base de cada renglon, comparativo opcional y, si el reporte tiene salida en
	 * archivo, su nombre y contenido. Si el reporte se presenta a la Superintendencia exige el mes cerrado (C17,
	 * REPORTE_MES_NO_CERRADO) salvo {@code preliminar = true}, y avisa las cuentas con saldo negativo que la
	 * Superintendencia rechaza (C39, advertencias «SALDO NEGATIVO»). El reporte se elige por {@code codigoAlterno} o,
	 * si no viene, por {@code codigoEstructura} (B17). Rechaza los formatos derivados del B17 (C38: FLE, FCD, CMP).
	 */
	com.saa.ejb.cnt.estructura.ResultadoEstructura calculaRenglones(com.saa.model.cnt.ParametrosBalance parametros) throws Throwable;

	/** Escribe el resultado de {@link #calculaRenglones} en CNT.DTMT como una ejecucion del motor; devuelve su id. */
	Long reporteRenglones(com.saa.ejb.cnt.estructura.ResultadoEstructura resultado) throws Throwable;

	/**
	 * Cabecera de un reporte de renglones propios activo por su codigo de estructura (RPRTCDES: B17, FLE, FCD, CMP);
	 * lanza si no existe (C38, antes EstructuraReporteService.definicionEstructura).
	 */
	com.saa.ejb.cnt.estructura.DefinicionEstructura definicionRenglones(Long empresa, String codigoEstructura) throws Throwable;

	/** Renglones del reporte vigentes a la fecha de corte (CNT.RNRP), en su orden. */
	java.util.List<com.saa.ejb.cnt.estructura.RenglonEstructura> renglonesVigentes(Long codigoReporte, LocalDate corte) throws Throwable;

	/** Reglas cuenta -> renglon del reporte vigentes a la fecha de corte (CNT.DTRP). */
	java.util.List<com.saa.ejb.cnt.estructura.ReglaCuentaRenglon> reglasVigentes(Long codigoReporte, LocalDate corte) throws Throwable;

}
