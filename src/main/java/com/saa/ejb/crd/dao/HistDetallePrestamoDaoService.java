package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.HistDetallePrestamo;

import jakarta.ejb.Local;

/**
 * Interface DAO para HistDetallePrestamo (HDTP).
 *
 * @author Sistema SAA
 * @since 2026-08-13
 */
@Local
public interface HistDetallePrestamoDaoService extends EntityDao<HistDetallePrestamo> {

    /**
     * Cuotas historizadas por un evento, ordenadas por número de cuota ASC.
     * Es la lista que el reverso de un ABONO_CAPITAL restaura en CRD.DTPR.
     *
     * @param codigoEvento Código del EventoPrestamo (EVPR)
     * @return Lista de cuotas historizadas; vacía si no hay o si falla la consulta
     * @throws Throwable Si ocurre un error
     */
    List<HistDetallePrestamo> selectByEvento(Long codigoEvento) throws Throwable;

    /**
     * Todas las cuotas historizadas de un préstamo, ordenadas por evento y número de cuota.
     *
     * @param codigoPrestamo Código del préstamo
     * @return Lista de cuotas historizadas; vacía si no hay o si falla la consulta
     * @throws Throwable Si ocurre un error
     */
    List<HistDetallePrestamo> selectByPrestamo(Long codigoPrestamo) throws Throwable;

    /**
     * Menor número de cuota historizado por un evento (MIN DTPRNMCT).
     * El reverso de un ABONO_CAPITAL lo usa para identificar qué cuotas vivas de DTPR
     * fueron generadas por ese evento (las de numeroCuota >= este valor).
     *
     * @param codigoEvento Código del EventoPrestamo (EVPR)
     * @return Menor número de cuota historizado, o null si el evento no historizó cuotas
     * @throws Throwable Si ocurre un error
     */
    Double selectMinNumeroCuotaByEvento(Long codigoEvento) throws Throwable;
}
