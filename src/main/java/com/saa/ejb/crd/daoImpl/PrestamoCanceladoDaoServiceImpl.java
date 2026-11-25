package com.saa.ejb.crd.daoImpl;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.dao.PrestamoCanceladoDaoService;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación de las consultas del reporte de préstamos cancelados. Ver
 * {@link PrestamoCanceladoDaoService} para el orden de las columnas de cada una.
 *
 * <p>Son nativas a propósito: la fecha derivada, la operación final (ROW_NUMBER) y los agregados no se
 * expresan en JPQL. Las consultas por lista de ids se parten en trozos de {@link #TROZO} (límite del IN de
 * Oracle).</p>
 */
@SuppressWarnings("unchecked")
@Stateless
public class PrestamoCanceladoDaoServiceImpl implements PrestamoCanceladoDaoService {

    /** Máximo de elementos de un IN en Oracle. */
    private static final int TROZO = 1000;

    private static final String NOVACION = "NOVACION";
    private static final String REESTRUCTURACION = "REESTRUCTURACION";

    /** Estados 5 y 13, cuya fecha sale del evento de sustitución. */
    private static final String ESTADOS_SUSTITUCION =
        EstadoPrestamo.CANCELADO_POR_NOVACION + ", " + EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION;

    /** Estados 3 y 4, cuya fecha sale de la operación final. */
    private static final String ESTADOS_PAGO =
        EstadoPrestamo.CANCELADO + ", " + EstadoPrestamo.CANCELADO_ANTICIPADO;

    @PersistenceContext
    EntityManager em;

    // =====================================================================
    // Candidatos y conteo (la regla de fecha del §3, igual al bloque 2 del 381)
    // =====================================================================

    /** Préstamos terminales con sus cuatro fechas candidatas (f_sust, f_evento, f_pago, f_cuota). */
    private static String sqlBase(Long idProducto, Long idFilial) {
        return "SELECT p.PRSTCDGO AS prst, p.PRSTIDST AS est,"
            + " (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1"
            + "   AND e.EVPRTPOO IN ('" + NOVACION + "', '" + REESTRUCTURACION + "')) AS f_sust,"
            + " (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1"
            + "   AND e.EVPRTPOO NOT IN ('" + NOVACION + "', '" + REESTRUCTURACION + "')) AS f_evento,"
            + " (SELECT MAX(g.PGPRFCHA) FROM CRD.PGPR g WHERE g.DTPRCDGO IN (SELECT d2.DTPRCDGO FROM CRD.DTPR d2 WHERE d2.PRSTCDGO = p.PRSTCDGO)"
            + "   AND g.EVPRCDGO IS NULL AND NVL(g.PGPRANUL, 0) = 0) AS f_pago,"
            + " (SELECT MAX(d.DTPRFCPG) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO"
            + "   AND d.DTPRESTD IN (4, 7)) AS f_cuota"
            + " FROM CRD.PRST p WHERE p.PRSTIDST IN (:estados)"
            + (idProducto != null ? " AND p.PRDCCDGO = :idProducto" : "")
            + (idFilial != null ? " AND p.FLLLCDGO = :idFilial" : "");
    }

    /** Agrega la fecha y el origen derivados. Un 5/13 sin evento de sustitución cae a la cuota. */
    private static String sqlDerivada(Long idProducto, Long idFilial) {
        return "SELECT b.prst, b.est, b.f_evento, b.f_pago,"
            + " CASE"
            + "  WHEN b.est IN (" + ESTADOS_SUSTITUCION + ") AND b.f_sust IS NOT NULL THEN TRUNC(b.f_sust)"
            + "  WHEN b.est IN (" + ESTADOS_PAGO + ") AND (b.f_evento IS NOT NULL OR b.f_pago IS NOT NULL)"
            + "       THEN TRUNC(GREATEST(NVL(b.f_evento, DATE '1900-01-01'), NVL(b.f_pago, DATE '1900-01-01')))"
            + "  ELSE TRUNC(b.f_cuota) END AS fecha,"
            + " CASE"
            + "  WHEN b.est IN (" + ESTADOS_SUSTITUCION + ") AND b.f_sust IS NOT NULL THEN 'SUSTITUCION'"
            + "  WHEN b.est IN (" + ESTADOS_PAGO + ") AND b.f_evento IS NOT NULL"
            + "       AND (b.f_pago IS NULL OR b.f_evento >= b.f_pago) THEN 'EVENTO'"
            + "  WHEN b.est IN (" + ESTADOS_PAGO + ") AND b.f_pago IS NOT NULL THEN 'PAGO'"
            + "  WHEN b.f_cuota IS NOT NULL THEN 'CUOTA' END AS origen"
            + " FROM (" + sqlBase(idProducto, idFilial) + ") b";
    }

    @Override
    public List<Object[]> candidatos(Date desde, Date hasta, Long idProducto, Long idFilial, List<Long> estados) {
        System.out.println("PrestamoCanceladoDaoService.candidatos - " + desde + " a " + hasta);
        String sql = "SELECT d.prst, d.est, d.fecha, d.origen, d.f_evento, d.f_pago FROM ("
            + sqlDerivada(idProducto, idFilial) + ") d"
            + " WHERE d.fecha BETWEEN :desde AND :hasta ORDER BY d.prst";
        Query q = em.createNativeQuery(sql);
        q.setParameter("estados", estados);
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        if (idProducto != null) {
            q.setParameter("idProducto", idProducto);
        }
        if (idFilial != null) {
            q.setParameter("idFilial", idFilial);
        }
        return q.getResultList();
    }

    @Override
    public long contarSinFecha(Long idProducto, Long idFilial, List<Long> estados) {
        System.out.println("PrestamoCanceladoDaoService.contarSinFecha");
        String sql = "SELECT COUNT(*) FROM (" + sqlDerivada(idProducto, idFilial) + ") d WHERE d.fecha IS NULL";
        Query q = em.createNativeQuery(sql);
        q.setParameter("estados", estados);
        if (idProducto != null) {
            q.setParameter("idProducto", idProducto);
        }
        if (idFilial != null) {
            q.setParameter("idFilial", idFilial);
        }
        Object r = q.getSingleResult();
        return r == null ? 0L : ((Number) r).longValue();
    }

    // =====================================================================
    // Consultas en lote
    // =====================================================================

    /** Corre {@code sql} (con el parámetro {@code :ids}) por trozos de 1000 y junta los resultados. */
    private List<Object[]> porLotes(String sql, List<Long> ids) {
        List<Object[]> todo = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return todo;
        }
        for (int i = 0; i < ids.size(); i += TROZO) {
            List<Long> trozo = ids.subList(i, Math.min(i + TROZO, ids.size()));
            Query q = em.createNativeQuery(sql);
            q.setParameter("ids", trozo);
            todo.addAll(q.getResultList());
        }
        return todo;
    }

    private static int tam(List<Long> ids) {
        return ids == null ? 0 : ids.size();
    }

    private String primerTexto(String sql, Long id) {
        Query q = em.createNativeQuery(sql);
        q.setParameter("id", id);
        List<Object> r = q.getResultList();
        return r.isEmpty() || r.get(0) == null ? null : r.get(0).toString();
    }

    @Override
    public String nombreProducto(Long idProducto) {
        System.out.println("PrestamoCanceladoDaoService.nombreProducto - " + idProducto);
        return primerTexto("SELECT pd.PRDCNMBR FROM CRD.PRDC pd WHERE pd.PRDCCDGO = :id", idProducto);
    }

    @Override
    public String nombreFilial(Long idFilial) {
        System.out.println("PrestamoCanceladoDaoService.nombreFilial - " + idFilial);
        return primerTexto("SELECT f.FLLLNMBR FROM CRD.FLLL f WHERE f.FLLLCDGO = :id", idFilial);
    }

    @Override
    public List<Object[]> datosPrestamos(List<Long> ids) {
        System.out.println("PrestamoCanceladoDaoService.datosPrestamos - " + tam(ids));
        return porLotes("SELECT p.PRSTCDGO, p.PRSTIDAS, en.ENTDNMID, en.ENTDRZNS, f.FLLLNMBR, pd.PRDCNMBR,"
            + " p.PRSTIDST, p.PRSTFCHA, p.PRSTFCIN, p.PRSTFCFN, p.PRSTPLZO, p.PRSTTSAA, p.PRSTVLCT,"
            + " p.PRSTMNSL, p.PRSTMNLD, p.ENTDCDGO"
            + " FROM CRD.PRST p"
            + " LEFT JOIN CRD.ENTD en ON en.ENTDCDGO = p.ENTDCDGO"
            + " LEFT JOIN CRD.FLLL f ON f.FLLLCDGO = p.FLLLCDGO"
            + " LEFT JOIN CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO"
            + " WHERE p.PRSTCDGO IN (:ids)", ids);
    }

    @Override
    public List<Object[]> agregadosPagos(List<Long> ids) {
        System.out.println("PrestamoCanceladoDaoService.agregadosPagos - " + tam(ids));
        return porLotes("SELECT dq.PRSTCDGO, COUNT(*), MIN(g.PGPRFCHA), MAX(g.PGPRFCHA),"
            + " NVL(SUM(g.PGPRCPPG), 0), NVL(SUM(g.PGPRINPG), 0), NVL(SUM(g.PGPRMRPG), 0),"
            + " NVL(SUM(g.PGPRINVP), 0), NVL(SUM(g.PGPRDSGR), 0), NVL(SUM(g.PGPRVLSI), 0),"
            + " NVL(SUM(g.PGPRSLOT), 0), NVL(SUM(g.PGPRVLRR), 0),"
            + " SUM(CASE WHEN g.PGPRTPOO IN ('MIGRACION', 'DEP') THEN 1 ELSE 0 END)"
            + " FROM CRD.PGPR g JOIN CRD.DTPR dq ON dq.DTPRCDGO = g.DTPRCDGO WHERE dq.PRSTCDGO IN (:ids) AND NVL(g.PGPRANUL, 0) = 0"
            + " GROUP BY dq.PRSTCDGO", ids);
    }

    @Override
    public List<Object[]> agregadosCuotas(List<Long> ids) {
        System.out.println("PrestamoCanceladoDaoService.agregadosCuotas - " + tam(ids));
        return porLotes("SELECT d.PRSTCDGO,"
            + " SUM(CASE WHEN d.DTPRESTD = 4 THEN 1 ELSE 0 END),"
            + " SUM(CASE WHEN d.DTPRESTD = 7 THEN 1 ELSE 0 END),"
            + " MAX(CASE WHEN d.DTPRESTD = 4 THEN d.DTPRNMCT END)"
            + " FROM CRD.DTPR d WHERE d.PRSTCDGO IN (:ids) GROUP BY d.PRSTCDGO", ids);
    }

    @Override
    public List<Object[]> aportesPorPago(List<Long> ids) {
        System.out.println("PrestamoCanceladoDaoService.aportesPorPago - " + tam(ids));
        return porLotes("SELECT r.PRSTCDGO, r.PGPRCDGO, r.EVPRCDGO, r.PGPRFCHA, NVL(SUM(ga.PGAPVLRR), 0),"
            + " NVL(SUM(CASE WHEN a.ENTDCDGO <> p.ENTDCDGO THEN ga.PGAPVLRR ELSE 0 END), 0)"
            + " FROM CRD.PGPR r"
            + " JOIN CRD.PRST p ON p.PRSTCDGO = r.PRSTCDGO"
            + " JOIN CRD.PGAP ga ON ga.PGPRCDGO = r.PGPRCDGO"
            + " JOIN CRD.APRT a ON a.APRTCDGO = ga.APRTCDGO"
            + " WHERE r.PRSTCDGO IN (:ids) AND NVL(r.PGPRANUL, 0) = 0 AND NVL(ga.PGAPIDST, 1) <> 0"
            + " GROUP BY r.PRSTCDGO, r.PGPRCDGO, r.EVPRCDGO, r.PGPRFCHA", ids);
    }

    @Override
    public List<Object[]> operacionFinal(List<Long> ids) {
        System.out.println("PrestamoCanceladoDaoService.operacionFinal - " + tam(ids));
        return porLotes("SELECT x.prst, x.k, x.cod, x.fch FROM ("
            + " SELECT o.prst, o.k, o.cod, o.fch,"
            + "        ROW_NUMBER() OVER (PARTITION BY o.prst ORDER BY o.fch DESC, o.cod DESC) AS rn FROM ("
            + "   SELECT e.PRSTCDGO AS prst, 'E' AS k, e.EVPRCDGO AS cod, e.EVPRFCHA AS fch FROM CRD.EVPR e"
            + "    WHERE e.PRSTCDGO IN (:ids) AND e.EVPRESTD = 1"
            + "      AND e.EVPRTPOO NOT IN ('" + NOVACION + "', '" + REESTRUCTURACION + "')"
            + "   UNION ALL"
            + "   SELECT dq.PRSTCDGO, 'P', g.PGPRCDGO, g.PGPRFCHA FROM CRD.PGPR g JOIN CRD.DTPR dq ON dq.DTPRCDGO = g.DTPRCDGO"
            + "    WHERE dq.PRSTCDGO IN (:ids) AND g.EVPRCDGO IS NULL AND NVL(g.PGPRANUL, 0) = 0"
            + " ) o) x WHERE x.rn = 1", ids);
    }

    @Override
    public List<Object[]> eventosSustitucion(List<Long> ids) {
        System.out.println("PrestamoCanceladoDaoService.eventosSustitucion - " + tam(ids));
        return porLotes("SELECT x.prst, x.cod FROM ("
            + " SELECT e.PRSTCDGO AS prst, e.EVPRCDGO AS cod,"
            + "        ROW_NUMBER() OVER (PARTITION BY e.PRSTCDGO ORDER BY e.EVPRFCHA DESC, e.EVPRCDGO DESC) AS rn"
            + "   FROM CRD.EVPR e WHERE e.PRSTCDGO IN (:ids) AND e.EVPRESTD = 1"
            + "    AND e.EVPRTPOO IN ('" + NOVACION + "', '" + REESTRUCTURACION + "')) x WHERE x.rn = 1", ids);
    }

    @Override
    public List<Object[]> detalleEventos(List<Long> idsEvento) {
        System.out.println("PrestamoCanceladoDaoService.detalleEventos - " + tam(idsEvento));
        return porLotes("SELECT e.EVPRCDGO, e.EVPRTPOO, e.EVPRFCHA, e.EVPRVLRR, e.EVPRNMAS, e.EVPROBSR, e.EVPRUSAR"
            + " FROM CRD.EVPR e WHERE e.EVPRCDGO IN (:ids)", idsEvento);
    }

    @Override
    public List<Object[]> detallePagosSinEvento(List<Long> idsPago) {
        System.out.println("PrestamoCanceladoDaoService.detallePagosSinEvento - " + tam(idsPago));
        return porLotes("SELECT g.PGPRCDGO, g.PGPRTPOO, g.PGPRFCHA,"
            + " (SELECT NVL(SUM(h.PGPRVLRR), 0) FROM CRD.PGPR h WHERE h.DTPRCDGO IN (SELECT d3.DTPRCDGO FROM CRD.DTPR d3 WHERE d3.PRSTCDGO = (SELECT d4.PRSTCDGO FROM CRD.DTPR d4 WHERE d4.DTPRCDGO = g.DTPRCDGO))"
            + "   AND h.PGPRFCHA = g.PGPRFCHA AND h.EVPRCDGO IS NULL AND NVL(h.PGPRANUL, 0) = 0),"
            + " g.PGPRUSRG, g.PGPROBSR, g.CRARCDGO, c.FLLLCDGO, g.PGPRASNT"
            + " FROM CRD.PGPR g LEFT JOIN CRD.CRAR c ON c.CRARCDGO = g.CRARCDGO"
            + " WHERE g.PGPRCDGO IN (:ids)", idsPago);
    }

    @Override
    public List<Object[]> cobrosPorEvento(List<Long> idsEvento) {
        System.out.println("PrestamoCanceladoDaoService.cobrosPorEvento - " + tam(idsEvento));
        return porLotes("SELECT d.EVPRCDGO, MAX(c.CBCRCDGO), MAX(c.CBCRTPOO), MAX(c.CBCRASN2)"
            + " FROM CRD.DCBC d JOIN CRD.CBCR c ON c.CBCRCDGO = d.CBCRCDGO"
            + " WHERE d.EVPRCDGO IN (:ids) GROUP BY d.EVPRCDGO", idsEvento);
    }

    @Override
    public List<Object[]> acuerdosPorEvento(List<Long> idsEvento) {
        System.out.println("PrestamoCanceladoDaoService.acuerdosPorEvento - " + tam(idsEvento));
        return porLotes("SELECT a.EVPRCDGO, a.ACCNCDGO, a.ACCNVLPG, a.ACCNVLCN, a.ACCNVLAP, a.ACCNVLDP, a.CBCRCDGO"
            + " FROM CRD.ACCN a WHERE a.EVPRCDGO IN (:ids)", idsEvento);
    }

    @Override
    public List<Object[]> nuevosPorOriginal(List<Long> idsOriginal) {
        System.out.println("PrestamoCanceladoDaoService.nuevosPorOriginal - " + tam(idsOriginal));
        return porLotes("SELECT n.PRSTPROR, n.PRSTCDGO, n.PRSTIDAS, n.PRSTMNSL, n.PRSTVLSS FROM CRD.PRST n"
            + " WHERE n.PRSTPROR IN (:ids) AND n.PRSTIDST <> " + EstadoPrestamo.ANULADO, idsOriginal);
    }

    @Override
    public List<Object[]> prestamosPorCodigo(List<Long> ids) {
        System.out.println("PrestamoCanceladoDaoService.prestamosPorCodigo - " + tam(ids));
        return porLotes("SELECT CAST(NULL AS NUMBER), n.PRSTCDGO, n.PRSTIDAS, n.PRSTMNSL, n.PRSTVLSS FROM CRD.PRST n"
            + " WHERE n.PRSTCDGO IN (:ids)", ids);
    }
}
