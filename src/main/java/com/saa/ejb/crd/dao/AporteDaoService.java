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
	 * Para G44 — Imposiciones acumuladas, agrupado por entidad, con fechaTransaccion &lt;=
	 * fechaCorte (filtro sin cambios; sigue siendo por fecha de CAJA).
	 *
	 * <p><b>Cambio del 2026-08-27 (decisión del usuario):</b> cuenta MESES DE DEVENGO
	 * distintos ({@code COUNT(DISTINCT} periodo efectivo{@code )}), no filas
	 * ({@code COUNT(*)}). Con anticipos y meses pagados a medias, filas ≠ meses:
	 * "imposiciones acumuladas" significa meses aportados. Ver
	 * {@code PeriodoEfectivoAporteSql}.</p>
	 *
	 * <p>⚠ Esta cifra se reporta a la Superintendencia. Antes de usar este método en
	 * producción, correr
	 * {@code docs/logica-negocio/crd/sql/67_COMPARACION_G44_IMPOSICIONES_ANTES_DESPUES.sql}
	 * para medir el impacto por entidad.</p>
	 *
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

	/**
	 * Para G40 — Suma total (todas las entidades) de aportes con tipoAporte.codigo = tipoAporte,
	 * con fechaTransaccion <= fechaCorte.
	 * Retorna Double suma global, o null si no hay registros.
	 */
	Double selectSumaTotalPorTipoAporte(LocalDateTime fechaCorte, Long tipoAporte) throws Throwable;

	/**
	 * Para CPRM — Suma de aportes agrupada por entidad Y tipo de aporte hasta fechaCorte.
	 * Genera un registro por cada combinación entidad+tipoAporte con valor != 0.
	 * Retorna Object[]{Long codigoEntidad, Long codigoTipoAporte, String nombreTipoAporte, Double suma}.
	 */
	List<Object[]> selectSumaPorEntidadYTipoAporte(LocalDateTime fechaCorte) throws Throwable;


	/**
	 * Para G43 — Imposiciones personales: tipoAporte.codigo IN (9, 11) y valor > 0, para una
	 * entidad.
	 *
	 * <p><b>Cambio del 2026-08-27 (decisión del usuario, mismo criterio que G44):</b> cuenta
	 * MESES DE DEVENGO distintos ({@code COUNT(DISTINCT} periodo efectivo{@code )}), no
	 * filas. Bajo el modelo de devengo filas ≠ meses: un partícipe puede tener varias filas
	 * del mismo mes (pago parcial completado después, anticipos, ajustes) y una sola fila
	 * puede cubrir un mes distinto al de su fecha de caja. Ver
	 * {@code PeriodoEfectivoAporteSql}.</p>
	 *
	 * @param codigoEntidad Código de la entidad cesante
	 * @return Cantidad de meses de devengo con imposición personal
	 */
	Long selectCountImposicionesPersonalesPorEntidad(Long codigoEntidad) throws Throwable;

	/**
	 * Para G43 — Imposiciones patronales: tipoAporte.codigo IN (13, 14) y valor > 0, para una
	 * entidad.
	 *
	 * <p>Mismo cambio del 2026-08-27 que {@link #selectCountImposicionesPersonalesPorEntidad}:
	 * cuenta meses de devengo distintos, no filas.</p>
	 *
	 * @param codigoEntidad Código de la entidad cesante
	 * @return Cantidad de meses de devengo con imposición patronal
	 */
	Long selectCountImposicionesPatronalesPorEntidad(Long codigoEntidad) throws Throwable;

	/**
	 * Para G43 — Saldo cuenta individual: SUM de aportes con valor < 0,
	 * tipoAporte.estado = 1, y fechaTransaccion dentro del mes de ejecucion.
	 * Retorna Double suma (negativa), o null si no hay registros.
	 * @param codigoEntidad Código de la entidad cesante
	 * @param fechaInicio   Primer instante del mes de ejecucion (inclusive)
	 * @param fechaFin      Último instante del mes de ejecucion (inclusive)
	 */
	Double selectSumaAportesNegativosMesPorEntidad(Long codigoEntidad,
			java.time.LocalDateTime fechaInicio,
			java.time.LocalDateTime fechaFin) throws Throwable;

	/**
	 * Suma los aportes positivos de una entidad, para los tipos indicados, cuya
	 * fechaTransaccion cae dentro del mes/año dado.
	 *
	 * Es la misma base que usa el padrón de partícipes para decidir si un mes
	 * cuenta como aportado (tipos 9/11 con valor &gt; 0), de modo que el proceso
	 * de carga Petro y el padrón no puedan contradecirse.
	 *
	 * <p>Compara por PERIODO EFECTIVO (D3, 2026-08-27), no por año/mes de
	 * {@code fechaTransaccion}: la fecha de caja es siempre el mes de la CARGA, nunca el del
	 * devengo. Ver {@code PeriodoEfectivoAporteSql}.</p>
	 *
	 * @param codigoEntidad Código de la entidad
	 * @param tiposAporte   Códigos de tipo de aporte a considerar (ej: 9 y 11)
	 * @param anio          Año del periodo de devengo a evaluar
	 * @param mes           Mes del periodo de devengo a evaluar (1-12)
	 * @return Suma de los aportes; 0.0 si no hay ninguno; null si no pudo evaluarse
	 * @throws Throwable Si ocurre algún error
	 */
	Double sumaAportesPositivosPorTipoYPeriodo(Long codigoEntidad, List<Long> tiposAporte,
			Long anio, Long mes) throws Throwable;

	/**
	 * Fecha del último aporte positivo de cada entidad, para los tipos indicados,
	 * anterior a la fecha de corte.
	 *
	 * Se resuelve en un solo query para evitar N+1 al generar el archivo Petro,
	 * donde hay que calcular la deuda acumulada de los partícipes en mora.
	 *
	 * @param codigosEntidad Entidades a consultar; si viene vacía retorna lista vacía
	 * @param tiposAporte    Códigos de tipo de aporte a considerar (ej: 9 y 11)
	 * @param corte          Solo se consideran aportes con fechaTransaccion &lt; corte
	 * @return Lista de Object[]{Long codigoEntidad, LocalDateTime ultimaFechaAporte}.
	 *         Las entidades sin ningún aporte no aparecen en el resultado.
	 * @throws Throwable Si ocurre algún error
	 */
	List<Object[]> selectUltimaFechaAportePorEntidad(List<Long> codigosEntidad,
			List<Long> tiposAporte, LocalDateTime corte) throws Throwable;

	/*filtra todos los aporte por id de entidad
	 * @param :idEntidad
	 * @return Lista de Aporte
	 */
	List<Aporte> selectByEntidad(Long idEntidad) throws Throwable;

	/**
	 * Todas las filas NEGATIVAS que generó UN registro de devolución de aportes para
	 * (entidad, tipo): {@code CRD.DDVA} solo guarda la referencia a la PRIMERA cuando el
	 * reparto por período crea varias — ver el comentario de
	 * {@code DevolucionAporteServiceImpl.crearFilaNegativaDevolucion}. Match exacto por
	 * {@code fechaTransaccion} (el mismo instante se reutiliza para TODAS las filas de un
	 * mismo registro) y {@code tipoMovimiento = DEVOLUCION} recupera las que DDVA no alcanza
	 * a referenciar.
	 *
	 * @param idEntidad       : Código de la entidad
	 * @param idTipoAporte    : Código del tipo de aporte
	 * @param fechaTransaccion: Instante exacto compartido por todas las filas del registro
	 * @return                : Listado; nunca vacío si la devolución generó al menos una fila
	 * @throws Throwable      : Excepcion
	 */
	List<Aporte> selectByEntidadTipoYFechaTransaccion(Long idEntidad, Long idTipoAporte,
			LocalDateTime fechaTransaccion) throws Throwable;

	/**
	 * TODAS las filas negativas (y solo esas) que generó una devolución — vía la FK directa
	 * {@code APRTIDDV}, poblada en cada fila desde el 2026-08-29. VACÍA para una devolución
	 * anterior a esa fecha (usar {@link #selectByEntidadTipoYFechaTransaccion} como fallback).
	 *
	 * @param idDevolucion : Código de la devolución (CRD.DVAP)
	 * @return             : Listado; vacío si la devolución es anterior a APRTIDDV
	 * @throws Throwable   : Excepcion
	 */
	List<Aporte> selectByDevolucion(Long idDevolucion) throws Throwable;

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
	 * @deprecated Sin llamadores desde antes de la Fase 1 del plan de devengo de aportes.
	 *             Mismo motivo que {@link #selectMinAporteConSaldo}: el FIFO por saldo
	 *             pendiente desapareció (D1: toda fila nueva nace con {@code saldo = 0}). Se
	 *             deja sin borrar por si hace falta para depurar datos históricos.
	 */
	@Deprecated
	Aporte selectMinAporteParcialSistema(Long idEntidad, Long idTipoAporte) throws Throwable;
	
	/**
	 * Busca el aporte más antiguo (MIN codigo) con saldo pendiente, SIN importar quién lo creó
	 * ✅ OPTIMIZADO: Query específica con índices en BD - Deja que la BD haga el trabajo
	 *
	 * @param idEntidad Código de la entidad
	 * @param idTipoAporte Código del tipo de aporte
	 * @return Aporte más antiguo con saldo pendiente o null
	 * @deprecated El FIFO por saldo pendiente desapareció en la Fase 1 del plan de devengo
	 *             de aportes (D1: {@code valor} pasa a ser siempre lo efectivamente recibido,
	 *             toda fila nueva nace con {@code saldo = 0}). Ningún llamador queda desde
	 *             esa fase; se deja el método sin borrar por si hace falta para depurar
	 *             datos históricos previos a la corrección de
	 *             {@code 62_CORRECCION_VALOR_APORTES_CARGA.sql}.
	 */
	@Deprecated
	Aporte selectMinAporteConSaldo(Long idEntidad, Long idTipoAporte) throws Throwable;

	/**
	 * Suma de {@code valor} de los aportes de una entidad y tipo cuyo PERIODO EFECTIVO
	 * coincide con el periodo indicado (primer día del mes). Es "aportado(m, tipo)" de la
	 * prelación por mes de devengo (§2.3 del plan de devengo de aportes): reemplaza al FIFO
	 * por saldo pendiente.
	 *
	 * <p><b>Periodo efectivo (D3, corregido el 2026-08-27)</b> — NO es
	 * {@code NVL(APRTPRDV, TRUNC(APRTFCTR,'MM'))} a secas, eso está mal para los movimientos
	 * NEGATIVOS: una devolución sin devengo (retiro de saldo, D5) no pertenece a ningún mes,
	 * y caer en su mes de caja la haría ver ese mes "incompleto" y se volvería a cobrar. La
	 * regla real (ver {@code AporteDaoServiceImpl.PERIODO_EFECTIVO_SQL}):
	 * <pre>
	 *   CASE WHEN APRTPRDV IS NOT NULL THEN APRTPRDV
	 *        WHEN APRTVLRR &gt; 0         THEN TRUNC(APRTFCTR, 'MM')
	 *        ELSE NULL END
	 * </pre>
	 * Un aporte POSITIVO sin devengo (dato histórico sin backfillear) sí pertenece a algún
	 * mes: el de su fecha de caja. Con esto, las filas que el backfill deje sin resolver ya
	 * no provocan recobros: se recomienda igual correr
	 * {@code 63_BACKFILL_DEVENGO_APORTES.sql} antes de usar esta prelación en producción,
	 * pero si no se ha corrido, ya no corrompe datos.</p>
	 *
	 * @param idEntidad    Código de la entidad
	 * @param idTipoAporte Código del tipo de aporte
	 * @param periodo      Primer día del mes de devengo
	 * @return Suma de valor; 0.0 si no hay filas
	 * @throws Throwable Si ocurre un error
	 */
	Double sumValorPorEntidadTipoYDevengo(Long idEntidad, Long idTipoAporte, java.time.LocalDate periodo)
			throws Throwable;

	/**
	 * Igual que {@link #sumValorPorEntidadTipoYDevengo}, pero agregado por PERIODO EFECTIVO
	 * sobre un rango [desde, hasta] inclusive, agrupado por periodo y tipo, en UNA sola
	 * consulta. Pensado para evitar N consultas al recorrer varios meses de una entidad (el
	 * caso de la prelación §2.3) y para el backfill.
	 *
	 * @param idEntidad Código de la entidad
	 * @param desde     Primer día del mes de devengo, inclusive
	 * @param hasta     Primer día del mes de devengo, inclusive
	 * @return Filas {@code Object[]{LocalDate periodo, Long idTipoAporte, Double suma}}
	 * @throws Throwable Si ocurre un error
	 */
	List<Object[]> sumValorPorEntidadTipoYRangoDevengo(Long idEntidad, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable;

	/**
	 * Periodos de devengo FUTUROS respecto de {@code mesActual} (estrictamente posteriores)
	 * de una entidad y tipo, con {@code SUM(valor) > 0}, ordenados del más futuro al más
	 * cercano. Es la base de la regla D5 (§2.4 del plan de devengo de aportes): las
	 * devoluciones consumen primero los anticipos no vencidos, LIFO desde el más futuro.
	 *
	 * @param idEntidad    Código de la entidad
	 * @param idTipoAporte Código del tipo de aporte
	 * @param mesActual    Primer día del mes en curso; los periodos deben ser POSTERIORES
	 * @return Filas {@code Object[]{LocalDate periodo, Double suma}}, más futuro primero
	 * @throws Throwable Si ocurre un error
	 */
	List<Object[]> selectPeriodosAnticipadosConSaldo(Long idEntidad, Long idTipoAporte,
			java.time.LocalDate mesActual) throws Throwable;

	/**
	 * Movimientos crudos de una entidad para el estado de cuenta por devengo (§4.2 del plan
	 * de devengo de aportes). Filtra por PERIODO EFECTIVO entre [desde, hasta] inclusive, O
	 * periodo efectivo NULL (histórico sin backfillear / retiro de saldo — ese grupo nunca se
	 * esconde, sin importar el rango pedido).
	 *
	 * @param idEntidad Código de la entidad
	 * @param desde     Primer día del mes de devengo, inclusive
	 * @param hasta     Primer día del mes de devengo, inclusive
	 * @return Filas {@code Object[]{LocalDate periodoEfectivo, Long idTipoAporte, Long idAporte,
	 *         LocalDateTime fechaTransaccion, Double valor, Long tipoMovimiento, String glosa}},
	 *         ordenadas por periodo (NULL al final), tipo, fecha
	 * @throws Throwable Si ocurre un error
	 */
	List<Object[]> selectMovimientosPorEntidadYRangoDevengo(Long idEntidad, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable;

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

	// ========================================================================
	// SERVICIOS DE PAGO DE PRÉSTAMOS (§5.2 ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md)
	// ========================================================================

	/**
	 * Saldo de aportes de una entidad agrupado por tipo de aporte vigente
	 * (TipoAporte.estado = 1). El saldo disponible ES la suma neta de APRTVLRR: los pagos
	 * se registran como filas negativas.
	 *
	 * Una sola query agregada; NUNCA traer las filas de APRT (la tabla tiene ~980.000
	 * registros y bajarlas al frontend es la causa del OutOfMemoryError documentado).
	 *
	 * @param codigoEntidad Código de la entidad (partícipe)
	 * @return Lista de Object[]{Long codigoTipoAporte, String nombreTipoAporte, Double suma};
	 *         vacía si la entidad no tiene aportes
	 * @throws Throwable Si ocurre algún error
	 */
	List<Object[]> sumValorPorTipoAporteByEntidad(Long codigoEntidad) throws Throwable;

	/**
	 * Saldo de aportes de una entidad para un tipo de aporte concreto: SUM(APRTVLRR) neto.
	 * Devuelve 0.0 si no hay filas.
	 *
	 * @param codigoEntidad    Código de la entidad (partícipe)
	 * @param codigoTipoAporte Código del tipo de aporte
	 * @return Saldo disponible del tipo (0.0 si no hay aportes)
	 * @throws Throwable Si ocurre algún error
	 */
	Double sumValorByEntidadYTipo(Long codigoEntidad, Long codigoTipoAporte) throws Throwable;

	/**
	 * Suma de {@code valor} de los aportes generados por una carga Petro (CRD.CRAR),
	 * agrupada por tipo de aporte. Base del asiento de APLICACION del cobro de Petro en dos
	 * pasos — ver {@code CobroPetroContableService.contabilizarAplicacion}.
	 *
	 * <p><b>⚠️ TRANSITORIO (2026-08-28/29): filtra por {@code idAsoprep}, NO por
	 * {@code cargaArchivo}/{@code CRARCDGO}</b>, aunque esta última es la columna gobernada
	 * (FK + índice) que se sigue llenando en cargas nuevas — ver
	 * {@code docs/logica-negocio/crd/sql/DDL-TRAZABILIDAD-CARGA-PETRO.sql}.
	 * {@code idAsoprep} es la trazabilidad de carga que ya existía antes de esa columna
	 * (usada en vivo por {@link #selectByEntidadTipoYCarga}/{@link #selectAporteAdelantado})
	 * y es lo único poblado en el histórico. Migrar el filtro a {@code cargaArchivo.codigo}
	 * solo después de correr y verificar en producción
	 * {@code docs/logica-negocio/crd/sql/78_BACKFILL_CRARCDGO_APORTES.sql} (su control 3.1
	 * en 0). Implementación en {@code AporteDaoServiceImpl}, con el mismo comentario.</p>
	 *
	 * @param idCarga Código de la carga (CRD.CRAR)
	 * @return Filas {@code Object[]{Long idTipoAporte, Double suma}}; VACÍA si la carga no
	 *         generó aportes
	 * @throws Throwable Si ocurre algún error
	 */
	List<Object[]> sumValorPorTipoAporteByCarga(Long idCarga) throws Throwable;

	/**
	 * Los aportes de una carga Petro (CRD.CRAR), uno por uno — misma carga y MISMO CRITERIO DE
	 * FILTRO que {@link #sumValorPorTipoAporteByCarga} ({@code idAsoprep}, no
	 * {@code cargaArchivo}/{@code CRARCDGO}; ver el javadoc de ese método para el porqué
	 * transitorio). Base de {@code DistribucionBandaService.registrarDistribucionCargaPetro}
	 * (2026-09-02, PLAN-AUDITORIA-BANDAS.md): la pantalla de auditoría de bandas necesita el
	 * detalle fila por fila de los aportes, no solo el agregado por tipo que usa el asiento —
	 * pero los dos tienen que leer la carga con el mismo filtro, para que el detalle y el
	 * asiento nunca puedan divergir por leer columnas distintas. El día que el criterio migre a
	 * {@code cargaArchivo.codigo}, migra en los dos métodos juntos.
	 *
	 * @param idCarga Código de la carga (CRD.CRAR)
	 * @return Aportes de la carga; VACÍA si no generó ninguno
	 * @throws Throwable Si ocurre algún error
	 */
	List<Aporte> selectByCarga(Long idCarga) throws Throwable;

}
