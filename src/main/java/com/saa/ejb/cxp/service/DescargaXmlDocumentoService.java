package com.saa.ejb.cxp.service;

import java.util.Map;

import jakarta.ejb.Local;

/**
 * Descarga del XML de <b>un</b> DocumentoCxp desde el SRI, en su propia
 * transacción.
 *
 * <p>
 * Es la unidad de trabajo del lote de la fase 1. Existe como bean separado por
 * la razón de siempre: el orquestador necesita que cada documento se confirme o
 * se revierta por su cuenta, y eso solo lo da un {@code REQUIRES_NEW} invocado
 * <b>a través del proxy</b>. Un {@code this.descargarXmlDocumento(...)} dentro
 * del propio orquestador no abriría transacción nueva y el lote entero quedaría
 * en una sola. Ver §8 regla 1 del plan.
 * </p>
 *
 * <p>
 * <b>Qué confirma y qué lanza.</b> Los desenlaces previstos —fuera de ventana,
 * el SRI no lo tiene, no está autorizado, no se pudo conectar— <b>no</b> son
 * excepciones: se estampan en el documento y el método retorna normalmente, así
 * que quedan grabados. Solo lo imprevisto (base, disco, un fallo de parseo que
 * no habíamos contemplado) se propaga, y de eso se encarga el orquestador
 * marcando el documento en estado 4 desde fuera de esta transacción.
 * </p>
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Local
public interface DescargaXmlDocumentoService {

    /** Clave del mapa de retorno: valor de {@code com.saa.rubros.ResultadoDescargaSri}. */
    String CLAVE_RESULTADO = "resultadoSri";

    /** Clave del mapa de retorno: mensaje del SRI o el motivo calculado. */
    String CLAVE_MENSAJE = "mensajeSri";

    /**
     * Clave del mapa de retorno: {@code true} cuando el fallo fue de red y vale
     * la pena que el orquestador lo intente otra vez.
     */
    String CLAVE_REINTENTABLE = "reintentable";

    /** Clave del mapa de retorno: {@code true} si el documento se saltó por idempotencia. */
    String CLAVE_OMITIDO = "omitido";

    /**
     * Clave del mapa de retorno: {@code false} si el XML bajó pero no coincide
     * con lo que declara el TXT.
     */
    String CLAVE_VALIDO = "valido";

    /** Subcarpeta bajo la raíz de subidas donde vive el XML de CXP. */
    String SUBDIRECTORIO_XML = "docs/xml/cxp";

    /**
     * Baja el XML del documento desde el SRI, lo guarda en disco y lo valida
     * contra el TXT reutilizando {@code cargarXmlDocumento}.
     *
     * <p>
     * Es idempotente: un documento ya registrado (estado 3) o que ya tiene
     * {@code pathXml} se salta sin llamar al SRI.
     * </p>
     *
     * @param idDocumentoCxp : Id del DocumentoCxp
     * @param idEmpresa      : Id de la empresa contable
     * @param idUsuario      : Id del usuario que dispara el lote
     * @return               : Mapa con resultadoSri, mensajeSri, reintentable, omitido y valido
     * @throws Throwable     : Solo ante fallos imprevistos; los desenlaces del SRI se graban
     */
    Map<String, Object> descargarXmlDocumento(Long idDocumentoCxp, Long idEmpresa, Long idUsuario)
            throws Throwable;
}
