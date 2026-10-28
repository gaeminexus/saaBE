package com.saa.ejb.asoprep.service;

import com.saa.ejb.crd.service.dto.ResultadoReprocesoCargaPetro;
import com.saa.ejb.crd.service.dto.SolicitudReprocesoCargaPetro;

import jakarta.ejb.Local;

/**
 * Reproceso de una carga Petro YA APLICADA (cuadre Superintendencia, cambio C4 de
 * {@code cuadreSuper/01-seguimiento/CAMBIOS-SAA.md}). Diseño completo en
 * {@code docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md}.
 *
 * <pre>
 *   1. ANULA la aplicación anterior de la carga, con trazabilidad y sin borrar nada:
 *      - pagos de préstamo (CRD.PGPR) enlazados por CRARCDGO o, los del esquema viejo,
 *        identificados por la observación "[CargaArchivo: N]" → PGPRANUL = 1 + motivo;
 *        cada cuota tocada se reconstruye desde sus pagos vigentes y el préstamo que
 *        quedó CANCELADO con cuotas pendientes vuelve a VIGENTE;
 *      - aportes (CRD.APRT) enlazados por APRTIDAS/CRARCDGO o, los del esquema viejo,
 *        por la glosa "CargaArchivo: N" → contra-movimiento REVERSO (misma fecha de caja,
 *        mismo devengo, valor negativo) y el original marcado APRTIDST = 0;
 *      - asientos de REPARTO y APLICACION (CRD.ANCP) → anulados/reversados por el
 *        mecanismo estándar de contabilidad ({@code AsientoService.anulaAsiento}).
 *      El asiento TRANSITORIO del paso 1 (recepción confirmada) NO se toca.
 *   2. Devuelve la carga a CONFIRMADO_CONTABILIDAD y vuelve a correr
 *      {@code CargaArchivoPetroService.aplicarPagosArchivoPetroReproceso}: la misma fase 3
 *      vigente, fechada al último día del mes de la carga, sin la validación de orden
 *      cronológico y tolerando los roles Petro sin partícipe (quedan como novedad).
 * </pre>
 *
 * Todo corre en UNA transacción: si algo falla, no queda ni la anulación ni la
 * re-aplicación. Correrlo dos veces deja el mismo resultado neto (cada corrida anula lo que
 * dejó la anterior y vuelve a aplicar), con una etiqueta de corrida distinta.
 */
@Local
public interface ReprocesoCargaPetroService {

    /**
     * Reprocesa una carga Petro.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @param solicitud  : usuario (obligatorio), ip, motivo (obligatorio)
     * @return           : Resumen de lo anulado, lo aplicado y las novedades
     * @throws Throwable : {@code IncomeException} si la carga no existe, no tiene mes/año de
     *                     afectación, no tiene la recepción confirmada por contabilidad (paso
     *                     1 del cobro en dos pasos — hay que registrar las transferencias y
     *                     confirmar antes), o faltan usuario/motivo; cualquier fallo de la
     *                     re-aplicación propaga tal cual (todo o nada)
     */
    ResultadoReprocesoCargaPetro reprocesar(Long idCarga, SolicitudReprocesoCargaPetro solicitud) throws Throwable;
}
