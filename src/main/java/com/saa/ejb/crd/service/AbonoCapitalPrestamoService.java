package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.ResultadoAbonoCapital;
import com.saa.ejb.crd.service.dto.SimulacionAbonoCapital;
import com.saa.ejb.crd.service.dto.SolicitudAbonoCapital;

import jakarta.ejb.Local;

/**
 * Abono a capital con re-amortización (§7.3 de ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md).
 *
 * Reemplaza por completo al antiguo {@code PrestamoServiceImpl.aplicarAbonoCapital}, que era
 * defectuoso (usaba saldoInicialCapital cuando estaba NULL, consideraba pendientes solo los
 * estados 2/5, recalculaba siempre en francés, borraba cuotas sin respaldo, no creaba
 * PagoPrestamo y no actualizaba el plazo).
 *
 * Modalidades:
 * <ul>
 *   <li><b>1</b> = mantener el valor de cuota y <b>reducir el plazo</b></li>
 *   <li><b>2</b> = mantener el plazo y <b>reducir el valor de la cuota</b></li>
 * </ul>
 *
 * El abono se registra en {@code DTPRSLOT} (saldoOtros) de la cuota ancla y en
 * {@code PGPRSLOT} del PagoPrestamo, NUNCA en {@code capitalPagado}: así no contamina la
 * reconstrucción de saldos por cuota del motor (§6.2).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Local
public interface AbonoCapitalPrestamoService {

    /** 422 - El préstamo tiene cuotas vencidas o parciales */
    String ERR_PRESTAMO_NO_AL_DIA = "PRESTAMO_NO_AL_DIA";
    /** 422 - El abono cubre todo el capital: corresponde precancelar */
    String ERR_ABONO_CUBRE_CAPITAL = "ABONO_CUBRE_CAPITAL";
    /** 422 - Con la cuota vigente el préstamo nunca se amortizaría (la cuota no cubre el interés) */
    String ERR_CUOTA_NO_CUBRE_INTERES = "CUOTA_NO_CUBRE_INTERES";
    /** 422 - Modalidad distinta de 1 o 2 */
    String ERR_MODALIDAD_INVALIDA = "MODALIDAD_INVALIDA";
    /** 422 - El préstamo no tiene cuotas pendientes que re-amortizar */
    String ERR_SIN_CUOTAS_PENDIENTES = "SIN_CUOTAS_PENDIENTES";

    /**
     * Calcula cómo quedaría el préstamo tras el abono, SIN escribir nada.
     *
     * @param idPrestamo Código del préstamo
     * @param valor      Monto del abono
     * @param modalidad  1 = reduce plazo; 2 = reduce cuota
     * @return Simulación con la tabla proyectada
     * @throws Throwable Si ocurre un error
     */
    SimulacionAbonoCapital simular(Long idPrestamo, double valor, int modalidad) throws Throwable;

    /**
     * Aplica el abono a capital en UNA transacción: crea el EventoPrestamo, acumula el monto en
     * {@code DTPRSLOT} de la cuota ancla, crea el PagoPrestamo del abono, historiza las cuotas
     * pendientes en CRD.HDTP, las borra de CRD.DTPR, genera la nueva tabla y actualiza el
     * préstamo.
     *
     * @param solicitud Préstamo, valor, modalidad, usuario, observación y fecha
     * @return Resultado con plazos/cuotas anterior y nuevo y los conteos de cuotas
     * @throws Throwable Si ocurre un error
     */
    ResultadoAbonoCapital aplicar(SolicitudAbonoCapital solicitud) throws Throwable;
}
