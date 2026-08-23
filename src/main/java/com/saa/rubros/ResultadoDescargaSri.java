package com.saa.rubros;

/**
 * Resultado del último intento de descarga del XML desde el SRI
 * (PGS.DCXP.DCXPRSRI, VARCHAR2(30)).
 *
 * <p>
 * Son cadenas y no códigos numéricos porque el frontend los muestra tal cual y
 * porque no existe catálogo en Rubro/DetalleRubro para ellos. El largo de la
 * columna da margen: la más larga de las cinco tiene 14 caracteres.
 * </p>
 */
public interface ResultadoDescargaSri {

    /** El SRI devolvió el comprobante autorizado y quedó guardado en disco. */
    String DESCARGADO     = "DESCARGADO";

    /**
     * La fecha de emisión es anterior a la ventana del SRI (un mes hacia atrás,
     * por día del mes) y no se llegó a gastar la llamada de red. Ver §2 del plan:
     * el servicio devuelve {@code numeroComprobantes=0} sin decir por qué, así
     * que el diagnóstico lo hacemos nosotros antes de llamar.
     */
    String FUERA_VENTANA  = "FUERA_VENTANA";

    /** Dentro de la ventana, pero el SRI no tiene el comprobante. */
    String NO_ENCONTRADO  = "NO_ENCONTRADO";

    /** El SRI lo tiene pero su estado no es AUTORIZADO. */
    String NO_AUTORIZADO  = "NO_AUTORIZADO";

    /** No se pudo hablar con el servicio: red, TLS, timeout o SOAP fault. */
    String ERROR_CONEXION = "ERROR_CONEXION";
}
