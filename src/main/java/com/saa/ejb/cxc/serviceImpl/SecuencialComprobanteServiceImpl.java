package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.service.SecuencialComprobanteService;
import com.saa.model.cxc.NumeracionPuntoEmision;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/**
 * @see SecuencialComprobanteService
 * @author Sistema SAA
 * @since 2026-11-25
 */
@Stateless
public class SecuencialComprobanteServiceImpl implements SecuencialComprobanteService {

    /** Milisegundos que espera el bloqueo de la fila antes de fallar (una emisión es corta: más de esto es un bloqueo colgado). */
    private static final int ESPERA_BLOQUEO_MS = 30000;

    @PersistenceContext
    private EntityManager em;

    @Override
    public String siguienteSecuencial(Long idPtoEmision, String tipoDoc) throws Exception {
        System.out.println("SecuencialComprobanteService.siguienteSecuencial - punto: " + idPtoEmision + " - tipo: " + tipoDoc);
        List<NumeracionPuntoEmision> filas = em.createQuery(
                "SELECT n FROM NumeracionPuntoEmision n WHERE n.ptoEmision.id = :ptoEmision AND n.tipoDoc = :tipoDoc",
                NumeracionPuntoEmision.class)
            .setParameter("ptoEmision", idPtoEmision)
            .setParameter("tipoDoc", tipoDoc)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint("jakarta.persistence.lock.timeout", ESPERA_BLOQUEO_MS)
            .getResultList();
        if (filas.isEmpty()) {
            throw new IncomeException("No existe numeración para el punto de emisión " + idPtoEmision + " y tipo de documento " + tipoDoc);
        }
        if (filas.size() > 1) {
            throw new IncomeException("Hay " + filas.size() + " filas de numeración para el punto de emisión " + idPtoEmision
                + " y tipo de documento " + tipoDoc + ": solo puede haber una.");
        }
        NumeracionPuntoEmision numeracion = filas.get(0);
        // Con el bloqueo ya tomado, se relee el valor de la base: si la fila estaba en el contexto de persistencia de esta transacción, su valor puede
        // ser anterior al commit de otra emisión.
        em.refresh(numeracion, LockModeType.PESSIMISTIC_WRITE);
        Long numeroActual = numeracion.getNumActual();
        if (numeroActual == null) {
            throw new IncomeException("La numeración del punto de emisión " + idPtoEmision + " y tipo " + tipoDoc + " no tiene número actual.");
        }
        numeracion.setNumActual(numeroActual + 1);
        em.flush();
        String secuencial = String.format("%09d", numeroActual);
        System.out.println("Secuencial generado: " + secuencial);
        return secuencial;
    }
}
