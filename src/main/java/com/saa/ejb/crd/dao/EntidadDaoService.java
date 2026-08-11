package com.saa.ejb.crd.dao;

import java.math.BigDecimal;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Entidad;

import jakarta.ejb.Local;

@Local
public interface EntidadDaoService extends EntityDao<Entidad> {
	
	/**
	 * Selecciona los ParticipeXCargaArchivo por codigoPetro.
	 * @param codigoPetro: Código Petro a buscar.
	 * @return: Lista de ParticipeXCargaArchivo asociados al código Petro.
	 * @throws Throwable: Excepción en caso de error.
	 */
	List<Entidad> selectByCodigoPetro(Long codigoPetro) throws Throwable;
	
	/**
	 * Selecciona las coincidencias de ParticipeXCargaArchivo por nombre.
	 * @param nombre: Nombre a buscar.
	 * @return: Lista de ParticipeXCargaArchivo que coinciden con el nombre.
	 * @throws Throwable: Excepción en caso de error.
	 */
	List<BigDecimal> selectCoincidenciasByNombre(String nombre) throws Throwable;
	
	/**
	 * Recupera una lista de Entidad cuyo nombre completo coincida con el proporcionado por petro de 35 caracteres.
	 * @param nombre: Nombre completo a buscar.
	 * @return: Lista de Entidad que coinciden con el nombre completo.
	 * @throws Throwable: Excepción en caso de error.
	 */
	List<Entidad> selectByNombrePetro35(String nombre) throws Throwable;
	
	/**
	 * Obtiene resumen de entidades agrupadas por estado (para dashboard)
	 * 
	 * @param estadosPermitidos Lista de estados a incluir (ej: 10, 2, 30)
	 * @return Lista de resúmenes con estado y cantidad de entidades
	 * @throws Throwable Excepción en caso de error
	 */
	java.util.List<com.saa.model.crd.dto.EntidadResumenEstadoDTO> selectResumenPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable;
	
	/**
	 * Obtiene resumen de préstamos agrupados por estado de entidad
	 * 
	 * @param estadosPermitidos Lista de estados a incluir (ej: 10, 2, 30)
	 * @return Lista de resúmenes con estado y total de préstamos
	 * @throws Throwable Excepción en caso de error
	 */
	java.util.List<com.saa.model.crd.dto.EntidadResumenPrestamosDTO> selectResumenPrestamosPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable;
	
	/**
	 * Obtiene resumen de aportes agrupados por estado de entidad
	 * 
	 * @param estadosPermitidos Lista de estados a incluir (ej: 10, 2, 30)
	 * @return Lista de resúmenes con estado y total de aportes
	 * @throws Throwable Excepción en caso de error
	 */
	java.util.List<com.saa.model.crd.dto.EntidadResumenAportesDTO> selectResumenAportesPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable;
	
	/**
	 * Obtiene resumen consolidado (entidades, préstamos y aportes) por estado
	 * 
	 * @param estadosPermitidos Lista de estados a incluir (ej: 10, 2, 30)
	 * @return Lista de resúmenes consolidados
	 * @throws Throwable Excepción en caso de error
	 */
	java.util.List<com.saa.model.crd.dto.EntidadResumenConsolidadoDTO> selectResumenConsolidadoPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable;

	/**
	 * Recupera todas las entidades que tengan un idEstado específico.
	 * @param idEstado : Estado a filtrar
	 * @return : Listado de entidades con ese estado
	 * @throws Throwable : Excepcion
	 */
	List<Entidad> selectByIdEstado(Long idEstado) throws Throwable;

	/**
	 * Para G45 — Busca una Entidad por su número de identificación.
	 * @param numeroIdentificacion : Número de identificación (cédula/RUC)
	 * @return : Entidad encontrada o null
	 */
	Entidad selectByNumeroIdentificacion(String numeroIdentificacion) throws Throwable;

	/**
	 * Busca una Entidad por su código usando em.find() — retorna null si no existe.
	 */
	Entidad findById(Long codigo) throws Throwable;

	/**
	 * Carga múltiples entidades por sus códigos en una sola consulta.
	 * Optimización para evitar N+1 queries.
	 */
	List<Entidad> findByCodigosIn(List<Long> codigos) throws Throwable;

	/**
	 * Genera el padrón de partícipes a partir de CRD.ENTD, con el nombre de la
	 * calidad tomado de CRD.ESPR y los indicadores de aportes y mora calculados
	 * sobre CRD.APRT (tipos 9 = jubilación, 11 = cesantía, con valor positivo).
	 *
	 * Todo el padrón se evalúa al CIERRE DEL MES ANTERIOR al de fechaEjecucion,
	 * no a fechaEjecucion. Los aportes se graban con fecha del último día del mes,
	 * así que el mes en curso todavía no tiene su carga procesada. Consecuencia
	 * deseada: el reporte devuelve lo mismo se corra el día 5 o el 28 del mes.
	 *
	 * Reglas aplicadas:
	 *  - Número de aportes: meses distintos con al menos un aporte positivo 9/11,
	 *    hasta el último día del mes anterior inclusive.
	 *  - Estado de mora: EN MORA si no hubo aporte en los dos meses previos al
	 *    mes de referencia.
	 *  - Meses en mora: 0 si está al día; meses desde el último aporte hasta el
	 *    mes de referencia si no.
	 *  - Habilitado para voto: estado ACTIVO y AL DIA.
	 *  - Elegible para miembro: estado ACTIVO y numeroAportes >= minimoAportes.
	 *
	 * @param fechaEjecucion: Fecha de ejecución; el corte real es el fin del mes anterior.
	 * @param calidadId: Filtro opcional por ENTDIDST; null incluye todas las calidades.
	 * @param minimoAportes: Mínimo de aportes para ser elegible como miembro.
	 * @return: Lista de filas del padrón, numeradas y ordenadas por nombre.
	 * @throws Throwable: Excepción en caso de error.
	 */
	java.util.List<com.saa.model.crd.dto.PadronParticipeDTO> selectPadronParticipes(
			java.time.LocalDateTime fechaEjecucion,
			Long calidadId,
			Long minimoAportes) throws Throwable;

}
