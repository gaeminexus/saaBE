package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.EventoPrestamo;

import jakarta.ejb.Local;

/**
 * Interface Service para EventoPrestamo (EVPR).
 *
 * Los eventos SOLO se crean y anulan desde los procesos de pago
 * (ProcesoPagoPrestamoService / AbonoCapitalPrestamoService); esta capa es de consulta.
 *
 * @author Sistema SAA
 * @since 2026-08-13
 */
@Local
public interface EventoPrestamoService extends EntityService<EventoPrestamo> {

    /**
     * Lista los eventos de un préstamo, del más reciente al más antiguo.
     *
     * @param codigoPrestamo Código del préstamo
     * @return Lista de eventos (vacía si no hay)
     * @throws Throwable Si ocurre un error
     */
    List<EventoPrestamo> listarPorPrestamo(Long codigoPrestamo) throws Throwable;
}
