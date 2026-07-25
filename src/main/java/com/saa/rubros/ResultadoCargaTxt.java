package com.saa.rubros;

/**
 * Rubro 174 — CXP_RESULTADO_CARGA_TXT
 *
 * Resultado que se almacena en DCTX.DCTXRSLT (NUMBER) al procesar
 * cada línea del archivo TXT recibido del SRI.
 * El label legible ("NUEVO", "DUPLICADO", etc.) se obtiene de PDTRVLRV en la tabla SCP.PDTR.
 *
 * Corresponde al campo {@code resultado} de la entidad {@code DetalleCargaTxt}.
 */
public interface ResultadoCargaTxt {

    /** Primera vez que el documento aparece en el sistema. */
    int NUEVO        = 1;

    /** El documento ya existía sin diferencias de valores ni fechas. */
    int DUPLICADO    = 2;

    /** El documento ya existía pero con diferencias en montos o fechas. */
    int NOVEDAD      = 3;

    /** El RUC receptor del documento no coincide con el RUC de la empresa — línea descartada. */
    int IGNORADO     = 4;

    /** Documento activo del período que no apareció en esta nueva carga del TXT. */
    int DESAPARECIDO = 5;
}