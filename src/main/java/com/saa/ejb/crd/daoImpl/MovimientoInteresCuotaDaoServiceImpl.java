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
            + " sum(case when m.tipoMovimiento in (1, 4) then m.valor"
            + "          when m.tipoMovimiento in (2, 3, 9, 10) then -m.valor"
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
            + " sum(case when m.tipoMovimiento in (1, 4) then m.valor"
            + "          when m.tipoMovimiento in (2, 3, 9, 10) then -m.valor"
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
            + " and m.tipoMovimiento in (1, 5, 7)");
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
    public java.util.Map<Long, Double> selectMoraContabilizadaPendientePorCuota(java.time.LocalDate corte)
            throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectMoraContabilizadaPendientePorCuota - corte: " + corte);
        java.sql.Date limite = corte != null ? java.sql.Date.valueOf(corte) : java.sql.Date.valueOf("9999-12-31");
        java.sql.Date limiteSig = corte != null ? java.sql.Date.valueOf(corte.plusDays(1))
            : java.sql.Date.valueOf("9999-12-31");
        // Una fila por cuota con devengo de mora (5 − 6) > 0. pend = devengo − mora cobrada (piso 0); la mora condonada del
        // préstamo (acuerdos APLICADOS, concepto 3 = mora) se asigna a las cuotas de la más vieja a la más nueva con una
        // suma acumulada: asignado_i = min(pend_i, max(0, condonada − (acumulado_i − pend_i))).
        Query query = em.createNativeQuery(
            " WITH dev AS ( "
            + "   SELECT m.DTPRCDGO, m.PRSTCDGO, "
            + "          SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) AS devengo "
            + "     FROM CRD.MVIC m "
            + "    WHERE m.MVICANUL = 0 AND m.MVICCMPN = 2 AND m.MVICTPMV IN (5, 6) AND m.MVICFCCT <= :limite "
            + "    GROUP BY m.DTPRCDGO, m.PRSTCDGO "
            + "   HAVING SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) > 0 ), "
            + " pag AS ( "
            + "   SELECT g.DTPRCDGO, SUM(NVL(g.PGPRMRPG, 0)) AS mora_pagada "
            + "     FROM CRD.PGPR g "
            + "    WHERE NVL(g.PGPRANUL, 0) = 0 AND g.PGPRFCHA < :limiteSig "
            + "    GROUP BY g.DTPRCDGO ), "
            + " cond AS ( "
            + "   SELECT a.PRSTCDGO, SUM(NVL(c.DACCVLCN, 0)) AS condonada "
            + "     FROM CRD.DACC c JOIN CRD.ACCN a ON a.ACCNCDGO = c.ACCNCDGO "
            + "    WHERE a.ACCNESTD = 2 AND c.DACCCPTO = 3 AND a.ACCNFCHA < :limiteSig "
            + "    GROUP BY a.PRSTCDGO ), "
            + " base AS ( "
            + "   SELECT dv.DTPRCDGO, dv.PRSTCDGO, d.DTPRFCVN, "
            + "          GREATEST(0, ROUND(dv.devengo - NVL(pg.mora_pagada, 0), 2)) AS pend, NVL(cd.condonada, 0) AS condonada "
            + "     FROM dev dv JOIN CRD.DTPR d ON d.DTPRCDGO = dv.DTPRCDGO "
            + "     LEFT JOIN pag pg ON pg.DTPRCDGO = dv.DTPRCDGO "
            + "     LEFT JOIN cond cd ON cd.PRSTCDGO = dv.PRSTCDGO ), "
            + " acum AS ( "
            + "   SELECT b.*, SUM(b.pend) OVER (PARTITION BY b.PRSTCDGO ORDER BY b.DTPRFCVN, b.DTPRCDGO) AS acumulado FROM base b ) "
            + " SELECT a.DTPRCDGO, "
            + "        GREATEST(0, ROUND(a.pend - LEAST(a.pend, GREATEST(0, a.condonada - (a.acumulado - a.pend))), 2)) "
            + "   FROM acum a");
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
    public List<Object[]> selectCuotasConProvisionMora(java.time.LocalDate corte) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectCuotasConProvisionMora - corte: " + corte);
        String expresion = "sum(case when m.tipoMovimiento in (1, 4) then m.valor"
            + "                when m.tipoMovimiento in (2, 3, 9, 10) then -m.valor else 0.0 end)";
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
