package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Local;


@Local
public interface PagoPrestamoDaoService extends EntityDao<PagoPrestamo> {

	/**
	 * Busca todos los pagos asociados a un DetallePrestamo específico
	 *
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Lista de PagoPrestamo encontrados (vacía si no hay registros)
	 */
	List<PagoPrestamo> selectByIdDetallePrestamo(Long codigoDetallePrestamo);

	// ========================================================================
	// SERVICIOS DE PAGO DE PRÉSTAMOS (§5.2 ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md)
	// ========================================================================

	/**
	 * Pagos VIGENTES (anulado IS NULL OR anulado = 0) de una cuota, ordenados por código ASC.
	 * REEMPLAZA el uso directo de selectByIdDetallePrestamo en el motor de pagos nuevo:
	 * los pagos anulados por un reverso NO deben contar en la reconstrucción de saldos.
	 *
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Lista de PagoPrestamo vigentes (vacía si no hay registros o si falla la consulta)
	 */
	List<PagoPrestamo> selectVigentesByIdDetallePrestamo(Long codigoDetallePrestamo);

	/**
	 * Pagos de un evento (para anulación/consulta), ordenados por código ASC.
	 *
	 * @param codigoEvento Código del EventoPrestamo (EVPR)
	 * @return Lista de PagoPrestamo del evento (vacía si no hay registros o si falla la consulta)
	 */
	List<PagoPrestamo> selectByEvento(Long codigoEvento);

	/**
	 * Cuenta los pagos VIGENTES (anulado IS NULL OR anulado = 0) de una cuota.
	 * El reverso del abono a capital lo usa para verificar que ninguna cuota generada por
	 * el evento tenga pagos antes de borrarla.
	 *
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Cantidad de pagos vigentes (0 si no hay o si falla la consulta)
	 */
	Long contarVigentesByIdDetallePrestamo(Long codigoDetallePrestamo);

	/**
	 * Pagos VIGENTES (anulado IS NULL OR anulado = 0) generados por una carga Petro
	 * (CRD.CRAR), ordenados por código ASC. Base del asiento de APLICACION del cobro de
	 * Petro en dos pasos — ver {@code CobroPetroContableService.contabilizarAplicacion} y
	 * {@code docs/logica-negocio/crd/sql/DDL-TRAZABILIDAD-CARGA-PETRO.sql}.
	 *
	 * @param idCarga Código de la carga (CRD.CRAR)
	 * @return Lista de PagoPrestamo vigentes; VACÍA si la carga no generó pagos o es anterior
	 *         al 2026-08-28 (CRARCDGO sin backfill)
	 * @throws Throwable Si ocurre algún error
	 */
	List<PagoPrestamo> selectVigentesByCargaArchivo(Long idCarga) throws Throwable;

	/**
	 * Cuenta TODOS los pagos de una cuota, incluidos los ANULADOS — a diferencia de
	 * {@link #contarVigentesByIdDetallePrestamo}, este método NO filtra por {@code anulado}.
	 * Un {@code PagoPrestamo} anulado sigue siendo una fila con FK a esa cuota: si la cuota
	 * desaparece, esa fila queda huérfana igual, aunque no cuente para saldos.
	 *
	 * <p><b>Guarda de borrado, no de saldos.</b> Este método existe para que
	 * {@code generarTablaAmortizacion(..., regenerar = true)} decida si puede borrar y
	 * recrear las cuotas de {@code CRD.DTPR}. Por eso, a propósito y al REVÉS de la
	 * convención de sus vecinos en este archivo, <b>NO atrapa la excepción</b>: la propaga
	 * ({@code throws Throwable}, sin {@code catch}). Una guarda de borrado que ante un error
	 * de consulta devolviera 0 le diría a la llamadora "adelante, no hay pagos" cuando en
	 * realidad no se pudo verificar — y borraría cuotas con pagos. No "uniformizar" este
	 * método con {@link #contarVigentesByIdDetallePrestamo} ni con los demás de esta clase:
	 * el catch-y-devolver-vacío es intencional en ellos y sería un bug acá.
	 *
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Cantidad total de pagos (vigentes + anulados) de la cuota
	 * @throws Throwable Si ocurre algún error en la consulta — se propaga, nunca se traga
	 */
	long countByIdDetallePrestamo(Long codigoDetallePrestamo) throws Throwable;

	/**
	 * Aplicado a préstamos de una carga Petro, agregado POR ENTIDAD (partícipe), con el
	 * desglose manual/automático por el que sale de la brecha — API-AUDITORIA-BANDAS.md §4
	 * ({@code GET /rest/dsbn/diferencia}), sql/184 bloque 3. El desglose usa el prefijo
	 * estable de {@code PGPROBSR} que dejó {@code e7b76c8}: {@code 'AFECTACION_MANUAL:%'} es
	 * manual, cualquier otra cosa (incluido {@code null}) es automático.
	 *
	 * @param idCarga Código de la carga (CRD.CRAR)
	 * @return Filas {@code Object[]{Long idEntidad, Double aplicadoManual, Double
	 *         aplicadoAutomatico, Double aplicadoTotal}}; VACÍA si la carga no generó pagos
	 * @throws Throwable Si ocurre algún error
	 */
	List<Object[]> selectAplicadoPorEntidadEnCarga(Long idCarga) throws Throwable;

}
