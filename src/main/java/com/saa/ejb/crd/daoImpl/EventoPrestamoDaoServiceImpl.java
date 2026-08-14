package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.model.crd.EventoPrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para EventoPrestamo (EVPR).
 *
 * @author Sistema SAA
 * @since 2026-08-13
 */
@Stateless
public class EventoPrestamoDaoServiceImpl extends EntityDaoImpl<EventoPrestamo>
        implements EventoPrestamoDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<EventoPrestamo> selectByPrestamo(Long codigoPrestamo) throws Throwable {
        System.out.println("EventoPrestamoDaoService.selectByPrestamo - Préstamo: " + codigoPrestamo);

        try {
            Query query = em.createQuery(
                "SELECT e FROM EventoPrestamo e " +
                "WHERE e.prestamo.codigo = :codigoPrestamo " +
                "ORDER BY e.codigo DESC"
            );
            query.setParameter("codigoPrestamo", codigoPrestamo);

            List<EventoPrestamo> resultados = query.getResultList();
            System.out.println("  Eventos encontrados: " + (resultados != null ? resultados.size() : 0));
            return resultados;

        } catch (Exception e) {
            System.err.println("ERROR en selectByPrestamo de EventoPrestamo: " + e.getMessage());
            e.printStackTrace();
            // NO lanzar excepción - retornar lista vacía para no detener el proceso
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<EventoPrestamo> selectVigentesPosterioresByPrestamo(Long codigoPrestamo, Long codigoEvento)
            throws Throwable {
        System.out.println("EventoPrestamoDaoService.selectVigentesPosterioresByPrestamo - Préstamo: "
            + codigoPrestamo + " - Evento: " + codigoEvento);

        try {
            Query query = em.createQuery(
                "SELECT e FROM EventoPrestamo e " +
                "WHERE e.prestamo.codigo = :codigoPrestamo " +
                "AND e.codigo > :codigoEvento " +
                "AND e.estado = :estadoVigente " +
                "ORDER BY e.codigo ASC"
            );
            query.setParameter("codigoPrestamo", codigoPrestamo);
            query.setParameter("codigoEvento", codigoEvento);
            query.setParameter("estadoVigente", 1L);

            List<EventoPrestamo> resultados = query.getResultList();
            System.out.println("  Eventos posteriores vigentes: " + (resultados != null ? resultados.size() : 0));
            return resultados;

        } catch (Exception e) {
            System.err.println("ERROR en selectVigentesPosterioresByPrestamo: " + e.getMessage());
            e.printStackTrace();
            // NO lanzar excepción - retornar lista vacía para no detener el proceso
            return new ArrayList<>();
        }
    }
}
