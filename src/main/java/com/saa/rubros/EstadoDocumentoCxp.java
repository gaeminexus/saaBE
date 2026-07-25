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
}
