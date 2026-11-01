package com.saa.ejb.crd.daoImpl;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.service.ModoCorteCartera;
import com.saa.model.crd.DeclaracionPlazoVencido;
import com.saa.rubros.CrdTipoMovimientoAporte;
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
     *
     * <p><b>D27 (2026-10-05, decisión del usuario):</b> incluye DE_PLAZO_VENCIDO (8) — para la
     * contabilidad del cierre, el 8 se trata IGUAL que EN_MORA (11): su capital está en las
     * mismas cuentas de capital y entra a todos los sub-procesos, con la misma clasificación
     * por banda según días vencidos. Antes de este cambio el cierre sólo veía {@code (2, 11)}
     * (H80) — ver {@code DISENO-PASE-A-PLAZO-VENCIDO.md} D27 y la transición del ⑥ (neteo) en
     * {@code CierreCarteraServiceImpl#armaNeteo}, que para la PRIMERA corrida después de este
     * cambio sigue usando el universo VIEJO (el que su ③ anterior realmente abrió).</p>
     */
    private static final String PRESTAMOS_VIVOS =
            " p.PRSTIDST IN (" + EstadoPrestamo.VIGENTE + ", " + EstadoPrestamo.DE_PLAZO_VENCIDO + ", "
            + EstadoPrestamo.EN_MORA + ") ";

    /**
     * Cuotas NO liquidadas. El {@code IS NULL} explícito es necesario: en Oracle un
     * {@code NOT IN} contra nulo descarta la fila en silencio.
     */
    private static final String CUOTAS_PENDIENTES =
            " (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN ("
            + EstadoCuotaPrestamo.PAGADA + ", " + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + ")) ";

    /**
     * D25/D27 (2026-10-05): un préstamo DE_PLAZO_VENCIDO (8) ya tiene condonado el interés de
     * sus cuotas FUTURAS (vencimiento posterior al corte de su declaración PLVN) — medido y
     * confirmado contra el código de {@code DeclaracionPlazoVencidoServiceImpl}: la condonación
     * es solo de CÁLCULO (memorando/liquidación), nunca se persiste en {@code DTPRINTR}, así
     * que sin este filtro el paso ④ (devengo) devengaría a ingreso un interés que el préstamo
     * ya no debe. Solo afecta el INTERÉS — la mora NO está condonada por D25, así que esta
     * expresión se usa en el {@code SUM} del interés, nunca en el {@code WHERE} (excluir la fila
     * entera también perdería su mora, que sí corresponde devengar).
     *
     * <p>{@code MIN(v.PLVNFCCR)}, no una subconsulta escalar sin agregar (corregido 2026-10-05,
     * hallazgo del árbitro): el índice {@code UX_PLVN_PRESTAMO_VIVO} debería dejar a lo sumo una
     * declaración VIVA por préstamo, pero una subconsulta escalar sin {@code MIN} revienta con
     * ORA-01427 si alguna vez hay más de una fila (dato corrupto, condición de carrera, lo que
     * sea) — y un caso raro así no puede tumbar el cierre entero. Si un préstamo en 8 no tuviera
     * ninguna declaración viva (no debería pasar), el {@code MIN} da NULL y la expresión completa
     * da NULL → el {@code CASE} la trata como falsa y el interés se cuenta normal (no se arriesga
     * a condonar algo no verificable).</p>
     */
    private static final String CONDONADO_INTERES_PLAZO_VENCIDO =
            " (p.PRSTIDST = " + EstadoPrestamo.DE_PLAZO_VENCIDO + " AND d.DTPRFCVN > ("
            + "   SELECT MIN(v.PLVNFCCR) FROM CRD.PLVN v"
            + "    WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN ("
            + DeclaracionPlazoVencido.ESTADO_DECLARADA + ", " + DeclaracionPlazoVencido.ESTADO_LIQUIDADA + ")"
            + " )) ";

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

    // ------------------------------------------------------------------------
    // C15 — fragmentos "a fecha de corte" (ver ModoCorteCartera). Con corte == null cada
    // fragmento devuelve EXACTAMENTE el de siempre: el comportamiento normal no cambia.
    // ------------------------------------------------------------------------

    /** Universo de préstamos: vivos HOY, o existentes y aprobados al corte. */
    private static String universo(LocalDate corte) {
        if (corte == null) {
            return PRESTAMOS_VIVOS;
        }
        return " (p.PRSTFCHA < :corteSig AND NVL(p.PRSTIDST,0) NOT IN ("
                + ModoCorteCartera.ESTADOS_FUERA_DE_CARTERA + ")) ";
    }

    /** Cuota no liquidada HOY, o no liquidada al corte (pagada con fecha posterior = impaga). */
    private static String pendiente(LocalDate corte) {
        if (corte == null) {
            return CUOTAS_PENDIENTES;
        }
        return " NOT (NVL(d.DTPRESTD,0) IN (" + EstadoCuotaPrestamo.PAGADA + ", "
                + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + ") AND NVL(d.DTPRFCPG, :corteSig) < :corteSig) ";
    }

    /**
     * Pagos vigentes agregados por cuota; al corte, solo los de fecha {@code <= corte} y con el
     * capital de la regla de la cuadratura C14 ({@code PGPRSLOT} si > 0, si no {@code PGPRCPPG}),
     * <b>salvo los pagos de abono a capital</b> (C14b, {@code PGPRTPOO = ABONO_CAPITAL}): su
     * {@code PGPRSLOT} es una reducción del saldo del préstamo que la tabla ya refleja (re-amortizada,
     * o sin regenerar en C16b, donde {@code Σ DTPRCPTL + Σ DTPRSLOT = monto}), no capital de la
     * cuota donde quedó registrado; de esa cuota solo se descuenta su {@code PGPRCPPG} (0 en el abono).
     * Ver {@link ModoCorteCartera}.
     */
    private static String pagos(LocalDate corte) {
        if (corte == null) {
            return PAGOS_VIGENTES;
        }
        return " (SELECT DTPRCDGO, "
                + "         SUM(" + ModoCorteCartera.CAPITAL_PAGADO_CUOTA_SQL + ") cap, "
                + "         SUM(NVL(PGPRINPG,0)) intr, SUM(NVL(PGPRINVP,0)) intv, "
                + "         SUM(NVL(PGPRMRPG,0)) mora, SUM(NVL(PGPRDSGR,0)) dsgr, "
                + "         SUM(NVL(PGPRVLSI,0)) segi "
                + "  FROM CRD.PGPR WHERE (PGPRANUL IS NULL OR PGPRANUL = 0) AND PGPRFCHA < :corteSig "
                + "  GROUP BY DTPRCDGO) g ";
    }

    /** Mora bruta de la cuota: la persistida HOY, o la fórmula del proceso diario al corte. */
    private static String moraBruta(LocalDate corte) {
        if (corte == null) {
            return " NVL(d.DTPRMRAA,0) ";
        }
        return " (CASE WHEN TRUNC(d.DTPRFCVN) < :corte THEN ROUND(NVL(d.DTPRCPTL,0)"
                + " * (CASE WHEN NVL(p.PRSTINNM,0) > 0 THEN p.PRSTINNM ELSE 9 END) / 36000"
                + " * (:corte - TRUNC(d.DTPRFCVN)), 2) ELSE 0 END) ";
    }

    /** Interés condonado de plazo vencido (D25/D27): al corte, solo si la declaración ya existía. */
    private static String condonado(LocalDate corte) {
        if (corte == null) {
            return CONDONADO_INTERES_PLAZO_VENCIDO;
        }
        return " (d.DTPRFCVN > ("
                + "   SELECT MIN(v.PLVNFCCR) FROM CRD.PLVN v"
                + "    WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN ("
                + DeclaracionPlazoVencido.ESTADO_DECLARADA + ", " + DeclaracionPlazoVencido.ESTADO_LIQUIDADA + ")"
                + "      AND v.PLVNFCCR < :corteSig"
                + " )) ";
    }

    /** Fija los parámetros de corte si la consulta es al corte. */
    private static void parametrosCorte(Query query, LocalDate corte) {
        if (corte == null) {
            return;
        }
        query.setParameter("corteSig", Date.valueOf(corte.plusDays(1)));
        query.setParameter("corte", Date.valueOf(corte));
    }

    /** Igual que {@link #parametrosCorte} para consultas que no usan {@code :corte} (solo {@code :corteSig}). */
    private static void parametroCorteSig(Query query, LocalDate corte) {
        if (corte == null) {
            return;
        }
        query.setParameter("corteSig", Date.valueOf(corte.plusDays(1)));
    }

    @Override
    public List<Object[]> selectCapitalPorProductoYVencimiento(Long idEmpresa) throws Throwable {
        return selectCapitalPorProductoYVencimiento(idEmpresa, null);
    }

    @Override
    public List<Object[]> selectCapitalPorProductoYVencimiento(Long idEmpresa, LocalDate corte) throws Throwable {
        System.out.println("Ingresa al metodo selectCapitalPorProductoYVencimiento"
                + " - empresa: " + idEmpresa + " - corte: " + corte);
        Query query = em.createNativeQuery(
                " SELECT p.PRDCCDGO, TRUNC(d.DTPRFCVN), "
                + "        SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), "
                + "        COUNT(*) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO "
                + " LEFT JOIN " + pagos(corte) + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + universo(corte)
                + " AND    " + pendiente(corte)
                + " AND    d.DTPRFCVN IS NOT NULL "
                + " AND    p.PRDCCDGO IS NOT NULL "
                + " GROUP BY p.PRDCCDGO, TRUNC(d.DTPRFCVN) "
                + " HAVING SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)) > 0 "
                + " ORDER BY p.PRDCCDGO, TRUNC(d.DTPRFCVN)");
        parametroCorteSig(query, corte);
        return normalizaVencimientos(query.getResultList());
    }

    @Override
    public List<Object[]> selectCuotasPendientesPorPrestamo(Long idEmpresa) throws Throwable {
        return selectCuotasPendientesPorPrestamo(idEmpresa, null);
    }

    @Override
    public List<Object[]> selectCuotasPendientesPorPrestamo(Long idEmpresa, LocalDate corte) throws Throwable {
        System.out.println("Ingresa al metodo selectCuotasPendientesPorPrestamo"
                + " - empresa: " + idEmpresa + " - corte: " + corte);
        Query query = em.createNativeQuery(
                " SELECT p.PRSTCDGO, p.PRDCCDGO, d.DTPRCDGO, d.DTPRNMCT, TRUNC(d.DTPRFCVN), "
                + "        GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO "
                + " LEFT JOIN " + pagos(corte) + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + universo(corte)
                + " AND    " + pendiente(corte)
                + " AND    d.DTPRFCVN IS NOT NULL "
                + " AND    p.PRDCCDGO IS NOT NULL "
                + (corte != null ? " AND GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) > 0.004 " : "")
                + " ORDER BY p.PRSTCDGO, TRUNC(d.DTPRFCVN)");
        parametroCorteSig(query, corte);
        return normalizaCuotasPorPrestamo(query.getResultList());
    }

    @Override
    public List<Object[]> selectInteresPorTipoPrestamoEnRango(LocalDate desde, LocalDate hasta) throws Throwable {
        return selectInteresPorTipoPrestamoEnRango(desde, hasta, null);
    }

    @Override
    public List<Object[]> selectInteresPorTipoPrestamoEnRango(LocalDate desde, LocalDate hasta, LocalDate corte)
            throws Throwable {
        System.out.println("Ingresa al metodo selectInteresPorTipoPrestamoEnRango - desde: " + desde
                + " - hasta: " + hasta + " - corte: " + corte);
        Query query = em.createNativeQuery(
                " SELECT pr.TPPRCDGO, "
                + "        SUM(CASE WHEN " + condonado(corte)
                + "                 THEN 0 ELSE GREATEST(NVL(d.DTPRINTR,0) - NVL(g.intr,0), 0) END), "
                + "        SUM(GREATEST(" + moraBruta(corte) + " - NVL(g.mora,0), 0)) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO "
                + " JOIN   CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO "
                + " LEFT JOIN " + pagos(corte) + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + universo(corte)
                + " AND    " + pendiente(corte)
                + " AND    TRUNC(d.DTPRFCVN) BETWEEN :desde AND :hasta "
                + " AND    pr.TPPRCDGO IS NOT NULL "
                + " GROUP BY pr.TPPRCDGO "
                + " ORDER BY pr.TPPRCDGO");
        query.setParameter("desde", Date.valueOf(desde));
        query.setParameter("hasta", Date.valueOf(hasta));
        parametrosCorte(query, corte);
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] fila : filas) {
            resultado.add(new Object[]{ aLong(fila[0]), aDouble(fila[1]), aDouble(fila[2]) });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectCuotasDevengoMoraEnRango(LocalDate desde, LocalDate hasta) throws Throwable {
        return selectCuotasDevengoMoraEnRango(desde, hasta, null);
    }

    @Override
    public List<Object[]> selectCuotasDevengoMoraEnRango(LocalDate desde, LocalDate hasta, LocalDate corte)
            throws Throwable {
        System.out.println("Ingresa al metodo selectCuotasDevengoMoraEnRango - desde: " + desde
                + " - hasta: " + hasta + " - corte: " + corte);
        Query query = em.createNativeQuery(
                " SELECT d.DTPRCDGO, p.PRSTCDGO, pr.TPPRCDGO, "
                + "        GREATEST(" + moraBruta(corte) + " - NVL(g.mora,0), 0) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO "
                + " JOIN   CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO "
                + " LEFT JOIN " + pagos(corte) + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + universo(corte)
                + " AND    " + pendiente(corte)
                + " AND    TRUNC(d.DTPRFCVN) BETWEEN :desde AND :hasta "
                + " AND    pr.TPPRCDGO IS NOT NULL "
                + " AND    GREATEST(" + moraBruta(corte) + " - NVL(g.mora,0), 0) > 0 "
                + " ORDER BY pr.TPPRCDGO, d.DTPRCDGO");
        query.setParameter("desde", Date.valueOf(desde));
        query.setParameter("hasta", Date.valueOf(hasta));
        parametrosCorte(query, corte);
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] fila : filas) {
            resultado.add(new Object[]{ aLong(fila[0]), aLong(fila[1]), aLong(fila[2]), aDouble(fila[3]) });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectCobrablePrestamosHasta(LocalDate hasta) throws Throwable {
        return selectCobrablePrestamosHasta(hasta, null);
    }

    @Override
    public List<Object[]> selectCobrablePrestamosHasta(LocalDate hasta, LocalDate corte) throws Throwable {
        System.out.println("Ingresa al metodo selectCobrablePrestamosHasta - hasta: " + hasta + " - corte: " + corte);
        Query query = em.createNativeQuery(
                " SELECT p.PRDCCDGO, "
                + "        SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), "
                + "        SUM(CASE WHEN " + condonado(corte)
                + "                 THEN 0 ELSE GREATEST(NVL(d.DTPRINTR,0) - NVL(g.intr,0), 0) END), "
                + "        SUM(GREATEST(" + moraBruta(corte) + " - NVL(g.mora,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRDSGR,0) - NVL(g.dsgr,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRVLSI,0) - NVL(g.segi,0), 0)), "
                + "        COUNT(*) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO "
                + " LEFT JOIN " + pagos(corte) + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + universo(corte)
                + " AND    " + pendiente(corte)
                + " AND    TRUNC(d.DTPRFCVN) <= :hasta "
                + " AND    p.PRDCCDGO IS NOT NULL "
                + " GROUP BY p.PRDCCDGO "
                + " ORDER BY p.PRDCCDGO");
        query.setParameter("hasta", Date.valueOf(hasta));
        parametrosCorte(query, corte);
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] fila : filas) {
            resultado.add(new Object[]{ aLong(fila[0]), aDouble(fila[1]), aDouble(fila[2]),
                    aDouble(fila[3]), aDouble(fila[4]), aDouble(fila[5]), aLong(fila[6]) });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectAperturaPorPrestamo(LocalDate hasta, LocalDate corte) throws Throwable {
        System.out.println("Ingresa al metodo selectAperturaPorPrestamo - hasta: " + hasta + " - corte: " + corte);
        // D-032: MISMAS expresiones que selectCobrablePrestamosHasta (la del ③), agrupadas por préstamo.
        Query query = em.createNativeQuery(
                " SELECT p.PRSTCDGO, "
                + "        SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) "
                + "          + CASE WHEN " + condonado(corte)
                + "                 THEN 0 ELSE GREATEST(NVL(d.DTPRINTR,0) - NVL(g.intr,0), 0) END "
                + "          + GREATEST(" + moraBruta(corte) + " - NVL(g.mora,0), 0) "
                + "          + GREATEST(NVL(d.DTPRDSGR,0) - NVL(g.dsgr,0), 0) "
                + "          + GREATEST(NVL(d.DTPRVLSI,0) - NVL(g.segi,0), 0)) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO "
                + " LEFT JOIN " + pagos(corte) + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + universo(corte)
                + " AND    " + pendiente(corte)
                + " AND    TRUNC(d.DTPRFCVN) <= :hasta "
                + " AND    p.PRDCCDGO IS NOT NULL "
                + " GROUP BY p.PRSTCDGO");
        query.setParameter("hasta", Date.valueOf(hasta));
        parametrosCorte(query, corte);
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] f : filas) {
            resultado.add(new Object[]{ aLong(f[0]), aDouble(f[1]) });
        }
        return resultado;
    }

    /** Asientos vigentes de la empresa en el rango y su haber neto en la cuenta (sin líneas de complemento). */
    private static final String HABER_PUENTE_MES =
            " av AS (SELECT a.ASNTCDGO FROM CNT.ASNT a WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) "
            + "        AND a.ASNTFCHA >= :desde AND a.ASNTFCHA < :hastaSig), "
            + " ha AS (SELECT t.ASNTCDGO, SUM(NVL(t.DTASHBRR,0) - NVL(t.DTASDBEE,0)) H FROM CNT.DTAS t "
            + "        JOIN av ON av.ASNTCDGO = t.ASNTCDGO "
            + "        WHERE (t.DTASCNTA = :cuenta OR t.DTASCNTA LIKE :cuentaHijas) "
            + "        AND NVL(t.DTASDSCR, ' ') NOT LIKE :glosaComplemento GROUP BY t.ASNTCDGO), "
            + " cargas AS (SELECT DISTINCT c.CRARCDGO, c.ANCPASNT FROM CRD.ANCP c JOIN av ON av.ASNTCDGO = c.ANCPASNT "
            + "        WHERE c.ANCPTPOO = " + com.saa.rubros.SubProcesoCobroPetro.REPARTO + " AND c.ANCPIDST = 1 "
            + "        AND c.CRARCDGO <> :excluirCarga), "
            + " kp AS (SELECT g.PRSTCDGO, SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) + NVL(g.PGPRMRPG,0) + NVL(g.PGPRDSGR,0) "
            + "        + NVL(g.PGPRVLSI,0) + NVL(g.PGPRINVP,0)) K FROM CRD.PGPR g "
            + "        WHERE NVL(g.PGPRANUL,0) = 0 AND g.CRARCDGO IN (SELECT CRARCDGO FROM cargas) GROUP BY g.PRSTCDGO) ";

    private void parametrosPuente(Query query, Long idEmpresa, String cuenta, LocalDate desde, LocalDate hasta,
            Long excluirCarga) {
        query.setParameter("empresa", idEmpresa);
        query.setParameter("desde", Date.valueOf(desde));
        query.setParameter("hastaSig", Date.valueOf(hasta.plusDays(1)));
        query.setParameter("cuenta", cuenta);
        query.setParameter("cuentaHijas", cuenta + ".%");
        query.setParameter("glosaComplemento",
                com.saa.ejb.crd.service.dto.ComplementoAperturaCobro.GLOSA_PREFIJO + "%");
        query.setParameter("excluirCarga", excluirCarga != null ? excluirCarga : Long.valueOf(-1L));
    }

    @Override
    public List<Object[]> selectCobradoPuentePorPrestamo(Long idEmpresa, String cuenta, LocalDate desde,
            LocalDate hasta, Long excluirCarga) throws Throwable {
        System.out.println("Ingresa al metodo selectCobradoPuentePorPrestamo - empresa: " + idEmpresa + " - cuenta: "
                + cuenta + " - " + desde + " a " + hasta + " - excluirCarga: " + excluirCarga);
        Query query = em.createNativeQuery(
                " WITH " + HABER_PUENTE_MES + ", "
                + " cba AS (SELECT CBCRCDGO, ASNT FROM ( "
                + "          SELECT b.CBCRCDGO, b.CBCRASN1 ASNT FROM CRD.CBCR b UNION "
                + "          SELECT b.CBCRCDGO, b.CBCRASN2 FROM CRD.CBCR b UNION "
                + "          SELECT b.CBCRCDGO, b.CBCRASRP FROM CRD.CBCR b) WHERE ASNT IS NOT NULL), "
                + " cbw AS (SELECT CBCRCDGO, PRSTCDGO, SUM(DCBCVLRR) W FROM CRD.DCBC WHERE PRSTCDGO IS NOT NULL "
                + "        GROUP BY CBCRCDGO, PRSTCDGO), "
                + " cbt AS (SELECT CBCRCDGO, SUM(W) TW, COUNT(*) N FROM cbw GROUP BY CBCRCDGO), "
                + " kc AS (SELECT w.PRSTCDGO, SUM(ha.H * CASE WHEN t.TW <> 0 THEN w.W / t.TW ELSE 1 / t.N END) K "
                + "        FROM cba JOIN ha ON ha.ASNTCDGO = cba.ASNT JOIN cbw w ON w.CBCRCDGO = cba.CBCRCDGO "
                + "        JOIN cbt t ON t.CBCRCDGO = cba.CBCRCDGO GROUP BY w.PRSTCDGO), "
                + " dw AS (SELECT g.PGPRASNT ASNT, g.PRSTCDGO, SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRINPG,0) + NVL(g.PGPRMRPG,0) "
                + "        + NVL(g.PGPRDSGR,0) + NVL(g.PGPRVLSI,0) + NVL(g.PGPRINVP,0)) W FROM CRD.PGPR g "
                + "        WHERE g.PGPRASNT IN (SELECT ASNTCDGO FROM ha) AND g.CRARCDGO IS NULL AND NVL(g.PGPRANUL,0) = 0 "
                + "        AND g.PGPRASNT NOT IN (SELECT ASNT FROM cba) GROUP BY g.PGPRASNT, g.PRSTCDGO), "
                + " dt AS (SELECT ASNT, SUM(W) TW, COUNT(*) N FROM dw GROUP BY ASNT), "
                + " kd AS (SELECT w.PRSTCDGO, SUM(ha.H * CASE WHEN t.TW <> 0 THEN w.W / t.TW ELSE 1 / t.N END) K "
                + "        FROM ha JOIN dw w ON w.ASNT = ha.ASNTCDGO JOIN dt t ON t.ASNT = ha.ASNTCDGO GROUP BY w.PRSTCDGO) "
                + " SELECT PRSTCDGO, SUM(K) FROM (SELECT PRSTCDGO, K FROM kp UNION ALL SELECT PRSTCDGO, K FROM kc "
                + "        UNION ALL SELECT PRSTCDGO, K FROM kd) GROUP BY PRSTCDGO");
        parametrosPuente(query, idEmpresa, cuenta, desde, hasta, excluirCarga);
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] f : filas) {
            resultado.add(new Object[]{ aLong(f[0]), aDouble(f[1]) });
        }
        return resultado;
    }

    @Override
    public double selectNoAtribuiblePetro(Long idEmpresa, String cuenta, LocalDate desde, LocalDate hasta,
            Long excluirCarga) throws Throwable {
        System.out.println("Ingresa al metodo selectNoAtribuiblePetro - empresa: " + idEmpresa + " - cuenta: " + cuenta
                + " - " + desde + " a " + hasta + " - excluirCarga: " + excluirCarga);
        Query query = em.createNativeQuery(
                " WITH " + HABER_PUENTE_MES
                + " SELECT (SELECT NVL(SUM(ha.H),0) FROM cargas c JOIN ha ON ha.ASNTCDGO = c.ANCPASNT) "
                + "      - (SELECT NVL(SUM(K),0) FROM kp) FROM dual");
        parametrosPuente(query, idEmpresa, cuenta, desde, hasta, excluirCarga);
        Double valor = aDouble(query.getSingleResult());
        return valor != null ? valor.doubleValue() : 0D;
    }

    @Override
    public double selectComplementoRegistrado(Long idEmpresa, String cuenta, LocalDate desde, LocalDate hasta)
            throws Throwable {
        System.out.println("Ingresa al metodo selectComplementoRegistrado - empresa: " + idEmpresa + " - cuenta: "
                + cuenta + " - " + desde + " a " + hasta);
        Query query = em.createNativeQuery(
                " SELECT NVL(SUM(NVL(t.DTASDBEE,0) - NVL(t.DTASHBRR,0)), 0) FROM CNT.DTAS t "
                + " JOIN CNT.ASNT a ON a.ASNTCDGO = t.ASNTCDGO "
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) "
                + " AND a.ASNTFCHA >= :desde AND a.ASNTFCHA < :hastaSig "
                + " AND (t.DTASCNTA = :cuenta OR t.DTASCNTA LIKE :cuentaHijas) "
                + " AND t.DTASDSCR LIKE :glosaComplemento");
        query.setParameter("empresa", idEmpresa);
        query.setParameter("desde", Date.valueOf(desde));
        query.setParameter("hastaSig", Date.valueOf(hasta.plusDays(1)));
        query.setParameter("cuenta", cuenta);
        query.setParameter("cuentaHijas", cuenta + ".%");
        query.setParameter("glosaComplemento",
                com.saa.ejb.crd.service.dto.ComplementoAperturaCobro.GLOSA_PREFIJO + "%");
        Double valor = aDouble(query.getSingleResult());
        return valor != null ? valor.doubleValue() : 0D;
    }

    @Override
    public List<Object[]> selectOperacionesAlCorte(LocalDate corte, boolean corteInclusivo) throws Throwable {
        System.out.println("Ingresa al metodo selectOperacionesAlCorte - corte: " + corte
                + " - corteInclusivo: " + corteInclusivo);
        if (corte == null) {
            throw new IllegalArgumentException("selectOperacionesAlCorte: la fecha de corte es obligatoria");
        }
        // Vencida al corte: vto <= corte (inclusiva) o vto < corte (regla del SAA).
        String vencida = corteInclusivo ? " cu.FCVN <= :corte " : " cu.FCVN < :corte ";
        Query query = em.createNativeQuery(
                " WITH cu AS ( "
                + "   SELECT p.PRSTCDGO, p.PRDCCDGO, pr.TPPRCDGO, p.ENTDCDGO, d.DTPRCDGO, TRUNC(d.DTPRFCVN) FCVN, "
                + "          GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) CAPP, "
                + "          CASE WHEN " + condonado(corte) + " THEN 0 ELSE "
                + "            GREATEST(NVL(d.DTPRINTR,0) + NVL(d.DTPRINVN,0) - NVL(g.intr,0) - NVL(g.intv,0), 0) END INTP, "
                + "          GREATEST(" + moraBruta(corte) + " - NVL(g.mora,0), 0) MORAP "
                + "   FROM   CRD.DTPR d "
                + "   JOIN   CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO "
                + "   JOIN   CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO "
                + "   LEFT JOIN " + pagos(corte) + " ON g.DTPRCDGO = d.DTPRCDGO "
                + "   WHERE  " + universo(corte)
                + "   AND    " + pendiente(corte)
                + "   AND    d.DTPRFCVN IS NOT NULL "
                + "   AND    GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) > 0.004 ), "
                + " ci AS ( "
                + "   SELECT a.ENTDCDGO, SUM(a.APRTVLRR) SALDO "
                + "   FROM   CRD.APRT a JOIN CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO "
                + "   JOIN   CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO "
                + "   WHERE  t.TPAPIDST = 1 AND a.APRTFCTR < :corteSig AND NVL(e.ENTDNMID,'0') <> '0' "
                // C15 (2026-10-28): misma lectura de aportes vigentes que el resto (AporteVigenteSql).
                // En esta suma neta no cambia el número (marcado + contra = 0), deja la regla única.
                + "   AND    " + AporteVigenteSql.VIGENTE_SQL
                + "   GROUP BY a.ENTDCDGO ), "
                + " op AS ( "
                + "   SELECT cu.PRSTCDGO, MAX(cu.PRDCCDGO) PRDCCDGO, MAX(cu.TPPRCDGO) TPPRCDGO, MAX(cu.ENTDCDGO) ENTDCDGO, "
                + "          SUM(CASE WHEN " + vencida + " THEN 0 ELSE cu.CAPP END) PV, "
                + "          SUM(CASE WHEN " + vencida + " THEN cu.CAPP ELSE 0 END) VV, "
                + "          MAX(CASE WHEN " + vencida + " THEN :corte - cu.FCVN ELSE 0 END) DIAS, "
                + "          SUM(CASE WHEN " + vencida + " THEN cu.INTP ELSE 0 END) INTV, "
                + "          SUM(CASE WHEN " + vencida + " THEN cu.MORAP ELSE 0 END) MORA, "
                + "          MIN(cu.DTPRCDGO) KEEP (DENSE_RANK FIRST ORDER BY cu.FCVN, cu.DTPRCDGO) ANCLA "
                + "   FROM cu GROUP BY cu.PRSTCDGO ) "
                + " SELECT op.PRSTCDGO, op.PRDCCDGO, op.TPPRCDGO, op.ENTDCDGO, op.PV, op.VV, op.DIAS, op.INTV, op.MORA, "
                + "        op.ANCLA, "
                + "        (SELECT NVL(SUM(NVL(x.DTPRINTR,0)),0) FROM CRD.DTPR x WHERE x.PRSTCDGO = op.PRSTCDGO "
                + "          AND TRUNC(x.DTPRFCVN) BETWEEN TRUNC(:corte, 'MM') AND :corte AND NVL(x.DTPRESTD,0) <> "
                + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + ") INTMES, "
                // D-039 (G48 campo 20, manual de crédito §3.4): "cuota del crédito" = capital del dividendo
                // del MES de reporte, sin vencidos de meses anteriores ni intereses.
                + "        (SELECT NVL(SUM(NVL(x.DTPRCPTL,0)),0) FROM CRD.DTPR x WHERE x.PRSTCDGO = op.PRSTCDGO "
                + "          AND TRUNC(x.DTPRFCVN) BETWEEN TRUNC(:corte, 'MM') AND :corte AND NVL(x.DTPRESTD,0) <> "
                + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + ") CAPMES, "
                + "        CASE WHEN NVL(e.ENTDIDST,0) = " + EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO
                + "             OR ROW_NUMBER() OVER (PARTITION BY op.ENTDCDGO ORDER BY op.PRSTCDGO) > 1 THEN 0 "
                + "             ELSE NVL(ci.SALDO,0) END CI "
                + " FROM op LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = op.ENTDCDGO "
                + " LEFT JOIN ci ON ci.ENTDCDGO = op.ENTDCDGO "
                + " ORDER BY op.PRSTCDGO");
        parametrosCorte(query, corte);
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] f : filas) {
            resultado.add(new Object[]{ aLong(f[0]), aLong(f[1]), aLong(f[2]), aLong(f[3]),
                    aDouble(f[4]), aDouble(f[5]), aLong(f[6]), aDouble(f[7]), aDouble(f[8]),
                    aLong(f[9]), aDouble(f[10]), aDouble(f[12]), aDouble(f[11]) }); // [11] CI, [12] CAPMES
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectMoraPorDevengarAlCorte(LocalDate corte, LocalDate inicioDevengo) throws Throwable {
        System.out.println("Ingresa al metodo selectMoraPorDevengarAlCorte - corte: " + corte
                + " - inicioDevengo: " + inicioDevengo);
        // inicioDevengo (opcional): la mora se cuenta desde max(vencimiento, inicioDevengo). Con la
        // fecha del ancla, la mora anterior al ancla (que Delta no tenía en el balance) no se
        // devenga: cuando se cobre irá directo a ingreso como excedente no devengado.
        String bruta = inicioDevengo == null ? moraBruta(corte)
                : " (CASE WHEN GREATEST(TRUNC(d.DTPRFCVN), :inicioMora) < :corte THEN ROUND(NVL(d.DTPRCPTL,0)"
                + " * (CASE WHEN NVL(p.PRSTINNM,0) > 0 THEN p.PRSTINNM ELSE 9 END) / 36000"
                + " * (:corte - GREATEST(TRUNC(d.DTPRFCVN), :inicioMora)), 2) ELSE 0 END) ";
        // D-017 (H4): mora BRUTA al corte de cada cuota vencida impaga (fórmula del proceso
        // diario, la misma de calcularMoraCuota) menos lo ya reconocido en el libro MVIC
        // (tipo 5 devengo, del cierre o del excedente de un cobro, menos tipo 6), con fecha
        // contable <= corte. Lo que queda es la mora del mes a devengar.
        Query query = em.createNativeQuery(
                " SELECT x.DTPRCDGO, x.PRSTCDGO, x.TPPRCDGO, GREATEST(x.BRUTA - x.RECONOCIDA, 0) "
                + " FROM ( "
                + "   SELECT d.DTPRCDGO, p.PRSTCDGO, pr.TPPRCDGO, " + bruta + " BRUTA, "
                + "          NVL((SELECT SUM(CASE WHEN m.MVICTPMV = 5 THEN m.MVICVLOR "
                + "                               WHEN m.MVICTPMV = 6 THEN -m.MVICVLOR ELSE 0 END) "
                + "               FROM CRD.MVIC m WHERE m.DTPRCDGO = d.DTPRCDGO AND m.MVICANUL = 0 "
                + "                AND m.MVICFCCT < :corteSig), 0) RECONOCIDA "
                + "   FROM   CRD.DTPR d "
                + "   JOIN   CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO "
                + "   JOIN   CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO "
                + "   WHERE  " + universo(corte)
                + "   AND    " + pendiente(corte)
                + "   AND    TRUNC(d.DTPRFCVN) < :corte "
                + "   AND    pr.TPPRCDGO IS NOT NULL ) x "
                + " WHERE x.BRUTA - x.RECONOCIDA > 0.004 "
                + " ORDER BY x.TPPRCDGO, x.DTPRCDGO");
        parametrosCorte(query, corte);
        if (inicioDevengo != null) {
            query.setParameter("inicioMora", Date.valueOf(inicioDevengo));
        }
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] f : filas) {
            resultado.add(new Object[]{ aLong(f[0]), aLong(f[1]), aLong(f[2]), aDouble(f[3]) });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectSaldoContablePorCuenta(Long idEmpresa, String prefijo, String prefijoExcluido,
            LocalDate corte) throws Throwable {
        System.out.println("Ingresa al metodo selectSaldoContablePorCuenta - empresa: " + idEmpresa
                + " - prefijo: " + prefijo + " - excluido: " + prefijoExcluido + " - corte: " + corte);
        // Misma regla que la cuadratura C14 (fuente a): asientos activos (1) o mayorizados (3)
        // con fecha <= corte. Saldo DEUDOR (debe - haber).
        Query query = em.createNativeQuery(
                " SELECT d.DTASCNTA, MAX(d.PLNNCDGO), SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0)) "
                + " FROM   CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO "
                + " WHERE  a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig "
                + " AND    d.DTASCNTA LIKE :prefijo "
                + (prefijoExcluido != null ? " AND d.DTASCNTA NOT LIKE :excluido " : "")
                + " GROUP BY d.DTASCNTA ORDER BY d.DTASCNTA");
        query.setParameter("empresa", idEmpresa);
        query.setParameter("corteSig", Date.valueOf(corte.plusDays(1)));
        query.setParameter("prefijo", prefijo + "%");
        if (prefijoExcluido != null) {
            query.setParameter("excluido", prefijoExcluido + "%");
        }
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] f : filas) {
            resultado.add(new Object[]{ f[0] != null ? f[0].toString() : null, aLong(f[1]), aDouble(f[2]) });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectPrestamosSinAsientoEntrega(LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectPrestamosSinAsientoEntrega - desde: " + desde + " - hasta: " + hasta);
        Query query = em.createNativeQuery(
                " SELECT p.PRSTCDGO, p.PRSTIDAS, TRUNC(p.PRSTFCIN), p.PRSTMNSL, p.PRSTIDST, "
                + "        CASE WHEN p.PRSTPROR IS NOT NULL THEN 1 ELSE 0 END "
                + " FROM   CRD.PRST p "
                + " WHERE  p.PRSTASEN IS NULL "
                + " AND    NVL(p.PRSTIDST,0) NOT IN (" + ModoCorteCartera.ESTADOS_FUERA_DE_CARTERA + ") "
                + " AND    p.PRSTFCIN >= :desde AND p.PRSTFCIN < :hastaSig "
                + " ORDER BY p.PRSTFCIN, p.PRSTCDGO");
        query.setParameter("desde", Date.valueOf(desde));
        query.setParameter("hastaSig", Date.valueOf(hasta.plusDays(1)));
        List<Object[]> filas = query.getResultList();
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] f : filas) {
            resultado.add(new Object[]{ aLong(f[0]), aLong(f[1]), aFecha(f[2]), aDouble(f[3]), aLong(f[4]), aLong(f[5]) });
        }
        return resultado;
    }

    @Override
    public List<Object[]> selectCobrablePrestamosHastaTransicionPlazoVencido(LocalDate hasta,
            LocalDate fechaCorteAnterior) throws Throwable {
        System.out.println("Ingresa al metodo selectCobrablePrestamosHastaTransicionPlazoVencido - hasta: "
                + hasta + " - fechaCorteAnterior: " + fechaCorteAnterior);
        String universoTransicion =
                " (p.PRSTIDST IN (" + EstadoPrestamo.VIGENTE + ", " + EstadoPrestamo.EN_MORA + ")"
                + "  OR (p.PRSTIDST = " + EstadoPrestamo.DE_PLAZO_VENCIDO + " AND EXISTS ("
                + "       SELECT 1 FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO"
                + "        AND v.PLVNESTD IN (" + DeclaracionPlazoVencido.ESTADO_DECLARADA + ", "
                + DeclaracionPlazoVencido.ESTADO_LIQUIDADA + ")"
                + "        AND v.PLVNFCCR > :fechaCorteAnterior))) ";
        Query query = em.createNativeQuery(
                " SELECT p.PRDCCDGO, "
                + "        SUM(GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0)), "
                + "        SUM(CASE WHEN " + CONDONADO_INTERES_PLAZO_VENCIDO
                + "                 THEN 0 ELSE GREATEST(NVL(d.DTPRINTR,0) - NVL(g.intr,0), 0) END), "
                + "        SUM(GREATEST(NVL(d.DTPRMRAA,0) - NVL(g.mora,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRDSGR,0) - NVL(g.dsgr,0), 0)), "
                + "        SUM(GREATEST(NVL(d.DTPRVLSI,0) - NVL(g.segi,0), 0)), "
                + "        COUNT(*) "
                + " FROM   CRD.DTPR d "
                + " JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO "
                + " LEFT JOIN " + PAGOS_VIGENTES + " ON g.DTPRCDGO = d.DTPRCDGO "
                + " WHERE  " + universoTransicion
                + " AND    " + CUOTAS_PENDIENTES
                + " AND    TRUNC(d.DTPRFCVN) <= :hasta "
                + " AND    p.PRDCCDGO IS NOT NULL "
                + " GROUP BY p.PRDCCDGO "
                + " ORDER BY p.PRDCCDGO");
        query.setParameter("hasta", Date.valueOf(hasta));
        query.setParameter("fechaCorteAnterior", Date.valueOf(fechaCorteAnterior));
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
        // Solo cuenta como cobranza de planilla el APORTE_MENSUAL y el EXCEDENTE_PETRO —
        // decisión del usuario, 2026-09-07: los dos vienen del archivo Petro, que es lo que
        // el neteo mide. MIGRADO(6), AJUSTE_MANUAL(2) y REVERSO(5) quedan fuera: ninguno es
        // cobranza del mes, y sin este filtro tapaban un faltante real.
        //
        // Medido en producción, agosto 2026 (por qué importa, no un caso hipotético):
        //   tipo 1 aporte mensual   2772 filas   116.804,80   <- la cobranza real
        //   tipo 6 migrado             2 filas     5.183,30
        //   tipo 2 ajuste manual      12 filas       849,73
        //   tipo 5 reverso             1 fila         96,37
        //   tipo 8 excedente Petro     1 fila         52,26
        //                                          122.986,46  = "registrado" ANTES de este filtro
        // Contra un esperado de 122.299,93 (sin el filtro): la planilla tenía un FALTANTE real
        // de 5.495,13, pero migrado+ajuste+reverso lo tapaban y el sistema veía un exceso de
        // 686,53 — el neteo daba CERO en vez de reversar un no-cobrado que existe.
        //
        // C15 (2026-10-28): solo aportes VIGENTES (AporteVigenteSql). Al filtrar por tipo de
        // movimiento (1, 8) y signo, el aporte anulado por un reproceso (APRTIDST = 0) entraba
        // y su contra-movimiento (tipo 5) quedaba fuera: junio 2025 daba 322.509,73
        // "registrados" (el esquema viejo de la carga 352, 160.706,82, contado dos veces)
        // contra 161.802,91 reales = el DEBE de 2.3.02.05 del asiento de aplicación de la carga.
        Query query = em.createNativeQuery(
                " SELECT NVL(SUM(CASE WHEN a.TPAPCDGO = 9  THEN a.APRTVLRR ELSE 0 END),0), "
                + "        NVL(SUM(CASE WHEN a.TPAPCDGO = 11 THEN a.APRTVLRR ELSE 0 END),0) "
                + " FROM   CRD.APRT a "
                + " WHERE  a.TPAPCDGO IN (9, 11) "
                + " AND    a.APRTVLRR > 0 "
                + " AND    a.APRTTPMV IN (" + CrdTipoMovimientoAporte.APORTE_MENSUAL + ", "
                + CrdTipoMovimientoAporte.EXCEDENTE_PETRO + ") "
                + " AND    " + AporteVigenteSql.VIGENTE_SQL
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
     * Normaliza las filas de {@link #selectCuotasPendientesPorPrestamo}: mismos tipos crudos
     * que {@link #normalizaVencimientos} (fecha como Timestamp/Date según driver, números como
     * BigDecimal), con la fecha en la quinta posición en vez de la segunda.
     *
     * @param filas : Filas crudas de la consulta nativa
     * @return      : Filas {@code [Long, Long, Long, Long, LocalDate, Double]}
     */
    private List<Object[]> normalizaCuotasPorPrestamo(List<Object[]> filas) {
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (Object[] fila : filas) {
            resultado.add(new Object[]{
                aLong(fila[0]), aLong(fila[1]), aLong(fila[2]), aLong(fila[3]),
                aFecha(fila[4]), aDouble(fila[5]) });
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
