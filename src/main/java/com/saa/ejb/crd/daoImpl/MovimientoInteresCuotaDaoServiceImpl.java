package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.MovimientoInteresCuotaDaoService;
import com.saa.model.crd.MovimientoInteresCuota;
import com.saa.rubros.ComponenteMovimientoInteresCuota;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class MovimientoInteresCuotaDaoServiceImpl extends EntityDaoImpl<MovimientoInteresCuota>
        implements MovimientoInteresCuotaDaoService {

    @PersistenceContext
    EntityManager em;

    private static final int TAMANIO_BLOQUE = 900;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) MovimientoInteresCuota");
        return new String[]{
            "codigo", "prestamo", "idCuota", "tipoMovimiento", "componente", "valor",
            "fechaContable", "corrida", "pago", "origen", "idOrigen", "asiento",
            "movimientoReversado", "tipoCartera", "idBanda", "anulado",
            "usuarioRegistro", "fechaRegistro"
        };
    }

    @Override
    public List<Object[]> selectSaldoProvisionadoPorCuotas(List<Long> idsCuota) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectSaldoProvisionadoPorCuotas - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0));
        if (idsCuota == null || idsCuota.isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "select m.idCuota, m.componente, "
            + " sum(case when m.tipoMovimiento in (1, 4, 14) then m.valor"
            + "          when m.tipoMovimiento in (2, 3, 9, 10, 13) then -m.valor"
            + "          else 0.0 end)"
            + " from MovimientoInteresCuota m"
            + " where m.idCuota in :idsCuota and m.anulado = 0"
            + " and m.componente in (" + ComponenteMovimientoInteresCuota.INTERES + ", "
            + ComponenteMovimientoInteresCuota.MORA + ")"
            + " group by m.idCuota, m.componente";

        List<Object[]> resultado = new ArrayList<>();
        for (int inicio = 0; inicio < idsCuota.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsCuota.size()));
            Query query = em.createQuery(jpql);
            query.setParameter("idsCuota", bloque);
            resultado.addAll(query.getResultList());
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectSaldoProvisionadoPorCuotas(List<Long> idsCuota, java.time.LocalDate corte)
            throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectSaldoProvisionadoPorCuotas - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0) + " - corte: " + corte);
        if (corte == null) {
            return selectSaldoProvisionadoPorCuotas(idsCuota);
        }
        if (idsCuota == null || idsCuota.isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "select m.idCuota, m.componente, "
            + " sum(case when m.tipoMovimiento in (1, 4, 14) then m.valor"
            + "          when m.tipoMovimiento in (2, 3, 9, 10, 13) then -m.valor"
            + "          else 0.0 end)"
            + " from MovimientoInteresCuota m"
            + " where m.idCuota in :idsCuota and m.anulado = 0 and m.fechaContable <= :corte"
            + " and m.componente in (" + ComponenteMovimientoInteresCuota.INTERES + ", "
            + ComponenteMovimientoInteresCuota.MORA + ")"
            + " group by m.idCuota, m.componente";
        List<Object[]> resultado = new ArrayList<>();
        for (int inicio = 0; inicio < idsCuota.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsCuota.size()));
            Query query = em.createQuery(jpql);
            query.setParameter("idsCuota", bloque);
            query.setParameter("corte", corte);
            resultado.addAll(query.getResultList());
        }
        return resultado;
    }

    @Override
    public int anularDevengoCobroByPagos(List<Long> idsPago) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.anularDevengoCobroByPagos - pagos: "
            + (idsPago != null ? idsPago.size() : 0));
        if (idsPago == null || idsPago.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int inicio = 0; inicio < idsPago.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsPago.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsPago.size()));
            Query query = em.createQuery(
                "update MovimientoInteresCuota m set m.anulado = 1"
                + " where m.pago.codigo in :idsPago and m.anulado = 0 and m.tipoMovimiento = 5"
                + " and m.origen = 'COBRO'");
            query.setParameter("idsPago", bloque);
            total += query.executeUpdate();
        }
        return total;
    }

    @Override
    public int anularByCorrida(Long idCorrida, String usuario) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.anularByCorrida - corrida: " + idCorrida
            + " - usuario: " + usuario);
        if (idCorrida == null) {
            return 0;
        }
        Query query = em.createQuery(
            "update MovimientoInteresCuota m set m.anulado = 1"
            + " where m.corrida.codigo = :idCorrida and m.anulado = 0"
            + " and m.tipoMovimiento in (1, 5, 7, 11, 12, 13, 14)");
        query.setParameter("idCorrida", idCorrida);
        return query.executeUpdate();
    }

    @Override
    public List<MovimientoInteresCuota> selectVigentesPorPagosYTipos(List<Long> idsPago, List<Long> tipos)
            throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectVigentesPorPagosYTipos - pagos: "
            + (idsPago != null ? idsPago.size() : 0) + " - tipos: " + tipos);
        if (idsPago == null || idsPago.isEmpty() || tipos == null || tipos.isEmpty()) {
            return new ArrayList<>();
        }
        List<MovimientoInteresCuota> resultado = new ArrayList<>();
        for (int inicio = 0; inicio < idsPago.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsPago.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsPago.size()));
            Query query = em.createQuery(
                "select m from MovimientoInteresCuota m"
                + " where m.pago.codigo in :idsPago and m.tipoMovimiento in :tipos and m.anulado = 0"
                + " and not exists (select 1 from MovimientoInteresCuota r"
                + "     where r.movimientoReversado = m and r.anulado = 0)");
            query.setParameter("idsPago", bloque);
            query.setParameter("tipos", tipos);
            resultado.addAll(query.getResultList());
        }
        return resultado;
    }

    @Override
    public java.util.Map<Long, Double> selectSaldoDevengadoPorCuotas(List<Long> idsCuota) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectSaldoDevengadoPorCuotas - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0));
        java.util.Map<Long, Double> resultado = new java.util.HashMap<>();
        if (idsCuota == null || idsCuota.isEmpty()) {
            return resultado;
        }
        String jpql = "select m.idCuota, "
            + " sum(case when m.tipoMovimiento = 5 then m.valor when m.tipoMovimiento = 6 then -m.valor"
            + "          else 0.0 end)"
            + " from MovimientoInteresCuota m"
            + " where m.idCuota in :idsCuota and m.anulado = 0 and m.tipoMovimiento in (5, 6)"
            + " group by m.idCuota";
        for (int inicio = 0; inicio < idsCuota.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsCuota.size()));
            Query query = em.createQuery(jpql);
            query.setParameter("idsCuota", bloque);
            for (Object[] fila : (List<Object[]>) query.getResultList()) {
                Long idCuota = (Long) fila[0];
                double saldo = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
                resultado.put(idCuota, saldo);
            }
        }
        return resultado;
    }

    @Override
    public double selectMoraContabilizadaPendienteDelPrestamo(Long idPrestamo, java.time.LocalDate fecha, Long excluirAcuerdo)
            throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectMoraContabilizadaPendienteDelPrestamo - préstamo: "
            + idPrestamo + " - fecha: " + fecha + " - excluye acuerdo: " + excluirAcuerdo);
        java.sql.Date limiteSig = java.sql.Date.valueOf(fecha.plusDays(1));
        Query query = em.createNativeQuery(
            " SELECT NVL((SELECT SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) "
            + "            FROM CRD.MVIC m WHERE m.PRSTCDGO = :prestamo AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) "
            + "             AND NVL(m.MVICANUL, 0) = 0 AND m.MVICFCCT < :limiteSig), 0) "
            + "      - NVL((SELECT SUM(NVL(g.PGPRMRPG, 0)) FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO "
            + "              WHERE d.PRSTCDGO = :prestamo AND NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < :limiteSig), 0) "
            + "      - NVL((SELECT SUM(NVL(c.DACCVLCN, 0)) FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO "
            + "              WHERE a.PRSTCDGO = :prestamo AND a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < :limiteSig "
            + "                AND (:excluir IS NULL OR a.ACCNCDGO <> :excluir)), 0) "
            + " FROM DUAL");
        query.setParameter("prestamo", idPrestamo);
        query.setParameter("limiteSig", limiteSig);
        query.setParameter("excluir", excluirAcuerdo);
        Object valor = query.getSingleResult();
        return valor != null ? ((Number) valor).doubleValue() : 0.0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> selectMoraDelPrestamoPorCuota(Long idPrestamo, java.time.LocalDate fecha) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectMoraDelPrestamoPorCuota - préstamo: " + idPrestamo + " - fecha: " + fecha);
        java.sql.Date limiteSig = java.sql.Date.valueOf(fecha.plusDays(1));
        Query query = em.createNativeQuery(
            " SELECT d.DTPRCDGO, d.DTPRFCVN, "
            + "  NVL((SELECT SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) FROM CRD.MVIC m "
            + "        WHERE m.DTPRCDGO = d.DTPRCDGO AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND NVL(m.MVICANUL, 0) = 0 AND m.MVICFCCT < :limiteSig), 0), "
            + "  NVL((SELECT SUM(NVL(g.PGPRMRPG, 0)) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < :limiteSig), 0), "
            + "  NVL((SELECT SUM(CASE WHEN m.MVICTPMV IN (1, 4, 14) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10, 13) THEN -m.MVICVLOR ELSE 0 END) FROM CRD.MVIC m "
            + "        WHERE m.DTPRCDGO = d.DTPRCDGO AND m.MVICCMPN = 2 AND NVL(m.MVICANUL, 0) = 0 AND m.MVICFCCT < :limiteSig), 0) "
            + " FROM CRD.DTPR d WHERE d.PRSTCDGO = :prestamo ORDER BY d.DTPRFCVN, d.DTPRCDGO");
        query.setParameter("prestamo", idPrestamo);
        query.setParameter("limiteSig", limiteSig);
        List<Object[]> resultado = new java.util.ArrayList<Object[]>();
        for (Object[] f : (List<Object[]>) query.getResultList()) {
            java.time.LocalDate vence = null;
            if (f[1] instanceof java.sql.Timestamp) {
                vence = ((java.sql.Timestamp) f[1]).toLocalDateTime().toLocalDate();
            } else if (f[1] instanceof java.sql.Date) {
                vence = ((java.sql.Date) f[1]).toLocalDate();
            }
            resultado.add(new Object[]{ ((Number) f[0]).longValue(), vence, ((Number) f[2]).doubleValue(),
                ((Number) f[3]).doubleValue(), ((Number) f[4]).doubleValue() });
        }
        return resultado;
    }

    @Override
    public double selectMoraCondonadaDelPrestamo(Long idPrestamo, java.time.LocalDate fecha) throws Throwable {
        Query query = em.createNativeQuery(
            " SELECT NVL(SUM(NVL(c.DACCVLCN, 0)), 0) FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO "
            + " WHERE a.PRSTCDGO = :prestamo AND a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < :limiteSig");
        query.setParameter("prestamo", idPrestamo);
        query.setParameter("limiteSig", java.sql.Date.valueOf(fecha.plusDays(1)));
        Object valor = query.getSingleResult();
        return valor != null ? ((Number) valor).doubleValue() : 0.0;
    }

    @Override
    public boolean existeVigentePorOrigen(String origen, Long idOrigen) throws Throwable {
        Long n = (Long) em.createQuery("select count(m) from MovimientoInteresCuota m where m.origen = :o and m.idOrigen = :i and m.anulado = 0")
            .setParameter("o", origen).setParameter("i", idOrigen).getSingleResult();
        return n != null && n.longValue() > 0L;
    }

    @Override
    public int anularByOrigenYIdOrigen(String origen, Long idOrigen) throws Throwable {
        return em.createQuery("update MovimientoInteresCuota m set m.anulado = 1 where m.origen = :o and m.idOrigen = :i and m.anulado = 0")
            .setParameter("o", origen).setParameter("i", idOrigen).executeUpdate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.Map<Long, List<Object[]>> selectDevengoMoraDatadoPorCuotas(List<Long> idsCuota) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectDevengoMoraDatadoPorCuotas - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0));
        java.util.Map<Long, List<Object[]>> resultado = new java.util.HashMap<>();
        if (idsCuota == null || idsCuota.isEmpty()) {
            return resultado;
        }
        String jpql = "select m.idCuota, m.fechaContable,"
            + " case when m.tipoMovimiento = 5 then m.valor else -m.valor end"
            + " from MovimientoInteresCuota m"
            + " where m.idCuota in :idsCuota and m.anulado = 0 and m.tipoMovimiento in (5, 6)"
            + " order by m.idCuota, m.fechaContable";
        for (int inicio = 0; inicio < idsCuota.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsCuota.size()));
            Query query = em.createQuery(jpql);
            query.setParameter("idsCuota", bloque);
            for (Object[] fila : (List<Object[]>) query.getResultList()) {
                Long idCuota = (Long) fila[0];
                resultado.computeIfAbsent(idCuota, k -> new java.util.ArrayList<Object[]>())
                    .add(new Object[]{ fila[1], Double.valueOf(((Number) fila[2]).doubleValue()) });
            }
        }
        return resultado;
    }

    @Override
    public boolean existeAlgunoByCorrida(Long idCorrida) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.existeAlgunoByCorrida - corrida: " + idCorrida);
        if (idCorrida == null) {
            return false;
        }
        Query query = em.createQuery(
            "select count(m) from MovimientoInteresCuota m where m.corrida.codigo = :idCorrida");
        query.setParameter("idCorrida", idCorrida);
        Long cantidad = (Long) query.getSingleResult();
        return cantidad != null && cantidad > 0L;
    }

    @Override
    public java.util.Map<Long, Double> selectMoraContabilizadaPendientePorPrestamo(java.time.LocalDate corte)
            throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectMoraContabilizadaPendientePorPrestamo - corte: " + corte);
        java.sql.Date limite = corte != null ? java.sql.Date.valueOf(corte) : java.sql.Date.valueOf("9999-12-31");
        java.sql.Date limiteSig = corte != null ? java.sql.Date.valueOf(corte.plusDays(1))
            : java.sql.Date.valueOf("9999-12-31");
        // Lote 7b: por PRÉSTAMO, sin piso por cuota. En 1.4.02 el negativo de una cuota (mora cobrada o condonada por encima de
        // lo devengado) sí rebaja la cuenta y compensa la mora pendiente de las otras cuotas del préstamo.
        //   pendiente(préstamo) = max(0, Σ cuotas (devengo 5 − 6) − Σ mora cobrada vigente − mora condonada de los acuerdos)
        // La mora cobrada se toma de las cuotas del préstamo por DTPR.PRSTCDGO (PGPR.PRSTCDGO viene nulo en muchos pagos).
        Query query = em.createNativeQuery(
            " WITH dev AS ( "
            + "   SELECT m.PRSTCDGO, SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo "
            + "     FROM CRD.MVIC m "
            + "    WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= :limite "
            + "    GROUP BY m.PRSTCDGO ), "
            + " pag AS ( "
            + "   SELECT d.PRSTCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_pagada "
            + "     FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO "
            + "    WHERE NVL(g.PGPRANUL, 0) = 0 AND (g.PGPRFCHA < :limiteSig OR g.PGPRFCRG < :limiteSig) AND d.PRSTCDGO IN (SELECT PRSTCDGO FROM dev) "
            + "    GROUP BY d.PRSTCDGO ), "
            + " cond AS ( "
            + "   SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada "
            + "     FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO "
            + "    WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < :limiteSig "
            + "    GROUP BY a.PRSTCDGO ) "
            + " SELECT dv.PRSTCDGO, "
            + "        GREATEST(0, ROUND(dv.devengo - NVL(pg.mora_pagada, 0) - NVL(cd.condonada, 0), 2)) "
            + "   FROM dev dv LEFT JOIN pag pg ON pg.PRSTCDGO = dv.PRSTCDGO LEFT JOIN cond cd ON cd.PRSTCDGO = dv.PRSTCDGO");
        query.setParameter("limite", limite);
        query.setParameter("limiteSig", limiteSig);
        java.util.Map<Long, Double> resultado = new java.util.HashMap<>();
        for (Object[] fila : (List<Object[]>) query.getResultList()) {
            double valor = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
            if (valor > 0.0) {
                resultado.put(((Number) fila[0]).longValue(), valor);
            }
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectLibroPorTipoAlCorte(java.time.LocalDate corte) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectLibroPorTipoAlCorte - corte: " + corte);
        Query query = em.createNativeQuery(
            " SELECT pr.TPPRCDGO, m.MVICTPMV, m.MVICCMPN, SUM(m.MVICVLOR) "
            + "   FROM CRD.MVIC m JOIN CRD.PRST p ON p.PRSTCDGO = m.PRSTCDGO JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO "
            + "  WHERE m.MVICANUL = 0 AND m.MVICFCCT <= :corte AND m.MVICCMPN IN (1, 2) "
            + "    AND m.MVICTPMV IN (1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14) AND pr.TPPRCDGO IS NOT NULL "
            + "  GROUP BY pr.TPPRCDGO, m.MVICTPMV, m.MVICCMPN");
        query.setParameter("corte", java.sql.Date.valueOf(corte));
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] f : (List<Object[]>) query.getResultList()) {
            resultado.add(new Object[]{ ((Number) f[0]).longValue(), ((Number) f[1]).longValue(), ((Number) f[2]).longValue(),
                f[3] != null ? ((Number) f[3]).doubleValue() : 0.0 });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectSaldoDevengoInteresPorCuotaAlCorte(java.time.LocalDate corte) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectSaldoDevengoInteresPorCuotaAlCorte - corte: " + corte);
        String expresion = "sum(case when m.tipoMovimiento = 11 then m.valor when m.tipoMovimiento = 12 then -m.valor else 0.0 end)";
        Query query = em.createQuery(
            "select m.idCuota, m.prestamo.codigo, " + expresion
            + " from MovimientoInteresCuota m where m.anulado = 0 and m.tipoMovimiento in (11, 12) and m.fechaContable <= :corte"
            + " group by m.idCuota, m.prestamo.codigo having abs(" + expresion + ") > 0.004");
        query.setParameter("corte", corte);
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] f : (List<Object[]>) query.getResultList()) {
            resultado.add(new Object[]{ f[0], f[1], f[2] != null ? ((Number) f[2]).doubleValue() : 0.0 });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectDevengoPorAsientoYTipo(Long idAsiento) throws Throwable {
        Query query = em.createNativeQuery(
            " SELECT pr.TPPRCDGO, SUM(m.MVICVLOR) FROM CRD.MVIC m JOIN CRD.PRST p ON p.PRSTCDGO = m.PRSTCDGO"
            + " JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO"
            + " WHERE m.ASNTCDGO = :asiento AND m.MVICANUL = 0 AND m.MVICTPMV IN (5, 11) AND pr.TPPRCDGO IS NOT NULL GROUP BY pr.TPPRCDGO");
        query.setParameter("asiento", idAsiento);
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] f : (List<Object[]>) query.getResultList()) {
            resultado.add(new Object[]{ ((Number) f[0]).longValue(), f[1] != null ? ((Number) f[1]).doubleValue() : 0.0 });
        }
        return resultado;
    }

    @Override
    public boolean existeAjusteUltimoRecurso(java.time.LocalDate corte) throws Throwable {
        Query query = em.createQuery("select count(m) from MovimientoInteresCuota m where m.origen = 'AJUSTE_ULTIMO_RECURSO'"
            + " and m.anulado = 0 and m.fechaContable = :corte");
        query.setParameter("corte", corte);
        Long n = (Long) query.getSingleResult();
        return n != null && n > 0L;
    }

    @Override
    public java.util.Map<Long, Long> selectCuotaRespaldoMoraPorPrestamo(java.time.LocalDate corte) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectCuotaRespaldoMoraPorPrestamo - corte: " + corte);
        java.sql.Date limite = corte != null ? java.sql.Date.valueOf(corte) : java.sql.Date.valueOf("9999-12-31");
        Query query = em.createNativeQuery(
            " SELECT PRSTCDGO, DTPRCDGO FROM ( "
            + "   SELECT s.PRSTCDGO, s.DTPRCDGO, ROW_NUMBER() OVER (PARTITION BY s.PRSTCDGO ORDER BY d.DTPRFCVN DESC, s.DTPRCDGO DESC) rn "
            + "     FROM ( SELECT m.PRSTCDGO, m.DTPRCDGO, "
            + "                   SUM(CASE WHEN m.MVICTPMV IN (1, 4, 14) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10, 13) THEN -m.MVICVLOR ELSE 0 END) sal "
            + "              FROM CRD.MVIC m WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICFCCT <= :limite "
            + "             GROUP BY m.PRSTCDGO, m.DTPRCDGO "
            + "            HAVING SUM(CASE WHEN m.MVICTPMV IN (1, 4, 14) THEN m.MVICVLOR WHEN m.MVICTPMV IN (2, 3, 9, 10, 13) THEN -m.MVICVLOR ELSE 0 END) > 0.009 ) s "
            + "     JOIN CRD.DTPR d ON d.DTPRCDGO = s.DTPRCDGO ) "
            + " WHERE rn = 1");
        query.setParameter("limite", limite);
        java.util.Map<Long, Long> resultado = new java.util.HashMap<>();
        for (Object[] f : (List<Object[]>) query.getResultList()) {
            resultado.put(((Number) f[0]).longValue(), ((Number) f[1]).longValue());
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectSaldosProvisionNoCeroAlCorte(java.time.LocalDate corte) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectSaldosProvisionNoCeroAlCorte - corte: " + corte);
        String expresion = "sum(case when m.tipoMovimiento in (1, 4, 14) then m.valor"
            + "                when m.tipoMovimiento in (2, 3, 9, 10, 13) then -m.valor else 0.0 end)";
        Query query = em.createQuery(
            "select m.idCuota, m.prestamo.codigo, m.componente, " + expresion
            + " from MovimientoInteresCuota m"
            + " where m.anulado = 0 and m.componente in (" + ComponenteMovimientoInteresCuota.INTERES + ", "
            + ComponenteMovimientoInteresCuota.MORA + ") and m.fechaContable <= :corte"
            + " group by m.idCuota, m.prestamo.codigo, m.componente"
            + " having abs(" + expresion + ") > 0.004");
        query.setParameter("corte", corte);
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] fila : (List<Object[]>) query.getResultList()) {
            resultado.add(new Object[]{ fila[0], fila[1], fila[2], fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0 });
        }
        return resultado;
    }

    @Override
    public java.util.Set<Long> selectCuotasConMovimientoHasta(List<Long> idsCuota, List<Long> tipos, java.time.LocalDate corte)
            throws Throwable {
        java.util.Set<Long> resultado = new java.util.HashSet<>();
        if (idsCuota == null || idsCuota.isEmpty() || tipos == null || tipos.isEmpty()) {
            return resultado;
        }
        for (int inicio = 0; inicio < idsCuota.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsCuota.size()));
            Query query = em.createQuery(
                "select distinct m.idCuota from MovimientoInteresCuota m"
                + " where m.idCuota in :ids and m.tipoMovimiento in :tipos and m.anulado = 0 and m.fechaContable <= :corte");
            query.setParameter("ids", bloque);
            query.setParameter("tipos", tipos);
            query.setParameter("corte", corte);
            for (Object id : (List<Object>) query.getResultList()) {
                resultado.add(((Number) id).longValue());
            }
        }
        return resultado;
    }

    @Override
    public List<MovimientoInteresCuota> selectVigentesPorOrigen(String origen, Long idOrigen, List<Long> tipos)
            throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectVigentesPorOrigen - origen: " + origen + " - idOrigen: " + idOrigen);
        if (origen == null || idOrigen == null || tipos == null || tipos.isEmpty()) {
            return new ArrayList<>();
        }
        Query query = em.createQuery(
            "select m from MovimientoInteresCuota m"
            + " where m.origen = :origen and m.idOrigen = :idOrigen and m.tipoMovimiento in :tipos and m.anulado = 0"
            + " and not exists (select 1 from MovimientoInteresCuota r"
            + "     where r.movimientoReversado = m and r.anulado = 0)");
        query.setParameter("origen", origen);
        query.setParameter("idOrigen", idOrigen);
        query.setParameter("tipos", tipos);
        return query.getResultList();
    }

    @Override
    public java.util.Map<Long, Double> selectMoraContabilizadaPendientePorPrestamo(java.time.LocalDate corte,
            java.util.Map<Long, Double> devengoAdicional) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectMoraContabilizadaPendientePorPrestamo - corte: " + corte
            + " - préstamos con devengo adicional: " + (devengoAdicional != null ? devengoAdicional.size() : 0));
        if (devengoAdicional == null || devengoAdicional.isEmpty()) {
            return selectMoraContabilizadaPendientePorPrestamo(corte);
        }
        java.sql.Date limite = corte != null ? java.sql.Date.valueOf(corte) : java.sql.Date.valueOf("9999-12-31");
        java.sql.Date limiteSig = corte != null ? java.sql.Date.valueOf(corte.plusDays(1))
            : java.sql.Date.valueOf("9999-12-31");
        // Lote 18: la misma cuenta de la consulta de arriba (devengo 5 − 6, − mora cobrada, − condonada), pero con el devengo adicional
        // sumado antes del piso en cero y con la mora cobrada y la condonada leídas también de los préstamos que solo traen devengo adicional.
        Query qDev = em.createNativeQuery(
            " SELECT m.PRSTCDGO, SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) "
            + "   FROM CRD.MVIC m "
            + "  WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= :limite "
            + "  GROUP BY m.PRSTCDGO");
        qDev.setParameter("limite", limite);
        java.util.Map<Long, java.math.BigDecimal> neto = new java.util.HashMap<>();
        for (Object[] fila : (List<Object[]>) qDev.getResultList()) {
            neto.put(((Number) fila[0]).longValue(), fila[1] != null
                ? java.math.BigDecimal.valueOf(((Number) fila[1]).doubleValue()) : java.math.BigDecimal.ZERO);
        }
        for (java.util.Map.Entry<Long, Double> e : devengoAdicional.entrySet()) {
            neto.merge(e.getKey(), java.math.BigDecimal.valueOf(e.getValue()), java.math.BigDecimal::add);
        }
        List<Long> ids = new ArrayList<>(neto.keySet());
        for (int inicio = 0; inicio < ids.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = ids.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, ids.size()));
            Query qPag = em.createNativeQuery(
                " SELECT d.PRSTCDGO, SUM(NVL(g.PGPRMRPG, 0)) "
                + "   FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO "
                + "  WHERE NVL(g.PGPRANUL, 0) = 0 AND (g.PGPRFCHA < :limiteSig OR g.PGPRFCRG < :limiteSig) AND d.PRSTCDGO IN (:ids) "
                + "  GROUP BY d.PRSTCDGO");
            qPag.setParameter("limiteSig", limiteSig);
            qPag.setParameter("ids", bloque);
            for (Object[] fila : (List<Object[]>) qPag.getResultList()) {
                if (fila[1] != null) {
                    neto.merge(((Number) fila[0]).longValue(), java.math.BigDecimal.valueOf(-((Number) fila[1]).doubleValue()),
                        java.math.BigDecimal::add);
                }
            }
            Query qCon = em.createNativeQuery(
                " SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) "
                + "   FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO "
                + "  WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < :limiteSig AND a.PRSTCDGO IN (:ids) "
                + "  GROUP BY a.PRSTCDGO");
            qCon.setParameter("limiteSig", limiteSig);
            qCon.setParameter("ids", bloque);
            for (Object[] fila : (List<Object[]>) qCon.getResultList()) {
                if (fila[1] != null) {
                    neto.merge(((Number) fila[0]).longValue(), java.math.BigDecimal.valueOf(-((Number) fila[1]).doubleValue()),
                        java.math.BigDecimal::add);
                }
            }
        }
        java.util.Map<Long, Double> resultado = new java.util.HashMap<>();
        for (java.util.Map.Entry<Long, java.math.BigDecimal> e : neto.entrySet()) {
            double valor = e.getValue().setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
            if (valor > 0.0) {
                resultado.put(e.getKey(), valor);
            }
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectCuotasConProvisionMora(java.time.LocalDate corte) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectCuotasConProvisionMora - corte: " + corte);
        String expresion = "sum(case when m.tipoMovimiento in (1, 4, 14) then m.valor"
            + "                when m.tipoMovimiento in (2, 3, 9, 10, 13) then -m.valor else 0.0 end)";
        Query query = em.createQuery(
            "select m.idCuota, m.prestamo.codigo, " + expresion
            + " from MovimientoInteresCuota m"
            + " where m.anulado = 0 and m.componente = " + ComponenteMovimientoInteresCuota.MORA
            + (corte != null ? " and m.fechaContable <= :corte" : "")
            + " group by m.idCuota, m.prestamo.codigo"
            + " having " + expresion + " > 0.009");
        if (corte != null) {
            query.setParameter("corte", corte);
        }
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] fila : (List<Object[]>) query.getResultList()) {
            resultado.add(new Object[]{ fila[0], fila[1], fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0 });
        }
        return resultado;
    }

    @Override
    public List<MovimientoInteresCuota> selectVigentesPorAsientoYTipo(Long idAsiento, long tipoMovimiento)
            throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectVigentesPorAsientoYTipo - asiento: " + idAsiento
            + " - tipo: " + tipoMovimiento);
        if (idAsiento == null) {
            return new ArrayList<>();
        }
        Query query = em.createQuery("select m from MovimientoInteresCuota m"
            + " where m.asiento = :asiento and m.tipoMovimiento = :tipo and m.anulado = 0 order by m.codigo");
        query.setParameter("asiento", idAsiento);
        query.setParameter("tipo", tipoMovimiento);
        return query.getResultList();
    }

    @Override
    public int anularByAsientoYTipo(Long idAsiento, long tipoMovimiento) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.anularByAsientoYTipo - asiento: " + idAsiento
            + " - tipo: " + tipoMovimiento);
        if (idAsiento == null) {
            return 0;
        }
        Query query = em.createQuery("update MovimientoInteresCuota m set m.anulado = 1"
            + " where m.asiento = :asiento and m.tipoMovimiento = :tipo and m.anulado = 0");
        query.setParameter("asiento", idAsiento);
        query.setParameter("tipo", tipoMovimiento);
        return query.executeUpdate();
    }
}
