package com.saa.rubros;

/**
 * Rubro 176 — CXP_ESTADO_NOVEDAD
 *
 * Estado de resolución de una novedad en {@code DocumentoCxp}.
 * Solo aplica cuando {@code estadoDocumento = EstadoDocumentoCxp.NOVEDAD (5)}.
 * Se almacena en DCXP.DCXPENVD como valor numérico (Long).
 *
 * Corresponde al campo {@code estadoNovedad} de la entidad {@code DocumentoCxp}.
 */
public interface EstadoNovedad {

    /** Novedad detectada, aún no resuelta por el usuario. */
    long PENDIENTE   = 1L;

    /** Usuario eligió subir nuevo XML y re-registrar el documento. */
    long REEMPLAZADO = 2L;

    /** Usuario eligió conservar el documento previo sin cambios. */
    long MANTENIDO   = 3L;
}
