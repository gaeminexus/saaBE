package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.model.crd.HistDetallePrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para HistDetallePrestamo (HDTP).
 *
 * @author Sistema SAA
 * @since 2026-08-13
 */
@Stateless
public class HistDetallePrestamoDaoServiceImpl extends EntityDaoImpl<HistDetallePrestamo>
        implements HistDetallePrestamoDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<HistDetallePrestamo> selectByEvento(Long codigoEvento) throws Throwable {
        System.out.println("HistDetallePrestamoDaoService.selectByEvento - Evento: " + codigoEvento);

        try {
            Query query = em.createQuery(
                "SELECT h FROM HistDetallePrestamo h " +
                "WHERE h.eventoPrestamo.codigo = :codigoEvento " +
                "ORDER BY h.numeroCuota ASC"
            );
            query.setParameter("codigoEvento", codigoEvento);

            List<HistDetallePrestamo> resultados = query.getResultList();
            System.out.println("  Cuotas historizadas encontradas: " + (resultados != null ? resultados.size() : 0));
            return resultados;

        } catch (Exception e) {
            System.err.println("ERROR en selectByEvento de HistDetallePrestamo: " + e.getMessage());
            e.printStackTrace();
            // NO lanzar excepción - retornar lista vacía para no detener el proceso
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<HistDetallePrestamo> selectByPrestamo(Long codigoPrestamo) throws Throwable {
        System.out.println("HistDetallePrestamoDaoService.selectByPrestamo - Préstamo: " + codigoPrestamo);

        try {
            Query query = em.createQuery(
                "SELECT h FROM HistDetallePrestamo h " +
                "WHERE h.prestamo.codigo = :codigoPrestamo " +
                "ORDER BY h.eventoPrestamo.codigo ASC, h.numeroCuota ASC"
            );
            query.setParameter("codigoPrestamo", codigoPrestamo);

            List<HistDetallePrestamo> resultados = query.getResultList();
            System.out.println("  Cuotas historizadas encontradas: " + (resultados != null ? resultados.size() : 0));
            return resultados;

        } catch (Exception e) {
            System.err.println("ERROR en selectByPrestamo de HistDetallePrestamo: " + e.getMessage());
            e.printStackTrace();
            // NO lanzar excepción - retornar lista vacía para no detener el proceso
            return new ArrayList<>();
        }
    }

    @Override
    public Double selectMinNumeroCuotaByEvento(Long codigoEvento) throws Throwable {
        System.out.println("HistDetallePrestamoDaoService.selectMinNumeroCuotaByEvento - Evento: " + codigoEvento);

        try {
            Query query = em.createQuery(
                "SELECT MIN(h.numeroCuota) FROM HistDetallePrestamo h " +
                "WHERE h.eventoPrestamo.codigo = :codigoEvento"
            );
            query.setParameter("codigoEvento", codigoEvento);

            Object resultado = query.getSingleResult();
            Double minimo = resultado != null ? ((Number) resultado).doubleValue() : null;
            System.out.println("  Mínimo número de cuota historizado: " + minimo);
            return minimo;

        } catch (Exception e) {
            System.err.println("ERROR en selectMinNumeroCuotaByEvento: " + e.getMessage());
            e.printStackTrace();
            // NO lanzar excepción - retornar null para no detener el proceso
            return null;
        }
    }
}
