package com.saa.ejb.cnt.service;

import java.util.List;
import java.util.Map;

import com.saa.ejb.cnt.service.dto.AccionNovedadV2;
import com.saa.ejb.cnt.service.dto.HechoCreditoDto;

import jakarta.ejb.Local;

/**
 * C34 — Seguimiento de Novedades v2 (Contabilidad > Procesos > Seguimiento Novedades v2).
 *
 * <p>Pantalla pensada para que el usuario de ASOPREP siga cada novedad por sí mismo. Tres grupos calculados: POR_REGULARIZAR
 * (la novedad tiene líneas en CNT.LNNV: se justifican hasta saldo 0, con reparto a cuentas), PEDIDO (requerimientos
 * vigentes: se responden) e INFORMATIVA (se da por conocida). Sin control de cuatro ojos (aprobación del líder,
 * 18-nov-2026): lo registrado cuenta de inmediato y se anula con motivo. Cerrar = la resolución está completa y
 * estructurada para la pasada v8 del re-proceso; no genera asientos. Diseño: cuadreSuper
 * 03-trabajo/cambios-saa/c34-novedades-v2/C34-NOVEDADES-V2-DISENO.md.</p>
 */
@Local
public interface SeguimientoNovedadV2Service {

	/** Contadores por grupo y estado visible (mes opcional: null = todos). */
	Map<String, Object> resumen(Long idEmpresa, Long anio, Long mes) throws Throwable;

	/** Bandeja: una fila por novedad en seguimiento (sin BORRADOR, ANULADA ni REEMPLAZADA). */
	List<Map<String, Object>> bandeja(Long idEmpresa, Long anio, Long mes, String grupo, String area, String estado,
			String texto) throws Throwable;

	/** Fila de la bandeja de una novedad (para refrescar el grid después de una acción). */
	Map<String, Object> fila(Long idNovedad) throws Throwable;

	/** Detalle completo: textos, cifras, líneas con justificaciones y reparto, pedidos, detalle de origen, decisiones. */
	Map<String, Object> detalle(Long idNovedad) throws Throwable;

	/** Decisiones por descripción; primero las sugeridas para la novedad (las que ya cita y las más usadas en su módulo). */
	List<Map<String, Object>> buscarDecisiones(Long idEmpresa, Long idNovedad, String texto) throws Throwable;

	/** Cuentas contables de movimiento (no por regularizar) por código o nombre. */
	List<Map<String, Object>> buscarCuentas(Long idEmpresa, String texto) throws Throwable;

	/** Préstamos por idAsoprep, cédula o nombre del partícipe. */
	List<Map<String, Object>> buscarPrestamos(String texto) throws Throwable;

	/** Partícipes por cédula o nombre. */
	List<Map<String, Object>> buscarParticipes(String texto) throws Throwable;

	/** Cuotas de un préstamo (para el reparto a una cuota). */
	List<Map<String, Object>> cuotasPrestamo(Long idPrestamo) throws Throwable;

	/** Justifica una o varias líneas de una misma novedad (total o parcial) con su reparto a cuentas. */
	Map<String, Object> justificar(AccionNovedadV2 a) throws Throwable;

	/** Anula una justificación (con motivo); el saldo de la línea vuelve. Un cruce anula sus dos mitades. */
	Map<String, Object> anularJustificacion(Long idJustificacion, AccionNovedadV2 a) throws Throwable;

	/** Responde un pedido; queda atendido de inmediato (sin revisión de otro usuario). */
	Map<String, Object> responder(Long idRequerimiento, AccionNovedadV2 a) throws Throwable;

	/** Anula una respuesta (con motivo); el pedido vuelve a pendiente. */
	Map<String, Object> anularRespuesta(Long idRespuesta, AccionNovedadV2 a) throws Throwable;

	/** Cierra la novedad si cumple la regla de su grupo. Devuelve la fila actualizada. */
	Map<String, Object> cerrar(Long idNovedad, AccionNovedadV2 a) throws Throwable;

	/** Da por conocidas una o varias informativas. Devuelve las filas actualizadas. */
	Map<String, Object> darPorConocidas(AccionNovedadV2 a) throws Throwable;

	/** Reabre una novedad cerrada (motivo obligatorio). Devuelve la fila actualizada. */
	Map<String, Object> reabrir(Long idNovedad, AccionNovedadV2 a) throws Throwable;

	/**
	 * Respuestas estructuradas para exportar: una fila por cuenta destino de cada justificación vigente o respuesta.
	 * {@code formato} = "reproceso" (solo números internos) o "asoprep" (con idAsoprep, cédula y nombre).
	 */
	List<Map<String, Object>> exportar(Long idEmpresa, Long anio, Long mes, String formato) throws Throwable;

	// ---- C34d: hechos de crédito

	/**
	 * Buscador de orígenes del dinero. {@code fuente}: SUGERIDOS (necesita idPrestamo: mismo partícipe, relacionados por
	 * cónyuge o referencia familiar y valor ± una cuota), EXTRACTO, APORTE o PENDIENTE. Incluye movimientos ya procesados,
	 * con «aplicado hoy» y lo disponible (descontado lo usado en otros hechos vigentes).
	 */
	List<Map<String, Object>> buscarOrigenes(Long idEmpresa, String fuente, Long idPrestamo, Long idEntidad, String texto, String desde,
			String hasta, Double valor, String fecha) throws Throwable;

	/** Movimientos por regularizar y pedidos abiertos de la operación o de su partícipe (para marcar qué resuelve el hecho). */
	Map<String, Object> candidatosHecho(Long idEmpresa, Long idPrestamo) throws Throwable;

	/** Registra un hecho con sus orígenes; resuelve los movimientos y pedidos indicados y cierra las novedades que quedan sin nada. */
	Map<String, Object> registrarHecho(HechoCreditoDto h) throws Throwable;

	/** Anula un hecho (con motivo): anula sus justificaciones y respuestas; las novedades cerradas deben reabrirse antes. */
	Map<String, Object> anularHecho(Long idHecho, HechoCreditoDto h) throws Throwable;

	/** Hechos vigentes de la empresa, estructurados para la pasada v8 (operación, tipo, fecha, valor, orígenes y qué resuelven). */
	List<Map<String, Object>> exportarHechos(Long idEmpresa) throws Throwable;

	/** C34d (D-123): valor de una precancelación o de un hecho en serie calculado con la tabla del SAA, repartido por mes. */
	Map<String, Object> calcularHecho(Long idPrestamo, String tipo, String desde, String fecha) throws Throwable;

	/** C34d §10: lo adeudado por cuota y concepto de un hecho, a su fecha, con el cálculo del SAA (para condonar el faltante). */
	Map<String, Object> componentesHecho(Long idPrestamo, String tipo, String cuotas, String desde, String fecha) throws Throwable;

	/** C34d: nota de ASOPREP en la novedad (texto y adjuntos, sin cuentas; no cambia saldos ni estados). */
	Map<String, Object> registrarNota(Long idNovedad, AccionNovedadV2 a) throws Throwable;

	/** C34d: anula una nota (con motivo). */
	Map<String, Object> anularNota(Long idNota, AccionNovedadV2 a) throws Throwable;
}
