package com.saa.ejb.cxp.service;

import jakarta.ejb.Local;

/**
 * Estampa el estado ERROR (4) y la observación del fallo sobre un
 * {@code DocumentoCxp} en una transacción propia.
 *
 * <p>
 * Existe como bean <b>separado</b> por una razón concreta, no por prolijidad:
 * {@code ProcesoCargaDocumentosServiceImpl} marcaba el error dentro del
 * {@code catch} y re-lanzaba, de modo que el rollback de esa misma transacción
 * se llevaba también el marcado — el documento nunca quedaba en estado 4 y la
 * observación del error se perdía. Con la carga por lote eso es inaceptable:
 * un error que no se persiste, en un proceso que nadie está mirando, es un
 * error que no existió.
 * </p>
 *
 * <p>
 * <b>Trampa de EJB — no mover este método a la clase que lo llama.</b> El
 * {@code REQUIRES_NEW} lo aplica el interceptor del contenedor, que solo
 * interviene cuando la llamada pasa por el proxy. Un {@code this.marcarError(...)}
 * dentro del mismo bean se salta el interceptor, corre en la transacción del
 * llamador y el marcado se revierte igual: el defecto quedaría idéntico y sin
 * ninguna señal de que no funciona.
 * </p>
 *
 * <p>
 * Ver {@code docs/logica-negocio/cxp/PLAN-CARGA-AUTOMATICA-SRI.md} §7 (fase 0.1)
 * y §8 regla 1.
 * </p>
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Local
public interface MarcadoErrorDocumentoService {

    /**
     * Graba {@code DCXPESTD = 4} (ERROR) y {@code DCXPOBSR = mensaje} en el
     * documento, en una transacción nueva que se confirma de inmediato, aunque
     * la transacción del llamador termine en rollback.
     *
     * <p>
     * <b>No propaga excepciones.</b> Se invoca desde el {@code catch} del
     * proceso de registro: si el marcado fallara y se lanzara, taparía el error
     * de negocio original, que es el que el usuario necesita ver. Todo fallo
     * del marcado se reporta por {@code System.err} y el método retorna.
     * </p>
     *
     * @param idDocumentoCxp : Id del DocumentoCxp a marcar
     * @param mensaje        : Texto del error; se recorta al largo de DCXPOBSR (2000)
     */
    void marcarError(Long idDocumentoCxp, String mensaje);
}
