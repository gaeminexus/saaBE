package com.saa.ejb.crd.daoImpl;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoContrato;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación de las consultas agregadas del cierre de cartera.
 * Ver {@link CierreCarteraDaoService} para el universo de cuotas y el porqué del saldo.
 *
 * <p>
 * Son consultas NATIVAS y no JPQL a propósito: todas necesitan un {@code LEFT JOIN} contra
 * una subconsulta agregada de {@code CRD.PGPR} y funciones de fecha de Oracle
 * ({@code TRUNC}, {@code LAST_DAY}), que JPQL no expresa. La alternativa —traer las cuotas
 * y sumar en Java— son 70.000 filas por corrida y una consulta de pagos por cuota.
 * </p>
 */
@SuppressWarnings("unchecked")
@Stateless
public class CierreCarteraDaoServiceImpl implements CierreCarteraDaoService {

    @PersistenceContext
    EntityManager em;

    /**
     * Estados de préstamo VIVO. Es {@code PRSTIDST}, no {@code ESPSCDGO}
     * (tabla de trampas de CLAUDE.md).
     */
    private static final String PRESTAMOS_VIVOS =
            " p.PRSTIDST IN (" + EstadoPrestamo.VIGENTE + ", " + EstadoPrestamo.EN_MORA + ") ";

    /**
     * Cuotas NO liquidadas. El {@code IS NULL} explícito es necesario: en Oracle un
     * {@code NOT IN} contra nulo descarta la fila en silencio.
     */
    private static final String CUOTAS_PENDIENTES =
            " (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN ("
            + EstadoCuotaPrestamo.PAGADA + ", " + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + ")) ";

    /**
     * Pagos vigentes agregados por cuota. {@code PGPRANUL} nulo cubre los pagos históricos
     * anteriores al ALTER de CRD.PGPR, igual que
     * {@code PagoPrestamoDaoServiceImpl.selectVigentesByIdDetallePrestamo}.
     */
    private static final String PAGOS_VIGENTES =
            " (SELECT DTPRCDGO, "
            + "         SUM(NVL(PGPRCPPG,0)) cap, SUM(NVL(PGPRINPG,0)) intr, "
            + "         SUM(NVL(PGPRMRPG,0)) mora, SUM(NVL(PGPRDSGR,0)) dsgr, "
            + "         SUM(NVL(PGPRVLSI,0)) segi "
            + "  FROM CRD.PGPR WHERE PGPRANUL IS NULL OR PGPRANUL = 0 "
            + "  GROUP BY DTPRCDGO) g ";

    @Override
    public List<Object[]> selectCapitalPorProductoYVencimiento(Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectCapitalPorProductoYVencimiento"
                + " - empresa: " + idEmpresa);
        Query query = em.createNativeQuery(
                " SELECT p.PRDCCDGO, TRUNC(d.DTPRFCVN), "
                + "        SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), "
                + "        COUNT(*) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO "
                + " LEFT JOIN " + PAGOS_VIGENTES + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + PRESTAMOS_VIVOS
                + " AND    " + CUOTAS_PENDIENTES
                + " AND    d.DTPRFCVN IS NOT NULL "
                + " AND    p.PRDCCDGO IS NOT NULL "
                + " GROUP BY p.PRDCCDGO, TRUNC(d.DTPRFCVN) "
                + " HAVING SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)) > 0 "
                + " ORDER BY p.PRDCCDGO, TRUNC(d.DTPRFCVN)");
        return normalizaVencimientos(query.getResultList());
    }

    @Override
    public List<Object[]> selectInteresPorTipoPrestamoEnRango(LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectInteresPorTipoPrestamoEnRango - desde: " + desde
                + " - hasta: " + hasta);
        Query query = em.createNativeQuery(
                " SELECT pr.TPPRCDGO, "
                + "        SUM(GREATEST(NVL(d.DTPRINTR,0) - NVL(g.intr,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRMRAA,0) - NVL(g.mora,0), 0)) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO "
                + " JOIN   CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO "
                + " LEFT JOIN " + PAGOS_VIGENTES + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + PRESTAMOS_VIVOS
                + " AND    " + CUOTAS_PENDIENTES
                + " AND    TRUNC(d.DTPRFCVN) BETWEEN :desde AND :hasta "
                + " AND    pr.TPPRCDGO IS NOT NULL "
                + " GROUP BY pr.TPPRCDGO "
                + " ORDER BY pr.TPPRCDGO");
        query.setParameter("desde", Date.valueOf(desde));
        query.setParameter("hasta", Date.valueOf(hasta));
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] fila : filas) {
            resultado.add(new Object[]{ aLong(fila[0]), aDouble(fila[1]), aDouble(fila[2]) });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectCobrablePrestamosHasta(LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectCobrablePrestamosHasta - hasta: " + hasta);
        Query query = em.createNativeQuery(
                " SELECT p.PRDCCDGO, "
                + "        SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRINTR,0) - NVL(g.intr,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRMRAA,0) - NVL(g.mora,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRDSGR,0) - NVL(g.dsgr,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRVLSI,0) - NVL(g.segi,0), 0)), "
                + "        COUNT(*) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO "
                + " LEFT JOIN " + PAGOS_VIGENTES + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + PRESTAMOS_VIVOS
                + " AND    " + CUOTAS_PENDIENTES
                + " AND    TRUNC(d.DTPRFCVN) <= :hasta "
                + " AND    p.PRDCCDGO IS NOT NULL "
                + " GROUP BY p.PRDCCDGO "
                + " ORDER BY p.PRDCCDGO");
        query.setParameter("hasta", Date.valueOf(hasta));
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] fila : filas) {
            resultado.add(new Object[]{ aLong(fila[0]), aDouble(fila[1]), aDouble(fila[2]),
                    aDouble(fila[3]), aDouble(fila[4]), aDouble(fila[5]), aLong(fila[6]) });
        }
        return resultado;
    }

    @Override
    public Object[] selectAporteMensualEsperado() throws Throwable {
        System.out.println("Ingresa al metodo selectAporteMensualEsperado");
        // Fase 3 (docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §3.5): la fuente
        // pasa de CRD.HSTR (estado 99) a CRD.VGCN (vigencia ABIERTA del contrato ACTIVO de
        // cada entidad). El ROW_NUMBER por ENTDCDGO evita doble conteo si una entidad
        // quedara con más de un contrato ACTIVO (no debería, pero no hay UNIQUE que lo
        // impida en CNTR).
        Query query = em.createNativeQuery(
                " SELECT NVL(SUM(NVL(vj.VGCNMNTO,0)),0), NVL(SUM(NVL(vc.VGCNMNTO,0)),0), COUNT(*) "
                + " FROM ( SELECT CNTRCDGO, ENTDCDGO, "
                + "               ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY CNTRCDGO DESC) rn "
                + "        FROM CRD.CNTR WHERE CNTRESTD = " + EstadoContrato.ACTIVO + " ) ca "
                + " JOIN CRD.ENTD e ON e.ENTDCDGO = ca.ENTDCDGO "
                + " LEFT JOIN CRD.VGCN vj ON vj.CNTRCDGO = ca.CNTRCDGO AND vj.TPAPCDGO = 9  "
                + "        AND vj.VGCNFCFN IS NULL AND vj.VGCNIDST = " + Estado.ACTIVO
                + " LEFT JOIN CRD.VGCN vc ON vc.CNTRCDGO = ca.CNTRCDGO AND vc.TPAPCDGO = 11 "
                + "        AND vc.VGCNFCFN IS NULL AND vc.VGCNIDST = " + Estado.ACTIVO
                + " WHERE ca.rn = 1 "
                + " AND   e.ENTDIDST IN (" + EstadoParticipeEntidad.ACTIVO + ", "
                + EstadoParticipeEntidad.ACTIVO_EN_MORA + ")");
        Object[] fila = (Object[]) query.getSingleResult();
        return new Object[]{ aDouble(fila[0]), aDouble(fila[1]), aLong(fila[2]) };
    }

    @Override
    public Object[] selectControlEsperadoHstrVsVgcn() throws Throwable {
        System.out.println("Ingresa al metodo selectControlEsperadoHstrVsVgcn");
        // Consulta de control pedida en §3.5: compara el HSTR viejo contra el VGCN nuevo
        // para el mismo universo de entidades (ACTIVO/ACTIVO_EN_MORA), sin tocar la
        // consulta operativa. Es de sólo lectura, para reportar la diferencia.
        Query query = em.createNativeQuery(
                " SELECT NVL(SUM(NVL(h.HSTRMNAJ,0)),0), NVL(SUM(NVL(h.HSTRMNAC,0)),0), "
                + "        NVL(SUM(NVL(vj.VGCNMNTO,0)),0), NVL(SUM(NVL(vc.VGCNMNTO,0)),0) "
                + " FROM CRD.ENTD e "
                + " LEFT JOIN ( SELECT ENTDCDGO, HSTRMNAJ, HSTRMNAC, "
                + "                    ROW_NUMBER() OVER (PARTITION BY ENTDCDGO "
                + "                             ORDER BY HSTRFCIN DESC, HSTRCDGO DESC) rn "
                + "             FROM CRD.HSTR WHERE HSTRESTD = 99 ) h "
                + "        ON h.ENTDCDGO = e.ENTDCDGO AND h.rn = 1 "
                + " LEFT JOIN ( SELECT CNTRCDGO, ENTDCDGO, "
                + "                    ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY CNTRCDGO DESC) rn "
                + "             FROM CRD.CNTR WHERE CNTRESTD = " + EstadoContrato.ACTIVO + " ) ca "
                + "        ON ca.ENTDCDGO = e.ENTDCDGO AND ca.rn = 1 "
                + " LEFT JOIN CRD.VGCN vj ON vj.CNTRCDGO = ca.CNTRCDGO AND vj.TPAPCDGO = 9  "
                + "        AND vj.VGCNFCFN IS NULL AND vj.VGCNIDST = " + Estado.ACTIVO
                + " LEFT JOIN CRD.VGCN vc ON vc.CNTRCDGO = ca.CNTRCDGO AND vc.TPAPCDGO = 11 "
                + "        AND vc.VGCNFCFN IS NULL AND vc.VGCNIDST = " + Estado.ACTIVO
                + " WHERE e.ENTDIDST IN (" + EstadoParticipeEntidad.ACTIVO + ", "
                + EstadoParticipeEntidad.ACTIVO_EN_MORA + ")");
        Object[] fila = (Object[]) query.getSingleResult();
        return new Object[]{ aDouble(fila[0]), aDouble(fila[1]), aDouble(fila[2]), aDouble(fila[3]) };
    }

    @Override
    public Object[] selectAportesRegistrados(LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectAportesRegistrados - desde: " + desde
                + " hasta: " + hasta);
        Query query = em.createNativeQuery(
                " SELECT NVL(SUM(CASE WHEN a.TPAPCDGO = 9  THEN a.APRTVLRR ELSE 0 END),0), "
                + "        NVL(SUM(CASE WHEN a.TPAPCDGO = 11 THEN a.APRTVLRR ELSE 0 END),0) "
                + " FROM   CRD.APRT a "
                + " WHERE  a.TPAPCDGO IN (9, 11) "
                + " AND    a.APRTVLRR > 0 "
                + " AND    TRUNC(a.APRTFCTR) BETWEEN :desde AND :hasta");
        query.setParameter("desde", Date.valueOf(desde));
        query.setParameter("hasta", Date.valueOf(hasta));
        Object[] fila = (Object[]) query.getSingleResult();
        return new Object[]{ aDouble(fila[0]), aDouble(fila[1]) };
    }

    // ------------------------------------------------------------------------
    // Conversión de tipos
    // ------------------------------------------------------------------------

    /**
     * Normaliza las filas de la consulta de capital: el driver devuelve la fecha como
     * {@code java.sql.Timestamp} o {@code java.sql.Date} según la versión, y los números
     * como {@code BigDecimal}.
     *
     * @param filas : Filas crudas de la consulta nativa
     * @return      : Filas {@code [Long, LocalDate, Double, Long]}
     */
    private List<Object[]> normalizaVencimientos(List<Object[]> filas) {
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] fila : filas) {
            resultado.add(new Object[]{
                aLong(fila[0]), aFecha(fila[1]), aDouble(fila[2]), aLong(fila[3]) });
        }
        return resultado;
    }

    /**
     * Convierte a {@code LocalDate} lo que devuelva el driver para una columna DATE.
     *
     * @param valor : Valor crudo
     * @return      : Fecha, o nulo
     */
    private LocalDate aFecha(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) valor).toLocalDateTime().toLocalDate();
        }
        if (valor instanceof java.sql.Date) {
            return ((java.sql.Date) valor).toLocalDate();
        }
        if (valor instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) valor).getTime())
                    .toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(valor.toString().substring(0, 10));
    }

    /**
     * Convierte a {@code Double} un numérico de consulta nativa.
     *
     * @param valor : Valor crudo
     * @return      : Valor, 0 si viene nulo
     */
    private Double aDouble(Object valor) {
        if (valor == null) {
            return Double.valueOf(0D);
        }
        if (valor instanceof BigDecimal) {
            return Double.valueOf(((BigDecimal) valor).doubleValue());
        }
        return Double.valueOf(((Number) valor).doubleValue());
    }

    /**
     * Convierte a {@code Long} un numérico de consulta nativa.
     *
     * @param valor : Valor crudo
     * @return      : Valor, 0 si viene nulo
     */
    private Long aLong(Object valor) {
        if (valor == null) {
            return Long.valueOf(0L);
        }
        if (valor instanceof BigDecimal) {
            return Long.valueOf(((BigDecimal) valor).longValue());
        }
        return Long.valueOf(((Number) valor).longValue());
    }
}
