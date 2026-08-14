package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.HistDetallePrestamo;

import jakarta.ejb.Local;

/**
 * Interface Service para HistDetallePrestamo (HDTP).
 *
 * Las cuotas historizadas SOLO las escribe el abono a capital / la precancelación;
 * esta capa es de consulta.
 *
 * @author Sistema SAA
 * @since 2026-08-13
 */
@Local
public interface HistDetallePrestamoService extends EntityService<HistDetallePrestamo> {

    /**
     * Lista las cuotas historizadas por un evento, ordenadas por número de cuota.
     *
     * @param codigoEvento Código del EventoPrestamo (EVPR)
     * @return Lista de cuotas historizadas (vacía si no hay)
     * @throws Throwable Si ocurre un error
     */
    List<HistDetallePrestamo> listarPorEvento(Long codigoEvento) throws Throwable;

    /**
     * Lista todas las cuotas historizadas de un préstamo.
     *
     * @param codigoPrestamo Código del préstamo
     * @return Lista de cuotas historizadas (vacía si no hay)
     * @throws Throwable Si ocurre un error
     */
    List<HistDetallePrestamo> listarPorPrestamo(Long codigoPrestamo) throws Throwable;
}
