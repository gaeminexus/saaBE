package com.saa.ejb.cnt.service;

import java.util.List;
import java.util.Map;

import com.saa.ejb.cnt.service.dto.AccionNovedad;
import com.saa.ejb.cnt.service.dto.LoteNovedades;
import com.saa.model.cnt.Decision;
import com.saa.model.cnt.HistorialNovedad;

import jakarta.ejb.Local;

/**
 * C25 — Seguimiento de novedades (Contabilidad > Procesos > Seguimiento Novedades).
 *
 * <p>Toda la lógica de estados vive aquí (una sola regla; el frontend solo oculta botones). Toda escritura valida
 * el usuario primero (U-2) y deja historial en CNT.HSNV. Los errores de negocio son {@code IncomeException} con un
 * mensaje que se muestra tal cual (el REST los devuelve como 400). Diseño: cuadreSuper
 * 03-trabajo/cambios-saa/c25-seguimiento-novedades/C25-SEGUIMIENTO-NOVEDADES.md con las respuestas de D-056.</p>
 */
@Local
public interface SeguimientoNovedadService {

	// ---------------------------------------------------------------- F1 carga

	/**
	 * Carga (o simula) el lote de novedades de un mes (JSON del expediente). Idempotente por empresa + mes + módulo +
	 * código: una novedad nueva se crea en BORRADOR; una existente en BORRADOR de la misma versión se actualiza
	 * (textos y requerimientos); una ya enviada no se toca (aviso: use nueva versión). Asientos validados contra
	 * CNT.ASNT de la empresa; decisiones contra CNT.DCSN. Con {@code simular} hace todo y deshace al final.
	 */
	Map<String, Object> cargarLote(Long idEmpresa, String usuario, boolean simular, LoteNovedades lote) throws Throwable;

	/** Carga (o simula) las decisiones del expediente (origen EXPEDIENTE). Idempotente por número. */
	Map<String, Object> cargarDecisiones(Long idEmpresa, String usuario, boolean simular, LoteNovedades.LoteDecisiones lote) throws Throwable;

	// ---------------------------------------------------------------- F2 consulta

	/** Bandeja: una fila por novedad (sin CLOB) con conteos de requerimientos, ajustes y saldo por regularizar vinculado. */
	List<Map<String, Object>> bandeja(Long idEmpresa, Long anio, Long mes, String modulo, String tipo, Long estado,
			String area, String texto, boolean incluirReemplazadas) throws Throwable;

	/** Vista por requerimiento (la lista de trabajo de cada área). */
	List<Map<String, Object>> vistaRequerimientos(Long idEmpresa, Long anio, Long mes, String modulo, String tipo,
			Long estado, String area, String texto) throws Throwable;

	/** Detalle completo de una novedad: textos, requerimientos con respuestas y adjuntos, vínculos con los asientos y sus líneas, cuentas por regularizar, historial. */
	Map<String, Object> detalle(Long idNovedad) throws Throwable;

	/** Resumen del mes: contadores por estado, módulo y tipo; antigüedad de los pedidos; saldo de las cuentas por regularizar. */
	Map<String, Object> resumenMes(Long idEmpresa, Long anio, Long mes) throws Throwable;

	List<HistorialNovedad> historial(Long idNovedad) throws Throwable;

	/**
	 * C25b — Listado de decisiones tomadas (CNT.DCSN) con su ficha completa y las novedades vinculadas (vínculo DECISION
	 * o decisión originada al responder un requerimiento). {@code texto} filtra por número, tema, texto, razones, quién
	 * decide o evidencia.
	 */
	List<Map<String, Object>> decisiones(Long idEmpresa, String texto) throws Throwable;

	// ---------------------------------------------------------------- F3 acciones

	/** BORRADOR -> ENVIADA de las novedades {@code ids} (o de todo el mes si {@code ids} viene vacío y se dan año y mes). */
	Map<String, Object> enviar(Long idEmpresa, Long anio, Long mes, AccionNovedad accion) throws Throwable;

	/** Respuesta de ASOPREP a un requerimiento (texto, opción, canal, adjuntos). PENDIENTE u OBSERVADO -> RESPONDIDO. */
	Map<String, Object> responder(Long idRequerimiento, AccionNovedad accion) throws Throwable;

	/** ASOPREP indica que el documento no existe: -> NO_EXISTE (lo decide Contabilidad con constancia). */
	Map<String, Object> noExiste(Long idRequerimiento, AccionNovedad accion) throws Throwable;

	/** GAEMII marca que el requerimiento ya no aplica (motivo obligatorio). */
	Map<String, Object> noAplica(Long idRequerimiento, AccionNovedad accion) throws Throwable;

	/** Revisión: acepta la respuesta (otro usuario que el que la registró). DECISION, APROBACION y NO_EXISTE exigen decisión registrada. */
	Map<String, Object> aceptar(Long idRespuesta, AccionNovedad accion) throws Throwable;

	/** Revisión: observa la respuesta (texto obligatorio); el requerimiento vuelve a ASOPREP. */
	Map<String, Object> observar(Long idRespuesta, AccionNovedad accion) throws Throwable;

	/**
	 * Registra la decisión de Contabilidad de ASOPREP (D-056 (4)) con su constancia (al menos un adjunto). Devuelve la
	 * decisión, el número D-nnn asignado y el texto listo para copiar a DECISIONES-Y-PARTIDAS.md (D-056 (3)).
	 */
	Map<String, Object> registrarDecision(Long idEmpresa, AccionNovedad accion) throws Throwable;

	/** Marca que la decisión ya se copió al expediente (DECISIONES-Y-PARTIDAS.md). */
	Decision marcarDecisionCopiada(Long idDecision, AccionNovedad accion) throws Throwable;

	/** Texto de la fila de DECISIONES-Y-PARTIDAS.md de una decisión. */
	String textoExpediente(Long idDecision) throws Throwable;

	/**
	 * Registra uno o varios asientos manuales de ajuste con que Contabilidad arregló la novedad (D-056 (7)). Cada
	 * asiento se valida contra CNT.ASNT de la empresa de la novedad (existe, activo). Si se indica un requerimiento
	 * ACEPTADO, queda APLICADO.
	 */
	Map<String, Object> registrarAjuste(Long idNovedad, AccionNovedad accion) throws Throwable;

	/** ACEPTADO -> APLICADO, con asientos de ajuste o con el motivo de por qué no hace falta ajuste. */
	Map<String, Object> aplicar(Long idRequerimiento, AccionNovedad accion) throws Throwable;

	/** Vínculo genérico (ASIENTO_ORIGEN, ASIENTO_AJUSTE, PARTIDA_C22, DECISION, NOVEDAD, DOCUMENTO_MODULO). */
	Map<String, Object> vincular(Long idNovedad, AccionNovedad accion) throws Throwable;

	/** Anula un vínculo (no se borra). Motivo obligatorio. */
	Map<String, Object> desvincular(Long idVinculo, AccionNovedad accion) throws Throwable;

	/** Registra los metadatos de un archivo ya subido (novedad, requerimiento, respuesta o decisión). */
	Map<String, Object> registrarAdjunto(AccionNovedad accion) throws Throwable;

	/** Marca un adjunto como copiado al expediente (02-informacion-cliente, regla 2). */
	Map<String, Object> marcarAdjuntoCopiado(Long idAdjunto, AccionNovedad accion) throws Throwable;

	/** Cierra la novedad: todos sus requerimientos APLICADO / NO_APLICA (o informativa). */
	Map<String, Object> cerrar(Long idNovedad, AccionNovedad accion) throws Throwable;

	/** Cierra en bloque las INFORMATIVA enviadas del mes (se dan por conocidas con el acta, diseño P6). */
	Map<String, Object> cerrarInformativas(Long idEmpresa, Long anio, Long mes, AccionNovedad accion) throws Throwable;

	Map<String, Object> reabrir(Long idNovedad, AccionNovedad accion) throws Throwable;

	Map<String, Object> anular(Long idNovedad, AccionNovedad accion) throws Throwable;

	/** Copia la novedad y sus requerimientos como versión nueva en BORRADOR; la anterior queda REEMPLAZADA. */
	Map<String, Object> nuevaVersion(Long idNovedad, AccionNovedad accion) throws Throwable;

	// ---------------------------------------------------------------- F4 integración y acta

	/**
	 * Saldo en vivo de las cuentas por regularizar desde CNT.DTAS (sin depender de C22): saldo inicial, movimiento y
	 * saldo final del mes por cuenta, cuánto explican las novedades del mes (asientos de origen vinculados) y cuánto
	 * regularizaron los asientos de ajuste. Es el punto donde C22 se enchufa cuando exista.
	 */
	Map<String, Object> cuentasPorRegularizar(Long idEmpresa, Long anio, Long mes) throws Throwable;

	/** Datos y texto del reporte para el acta mensual (resumen, decisiones, ajustes, pendientes, informativas). */
	Map<String, Object> reporteActa(Long idEmpresa, Long anio, Long mes) throws Throwable;

	/** Pedido pendiente por área (lo que reemplaza o complementa el .txt). {@code area} opcional. */
	Map<String, Object> pendientePorArea(Long idEmpresa, Long anio, Long mes, String area) throws Throwable;

	/** Controles C25 para GET /prdo/controlCierre (advertencias, no bloquean). */
	List<com.saa.ejb.cnt.service.dto.ControlCierre> controlesCierre(Long idEmpresa, Long anio, Long mes);
}
