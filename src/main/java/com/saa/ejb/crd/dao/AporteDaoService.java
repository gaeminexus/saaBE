package com.saa.ejb.crd.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Aporte;

import jakarta.ejb.Local;

@Local
public interface AporteDaoService extends EntityDao<Aporte>{

	/**
	 * Para G42 — Grupo 1: Suma de aportes (tipoAporte.estado=1, tipoAporte.codigoSBS='RE')
	 * agrupada por entidad, con fechaTransaccion <= fechaCorte.
	 * Retorna Object[]{Long codigoEntidad, Double suma}.
	 */
	List<Object[]> selectSumaRendimientoPorEntidad(LocalDateTime fechaCorte) throws Throwable;

	/**
	 * Para G42 — Grupo 2: Suma de aportes (tipoAporte.estado=1, tipoAporte.codigo IN(3,13,14))
	 * agrupada por entidad, con fechaTransaccion <= fechaCorte.
	 * Retorna Object[]{Long codigoEntidad, Double suma}.
	 */
	List<Object[]> selectSumaPatronalPorEntidad(LocalDateTime fechaCorte) throws Throwable;

	/**
	 * Para G42 — Grupo 3: Suma de aportes (tipoAporte.estado=1) que NO sean codigoSBS='RE'
	 * ni codigo IN(3,13,14), agrupada por entidad, con fechaTransaccion <= fechaCorte.
	 * Retorna Object[]{Long codigoEntidad, Double suma}.
	 */
	List<Object[]> selectSumaPersonalPorEntidad(LocalDateTime fechaCorte) throws Throwable;

	/**
	 * Para G44 — Imposiciones acumuladas: COUNT de aportes con tipoAporte.codigo IN (9, 11)
	 * agrupado por entidad, con fechaTransaccion <= fechaCorte.
	 * Retorna Object[]{Long codigoEntidad, Long count}.
	 */
	List<Object[]> selectCountImposicionesJubilacionPorEntidad(LocalDateTime fechaCorte) throws Throwable;

	/**
	 * Para G44 — Saldo de cuenta: SUM del campo valor de aportes con tipoAporte.codigo = 23
	 * agrupado por entidad, con fechaTransaccion <= fechaCorte.
	 * Retorna Object[]{Long codigoEntidad, Double suma}.
	 */
	List<Object[]> selectSumaSaldoCuentaJubilacionPorEntidad(LocalDateTime fechaCorte) throws Throwable;

	/**
	 * Para G44 ex-jubilados: SUM de aportes con tipoAporte.codigo = 23
	 * cuya fechaTransaccion esté entre fechaInicio y fechaFin (rango del mes).
	 * Retorna Object[]{Long codigoEntidad, Double suma}.
	 */
	List<Object[]> selectSumaAportesTipo23EnRango(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/**
	 * Para G42 — Tipo de prestación: Obtiene los códigos de tipoAporte distintos (9, 11) por entidad
	 * con tipoAporte.estado=1 y fechaTransaccion <= fechaCorte.
	 * Retorna Object[]{Long codigoEntidad, List<Long> codigosTipoAporte}.
	 */
	List<Object[]> selectTiposAportePorEntidad(LocalDateTime fechaCorte) throws Throwable;


	/*filtra todos los aporte por id de entidad
	 * @param :idEntidad
	 * @return Lista de Aporte
	 */
	List<Aporte> selectByEntidad(Long idEntidad) throws Throwable;
	
	Long selectCountByEntidad(Long idEntidad) throws Throwable;
	
	/**
	 * Busca un aporte específico por entidad, tipo de aporte, idAsoprep y estado
	 * OPTIMIZADO: Consulta directa a BD con filtros en lugar de traer todos y filtrar en memoria
	 * 
	 * @param idEntidad Código de la entidad
	 * @param idTipoAporte Código del tipo de aporte (9=Jubilación, 11=Cesantía)
	 * @param idAsoprep Código de la CargaArchivo
	 * @param estados Lista de estados permitidos (ej: [1=PENDIENTE, 6=PARCIAL])
	 * @return Aporte encontrado o null
	 */
	Aporte selectByEntidadTipoYCarga(Long idEntidad, Long idTipoAporte, Long idAsoprep, List<Long> estados) throws Throwable;
	
	/**
	 * Busca aportes adelantados (con saldo pendiente) por entidad y tipo
	 * Excluye aportes de la carga actual (para encontrar los del mes anterior)
	 * OPTIMIZADO: Filtros en BD, solo trae registros con saldo > 0
	 * 
	 * @param idEntidad Código de la entidad
	 * @param idTipoAporte Código del tipo de aporte
	 * @param idAsoprep Código de la carga ACTUAL (para excluir)
	 * @param estados Lista de estados permitidos (PENDIENTE, PARCIAL)
	 * @return Aporte adelantado encontrado o null
	 */
	Aporte selectAporteAdelantado(Long idEntidad, Long idTipoAporte, Long idAsoprep, List<Long> estados) throws Throwable;
	
	/**
	 * Busca el aporte más antiguo (MIN codigo) con saldo pendiente creado por el sistema
	 * Usuario = "SAA_AH" indica que fue creado automáticamente por el sistema
	 * Estado PARCIAL = Tiene saldo pendiente
	 * 
	 * @param idEntidad Código de la entidad
	 * @param idTipoAporte Código del tipo de aporte
	 * @return Aporte más antiguo con saldo pendiente o null
	 */
	Aporte selectMinAporteParcialSistema(Long idEntidad, Long idTipoAporte) throws Throwable;
	
	/**
	 * Busca el aporte más antiguo (MIN codigo) con saldo pendiente, SIN importar quién lo creó
	 * ✅ OPTIMIZADO: Query específica con índices en BD - Deja que la BD haga el trabajo
	 * 
	 * @param idEntidad Código de la entidad
	 * @param idTipoAporte Código del tipo de aporte
	 * @return Aporte más antiguo con saldo pendiente o null
	 */
	Aporte selectMinAporteConSaldo(Long idEntidad, Long idTipoAporte) throws Throwable;
	
	/**
	 * Obtiene KPIs globales de aportes para el dashboard
	 * 
	 * @param fechaDesde Fecha inicial (opcional)
	 * @param fechaHasta Fecha final (opcional)
	 * @param estadoAporte Estado del aporte (opcional)
	 * @return DTO con los KPIs calculados
	 */
	com.saa.model.crd.dto.AporteKpiDTO selectKpisGlobales(java.time.LocalDateTime fechaDesde, 
	                                                       java.time.LocalDateTime fechaHasta, 
	                                                       Long estadoAporte) throws Throwable;
	
	/**
	 * Obtiene resumen de aportes agrupados por tipo (para dona/tarjetas)
	 * 
	 * @param fechaDesde Fecha inicial (opcional)
	 * @param fechaHasta Fecha final (opcional)
	 * @param estadoAporte Estado del aporte (opcional)
	 * @return Lista de resúmenes por tipo con porcentajes
	 */
	java.util.List<com.saa.model.crd.dto.AporteResumenTipoDTO> selectResumenPorTipo(
			java.time.LocalDateTime fechaDesde, 
			java.time.LocalDateTime fechaHasta, 
			Long estadoAporte) throws Throwable;
	
	/**
	 * Obtiene top N entidades con mayor impacto por tipo de aporte
	 * 
	 * @param fechaDesde Fecha inicial (opcional)
	 * @param fechaHasta Fecha final (opcional)
	 * @param estadoAporte Estado del aporte (opcional)
	 * @param tipoAporteId Tipo de aporte específico (opcional)
	 * @param topN Cantidad de entidades a retornar
	 * @return Lista de top entidades ordenadas por magnitud
	 */
	java.util.List<com.saa.model.crd.dto.AporteTopEntidadDTO> selectTopEntidades(
			java.time.LocalDateTime fechaDesde, 
			java.time.LocalDateTime fechaHasta, 
			Long estadoAporte,
			Long tipoAporteId,
			Integer topN) throws Throwable;
	
	/**
	 * Obtiene top N movimientos individuales más grandes por tipo
	 * 
	 * @param fechaDesde Fecha inicial (opcional)
	 * @param fechaHasta Fecha final (opcional)
	 * @param estadoAporte Estado del aporte (opcional)
	 * @param tipoAporteId Tipo de aporte específico (opcional)
	 * @param topN Cantidad de movimientos a retornar
	 * @return Lista de movimientos ordenados por magnitud
	 */
	java.util.List<com.saa.model.crd.dto.AporteTopMovimientoDTO> selectTopMovimientos(
			java.time.LocalDateTime fechaDesde, 
			java.time.LocalDateTime fechaHasta, 
			Long estadoAporte,
			Long tipoAporteId,
			Integer topN) throws Throwable;

}
