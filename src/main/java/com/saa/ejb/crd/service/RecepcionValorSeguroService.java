package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.SolicitudRegistroRecepcionSeguro;
import com.saa.model.crd.RecepcionValorSeguro;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA
 *         Recepción del valor de seguro (sepelio) — CRD.RVSG. Crédito registra, contabilidad
 *         aprueba; al aprobar se genera UN asiento y el valor queda en la cuenta del partícipe.
 *         Ver {@code docs/logica-negocio/crd/API-RECEPCION-VALORES-SEGURO.md}.
 *
 *         Los errores de negocio salen como {@code IncomeException} cuyo mensaje empieza con uno
 *         de los prefijos de abajo, para que el REST elija el código HTTP sin adivinar.
 */
@Local
public interface RecepcionValorSeguroService extends EntityService<RecepcionValorSeguro> {

    /** Falta un obligatorio o un dato es inválido (HTTP 400). */
    String ERR_VALIDACION = "RVSG_VALIDACION";

    /** La recepción no existe (HTTP 404). */
    String ERR_NO_ENCONTRADA = "RVSG_NO_ENCONTRADA";

    /**
     * Registra la recepción en estado REGISTRADO. No genera asiento y no toca el saldo del
     * partícipe: eso pasa al aprobar.
     */
    RecepcionValorSeguro registrar(SolicitudRegistroRecepcionSeguro solicitud) throws Throwable;

    /**
     * Aprueba una recepción REGISTRADA, todo en una sola transacción: genera UN asiento (D cuenta
     * contable de la cuenta bancaria / H cuentaPasivo de CRD.CTAP), registra el aporte positivo
     * del tipo indicado y pasa a APROBADO.
     *
     * Con la contabilidad de CRD apagada NO aprueba (rechaza sin tocar nada): aprobar sin
     * asiento dejaría plata en el saldo del partícipe sin contabilizar.
     */
    RecepcionValorSeguro aprobar(Long idRecepcion, String usuario) throws Throwable;

    /** Rechaza una recepción REGISTRADA. Motivo obligatorio. No hay asiento que revertir. */
    RecepcionValorSeguro rechazar(Long idRecepcion, String usuario, String motivo) throws Throwable;

    /**
     * Anula una recepción APROBADA: reversa el aporte y anula el asiento. Si el partícipe ya usó
     * ese dinero (saldo insuficiente) rechaza sin tocar nada, en vez de dejar saldo negativo.
     * Motivo obligatorio.
     */
    RecepcionValorSeguro anular(Long idRecepcion, String usuario, String motivo) throws Throwable;
}
