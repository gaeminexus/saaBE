package com.saa.rubros;

/**
 * Rubro 175 — CXP_ESTADO_DOCUMENTO_CXP
 *
 * Ciclo de vida del {@code DocumentoCxp} (tabla PGS.DCXP).
 * Se almacena en DCXP.DCXPESTD como valor numérico (Long).
 *
 * Corresponde al campo {@code estadoDocumento} de la entidad {@code DocumentoCxp}.
 */
public interface EstadoDocumentoCxp {

    /** Documento leído del TXT, pendiente de cargar el XML. */
    long LEIDO         = 1L;

    /**
     * XML validado y guardado en disco, pendiente de registrar en BD.
     * Estado transitorio — con el endpoint unificado {@code /procesarXml} el documento
     * pasa directamente de LEIDO a REGISTRADO_BD.
     */
    long XML_CARGADO   = 2L;

    /** Registros creados en las tablas CXP destino (FacturaCompra, NotaCreditoCompra, etc.). */
    long REGISTRADO_BD = 3L;

    /** Falló algún paso del proceso. Ver campo {@code observacion} del documento. */
    long ERROR         = 4L;

    /**
     * Documento ya existía con valores distintos, o desapareció en una nueva carga.
     * Pendiente de resolución por el usuario (MANTENER o REEMPLAZAR).
     */
    long NOVEDAD       = 5L;

    /** Registros de BD eliminados por reversión manual. */
    long REVERTIDO     = 6L;

    /**
     * Documento anulado. <b>Estado terminal: no se puede volver a procesar.</b>
     *
     * <p>Agregado el 2026-08-31 (`PDTR` 1400, ver `REGISTRADO-RESERVAS-EQUIPOS.md`). Marca la
     * <b>anulación verdadera</b>: la factura se anuló, su asiento se anuló y el pago se anuló,
     * pero <b>los registros siguen existiendo</b> — sólo que anulados.
     *
     * <p>⛔ <b>No confundir con {@link #REVERTIDO}</b>, y la diferencia es justo la que importa:
     * <ul>
     *   <li>{@code REVERTIDO(6)} — los registros destino <b>se borraron</b>. La ingesta se deshizo
     *       como si nunca hubiera ocurrido, y el documento puede volver a procesarse.</li>
     *   <li>{@code ANULADO(7)} — los registros <b>siguen ahí</b>, anulados y con su motivo. Es un
     *       hecho contable que ocurrió y quedó registrado. <b>No se reprocesa nunca.</b></li>
     * </ul>
     *
     * <p>Tampoco confundir con dejar el documento en {@link #XML_CARGADO} para recontabilizar: eso
     * es el otro camino —regenerar el asiento con las cuentas corregidas— y ahí el documento
     * <b>sí</b> vuelve a {@link #REGISTRADO_BD}. Ver
     * `docs/logica-negocio/cxp/DISENO-ANULAR-VS-RECONTABILIZAR-FACTURA-COMPRA.md`.
     */
    long ANULADO       = 7L;
}
