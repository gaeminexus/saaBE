package com.saa.ejb.rpr.serviceImpl;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import com.saa.ejb.rpr.service.ActualizacionEjecucionGService;
import com.saa.ejb.rpr.service.ReglaActualizacionG;
import com.saa.model.rpr.EjecucionReporte;
import com.saa.rubros.EstadoCorridaCierreCartera;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/** C50: ver {@link ActualizacionEjecucionGService}. Solo lectura sobre CRD.CRCT y CNT.ASNT/DTAS. */
@Stateless
@SuppressWarnings("unchecked")
public class ActualizacionEjecucionGServiceImpl implements ActualizacionEjecucionGService {

    @PersistenceContext
    EntityManager em;

    @Override
    public void atar(EjecucionReporte ejrc) throws Throwable {
        System.out.println("ActualizacionEjecucionGService.atar - mes: " + ejrc.getMes() + " - anio: " + ejrc.getAnio());
        ejrc.setGeneradaEn(LocalDateTime.now());
        ejrc.setCorridaCartera(corridaVigente(ejrc.getAnio(), ejrc.getMes()));
        Object[] ultimo = ultimoAsiento(ejrc.getAnio(), ejrc.getMes());
        ejrc.setUltimoAsientoRegistro(ultimo != null ? (LocalDateTime) ultimo[2] : null);
    }

    @Override
    public ReglaActualizacionG.Resultado evaluar(EjecucionReporte ejrc) throws Throwable {
        System.out.println("ActualizacionEjecucionGService.evaluar - ejecucion: " + ejrc.getCodigo());
        Long vigente = corridaVigente(ejrc.getAnio(), ejrc.getMes());
        Object[] ultimo = ultimoAsiento(ejrc.getAnio(), ejrc.getMes());
        LocalDateTime ultimoAhora = ultimo != null ? (LocalDateTime) ultimo[2] : null;
        ReglaActualizacionG.Resultado r = ReglaActualizacionG.evaluar(ejrc.getCorridaCartera(), vigente,
                ejrc.getUltimoAsientoRegistro(), ejrc.getGeneradaEn(), ultimoAhora);
        if (r.estaDesactualizada() && ultimo != null && vigente != null && ejrc.getCorridaCartera() != null
                && vigente.longValue() == ejrc.getCorridaCartera().longValue()) {
            // Motivo (b): se agrega el asiento (número y fecha contable) para que el semáforo diga cuál es.
            return ReglaActualizacionG.conAsiento(r, ultimo[1], ultimo[3]);
        }
        return r;
    }

    @Override
    public ReglaActualizacionG.Resultado evaluarUltima(Long anio, Long mes) throws Throwable {
        List<Object> filas = em.createNativeQuery(
                " SELECT MAX(EJRCCDGO) FROM RPR.EJRC WHERE EJRCANOO = :anio AND EJRCMESS = :mes")
                .setParameter("anio", anio).setParameter("mes", mes).getResultList();
        Object codigo = filas.isEmpty() ? null : filas.get(0);
        if (codigo == null) {
            return null;
        }
        EjecucionReporte ejrc = em.find(EjecucionReporte.class, ((Number) codigo).longValue());
        return ejrc != null ? evaluar(ejrc) : null;
    }

    /** Corrida de cartera vigente del mes: EJECUTADA y activa (la misma que el control de cierre usa para el aviso de desactualización). */
    Long corridaVigente(Long anio, Long mes) {
        Object r = em.createNativeQuery(
                " SELECT MAX(CRCTCDGO) FROM CRD.CRCT WHERE CRCTANOO = :anio AND CRCTMESS = :mes AND CRCTESTD = 1 AND CRCTIDST = "
                + EstadoCorridaCierreCartera.EJECUTADA)
                .setParameter("anio", anio).setParameter("mes", mes).getSingleResult();
        return r instanceof Number ? ((Number) r).longValue() : null;
    }

    /**
     * Último asiento VIGENTE (estado 1 o 3) del mes con alguna línea en 1.3, 1.4 o 2.1, por fecha de ingreso ({@code ASNTFCPR}).
     * {@code [codigo, numero, fechaIngreso, fechaContable]}; {@code null} si no hay.
     */
    Object[] ultimoAsiento(Long anio, Long mes) {
        YearMonth ym = YearMonth.of(anio.intValue(), mes.intValue());
        LocalDate inicio = ym.atDay(1);
        LocalDate finSig = ym.atEndOfMonth().plusDays(1);
        Query q = em.createNativeQuery(
                " SELECT * FROM (SELECT a.ASNTCDGO, a.ASNTNMRO, a.ASNTFCPR, a.ASNTFCHA FROM CNT.ASNT a"
                + "  WHERE a.ASNTESTD IN (1, 3) AND a.ASNTFCHA >= :inicio AND a.ASNTFCHA < :finSig AND a.ASNTFCPR IS NOT NULL"
                + "    AND EXISTS (SELECT 1 FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ASNTCDGO AND (d.DTASCNTA LIKE '1.3.%'"
                + "         OR d.DTASCNTA LIKE '1.4.%' OR d.DTASCNTA LIKE '2.1.%'))"
                + "  ORDER BY a.ASNTFCPR DESC) WHERE ROWNUM = 1");
        q.setParameter("inicio", java.sql.Date.valueOf(inicio));
        q.setParameter("finSig", java.sql.Date.valueOf(finSig));
        List<Object[]> filas = q.getResultList();
        if (filas.isEmpty()) {
            return null;
        }
        Object[] f = filas.get(0);
        LocalDateTime ingreso = f[2] instanceof Timestamp ? ((Timestamp) f[2]).toLocalDateTime() : null;
        return new Object[]{ f[0], f[1], ingreso, f[3] };
    }

}
