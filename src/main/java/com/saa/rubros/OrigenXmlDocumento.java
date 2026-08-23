package com.saa.rubros;

/**
 * Origen del XML de un DocumentoCxp (PGS.DCXP.DCXPORXM).
 *
 * <p>
 * No hay catálogo en Rubro/DetalleRubro para esto: la columna guarda el número
 * directamente. Un valor <b>nulo</b> significa "manual, anterior a la carga
 * automática" — los documentos históricos se cargaron a mano y no se van a
 * rellenar hacia atrás.
 * </p>
 */
public interface OrigenXmlDocumento {

    /** El usuario subió el XML desde la pantalla. */
    long MANUAL = 1L;

    /** Lo bajó el servicio de autorización del SRI. */
    long SRI    = 2L;
}
