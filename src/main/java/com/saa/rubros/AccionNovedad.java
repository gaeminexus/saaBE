package com.saa.rubros;

/**
 * Rubro 177 — CXP_ACCION_NOVEDAD
 *
 * Acción que el FRONTEND envía en el body JSON del endpoint
 * {@code POST /carga-documentos/resolverNovedad/{idDocumentoCxp}}.
 *
 * Campo en el JSON: {@code "accion": 1 | 2}
 * El label legible ("MANTENER", "REEMPLAZAR") se obtiene de PDTRVLRV en la tabla SCP.PDTR.
 */
public interface AccionNovedad {

    /**
     * Conservar el documento previo sin ningún cambio.
     * No se requiere {@code contenidoXml} en el body.
     * Resultado: {@code estadoNovedad} pasa a {@code EstadoNovedad.MANTENIDO (3)}.
     */
    int MANTENER   = 1;

    /**
     * Revertir el registro anterior y procesar el nuevo XML enviado.
     * Requiere {@code contenidoXml} en el body.
     * Resultado: {@code estadoNovedad} pasa a {@code EstadoNovedad.REEMPLAZADO (2)}
     * y {@code estadoDocumento} pasa a {@code EstadoDocumentoCxp.REGISTRADO_BD (3)}.
     */
    int REEMPLAZAR = 2;
}