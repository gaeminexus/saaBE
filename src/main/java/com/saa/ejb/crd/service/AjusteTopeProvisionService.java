package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.ResultadoAjusteTopeMora;
import com.saa.ejb.crd.service.dto.SolicitudAjusteTopeMora;

import jakarta.ejb.Local;

/**
 * Lote 7 (D-186): graba en el libro MVIC, por cuota, el ajuste (tipo 10 {@code AJUSTE_TOPE_PROVISION}) del exceso de provisión de
 * mora que el cuadre ya contabilizó en un asiento (D 1.4.99.05 / H 4.7.05.10), para que el paso ⑦ no vuelva a provisionar lo
 * reversado. Usa la MISMA regla del ⑦: {@code tope = min(mora calculada pendiente, mora contabilizada pendiente)} y
 * {@code ajuste = max(0, saldo de mora provisionado − tope)} por cuota.
 *
 * <p>Contrato: docs/logica-negocio/crd/API-AJUSTE-TOPE-PROVISION-MORA.md.</p>
 */
@Local
public interface AjusteTopeProvisionService {

    /**
     * Calcula el ajuste al corte y, con {@code grabar = true}, lo graba. Valida contra el asiento (ACTIVO, fecha = corte, por cuenta de
     * la 1.4.99.05 de la plantilla 36 papel 81, tolerancia 0,01): si no cuadra, no graba. Idempotente por asiento.
     */
    ResultadoAjusteTopeMora ajustar(SolicitudAjusteTopeMora solicitud) throws Throwable;

    /** Anula (MVICANUL = 1, nunca borra) los ajustes tipo 10 de un asiento. @return cuántos movimientos anuló */
    int anular(Long idAsiento, String usuario) throws Throwable;
}
