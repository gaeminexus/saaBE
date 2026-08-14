package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.EventoPrestamo;

import jakarta.ejb.Local;

/**
 * Interface DAO para EventoPrestamo (EVPR).
 *
 * @author Sistema SAA
 * @since 2026-08-13
 */
@Local
public interface EventoPrestamoDaoService extends EntityDao<EventoPrestamo> {

    /**
     * Eventos de un préstamo, del más reciente al más antiguo (EVPRCDGO DESC).
     *
     * @param codigoPrestamo Código del préstamo
     * @return Lista de eventos; vacía si no hay o si falla la consulta
     * @throws Throwable Si ocurre un error
     */
    List<EventoPrestamo> selectByPrestamo(Long codigoPrestamo) throws Throwable;

    /**
     * Eventos VIGENTES (estado = 1) de un préstamo con código MAYOR al indicado.
     * Se usa en el reverso para exigir el orden LIFO: no se puede anular un evento si
     * existen operaciones posteriores vigentes sobre el mismo préstamo.
     *
     * @param codigoPrestamo Código del préstamo
     * @param codigoEvento   Código del evento que se pretende anular
     * @return Lista de eventos posteriores vigentes; vacía si no hay
     * @throws Throwable Si ocurre un error
     */
    List<EventoPrestamo> selectVigentesPosterioresByPrestamo(Long codigoPrestamo, Long codigoEvento) throws Throwable;
}
