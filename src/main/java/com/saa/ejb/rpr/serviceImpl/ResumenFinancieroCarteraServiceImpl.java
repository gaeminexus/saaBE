package com.saa.ejb.rpr.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosBandaDaoService;
import com.saa.ejb.rpr.service.EjecucionReporteCarteraService;
import com.saa.ejb.rpr.service.ResumenFinancieroCarteraService;
import com.saa.ejb.rpr.service.dto.LineaResumenFinanciero;
import com.saa.ejb.rpr.service.dto.ResumenFinancieroCartera;
import com.saa.model.rpr.CreditoCuotasPrestamosBanda;
import com.saa.model.rpr.EjecucionReporteCartera;
import com.saa.rubros.EstadoCorridaCierreCartera;
import com.saa.rubros.EstadoPeriodos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * C27-E — implementación de {@link ResumenFinancieroCarteraService}.
 *
 * <p>Reemplaza el armado del Resumen Financiero en el navegador
 * ({@code informes-mensuales-credito.component.ts}), que agrupaba por NOMBRE de producto, ponía
 * todo el vencido de un préstamo en la banda de sus días máximos, sumaba en "interés por cobrar"
 * el interés de la cuota del mes y la mora no contabilizada, tomaba el seguro de la cuota ancla y
 * no redondeaba las líneas. Aquí:</p>
 * <ul>
 * <li>Capital: RPR.CCPB por CUENTA contable de la banda (configuración de bandas del producto, la
 *     misma del cierre de cartera); cada cuenta 1.3 con saldo y sin línea en el informe sale como
 *     "sin línea en el informe".</li>
 * <li>Provisión, interés vencido, desgravamen y seguro de incendio: RPR.CCPB por tipo de préstamo,
 *     con la cuenta de {@link #CUENTAS_POR_TIPO}. La mora calculada al corte va aparte, informativa:
 *     no se contabiliza en ninguna cuenta.</li>
 * <li>Aportes: RPR.CPRM por tipo de aporte; la pensión complementaria en su propia línea (el filtro
 *     viejo buscaba un nombre que no existe y no excluía nada) y el total contra la cuenta 2.1.</li>
 * <li>Cada línea se redondea a 2 decimales antes de sumar subtotales.</li>
 * <li>Aviso de ejecución desactualizada: generada antes del último cierre contable del período o de
 *     la última corrida del cierre de cartera del mes, o período reabierto.</li>
 * </ul>
 */
@Stateless
@SuppressWarnings("unchecked")
public class ResumenFinancieroCarteraServiceImpl implements ResumenFinancieroCarteraService {

    /**
     * Cuentas de los rubros que no salen de la configuración de bandas, por código SB del tipo de
     * préstamo ({@code CRD.TPPR.TPPRCSPB}): {interés vencido, provisión, desgravamen, seguro de incendio}.
     * Plan de cuentas de ASOPREP (B17 de 2025); si cambia el plan, cambia aquí.
     */
    static final Map<String, String[]> CUENTAS_POR_TIPO = new HashMap<>();
    static {
        CUENTAS_POR_TIPO.put("PQ",  new String[]{ "1.4.02.05", "1.3.99.05", "1.4.90.90.10", null });
        CUENTAS_POR_TIPO.put("PH",  new String[]{ "1.4.02.15", "1.3.99.15", "1.4.90.90.10", "1.4.90.15.02" });
        CUENTAS_POR_TIPO.put("PP",  new String[]{ "1.4.02.10", "1.3.99.10", "1.4.90.90.10", "1.4.90.15.03" });
        CUENTAS_POR_TIPO.put("PRE", new String[]{ "1.4.02.10", "1.3.99.10", "1.4.90.90.10", "1.4.90.15.03" });
    }

    /** Cuenta de la pensión complementaria por pagar (dentro de 2.1). */
    static final String CUENTA_PENSION_COMPLEMENTARIA = "2.1.02.25.01";
    /** Raíz de la cartera bruta (sin la provisión 1.3.99) y de los aportes. */
    static final String RAIZ_CARTERA = "1.3";
    static final String RAIZ_PROVISION = "1.3.99";
    static final String RAIZ_APORTES = "2.1";

    private static final String G_PV   = "1. CAPITAL POR VENCER";
    private static final String G_VV   = "2. CAPITAL VENCIDO";
    private static final String G_SIN  = "3. CUENTAS DE CARTERA SIN LINEA EN EL INFORME";
    private static final String G_TOT  = "4. CARTERA BRUTA";
    private static final String G_PROV = "5. PROVISIONES";
    private static final String G_INT  = "6. INTERESES POR COBRAR (INTERES VENCIDO)";
    private static final String G_SEG  = "7. SEGUROS POR COBRAR (CUOTAS VENCIDAS)";
    private static final String G_MORA = "8. MORA CALCULADA AL CORTE (INFORMATIVA)";
    private static final String G_APO  = "9. APORTES (CPRM)";

    @PersistenceContext
    EntityManager em;

    @EJB private EjecucionReporteCarteraService ejccService;
    @EJB private CreditoCuotasPrestamosBandaDaoService ccpbDaoService;

    @Override
    public ResumenFinancieroCartera armar(Long codigoEjecucion, Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo armar ResumenFinancieroCartera - ejecucion: " + codigoEjecucion
                + " - empresa: " + idEmpresa);
        EjecucionReporteCartera ejcc = ejccService.selectById(codigoEjecucion);
        if (ejcc == null) {
            throw new IncomeException("No existe la ejecucion de informes mensuales " + codigoEjecucion);
        }
        LocalDate corte = YearMonth.of(ejcc.getAnio().intValue(), ejcc.getMes().intValue()).atEndOfMonth();

        ResumenFinancieroCartera r = new ResumenFinancieroCartera();
        r.setCodigoEjecucion(codigoEjecucion);
        r.setMes(ejcc.getMes());
        r.setAnio(ejcc.getAnio());
        r.setCorte(corte);
        r.setFechaGeneracion(ejcc.getFechaGeneracion());
        estadoDelMes(r, idEmpresa);

        Map<String, Double> saldos = saldosContables(idEmpresa, corte);
        Map<String, String> nombres = new HashMap<>();
        Map<Long, String> tipoSb = new HashMap<>();
        Map<Long, String> tipoNombre = new HashMap<>();
        for (Object[] f : (List<Object[]>) em.createNativeQuery(
                "SELECT TPPRCDGO, TPPRCSPB, TPPRNMBR FROM CRD.TPPR").getResultList()) {
            Long id = ((Number) f[0]).longValue();
            tipoSb.put(id, f[1] != null ? f[1].toString().trim() : null);
            tipoNombre.put(id, f[2] != null ? f[2].toString().trim() : String.valueOf(id));
        }

        List<Object[]> totales = ccpbDaoService.selectTotalesPorRubro(codigoEjecucion);
        if (totales.isEmpty()) {
            r.setAviso(texto(r.getAviso(), "La ejecucion no tiene detalle por banda (RPR.CCPB): fue generada antes de C27."
                    + " Regenere los informes del mes para ver el resumen comparado con el balance."));
            r.setDesactualizada(true);
            return r;
        }

        // ---- Capital por cuenta de banda ------------------------------------------------
        Map<String, Acum> capitalPV = new TreeMap<>();
        Map<String, Acum> capitalVV = new TreeMap<>();
        // ---- Rubros por cuenta del tipo de préstamo --------------------------------------
        Map<String, Acum> provision = new TreeMap<>();
        Map<String, Acum> interes = new TreeMap<>();
        Map<String, Acum> seguros = new TreeMap<>();
        Map<String, Acum> mora = new TreeMap<>();
        for (Object[] f : totales) {
            String rubro = (String) f[0];
            Long idTipo = f[1] != null ? ((Number) f[1]).longValue() : null;
            String etiqueta = (String) f[3];
            String cuentaBanda = (String) f[4];
            BigDecimal valor = redondear(f[5]);
            long ops = f[6] != null ? ((Number) f[6]).longValue() : 0L;
            String[] cuentasTipo = idTipo != null ? CUENTAS_POR_TIPO.get(tipoSb.get(idTipo)) : null;
            switch (rubro) {
                case CreditoCuotasPrestamosBanda.RUBRO_CAPITAL_POR_VENCER:
                    acumular(capitalPV, cuentaBanda, valor, ops, etiqueta);
                    break;
                case CreditoCuotasPrestamosBanda.RUBRO_CAPITAL_VENCIDO:
                    acumular(capitalVV, cuentaBanda, valor, ops, etiqueta);
                    break;
                case CreditoCuotasPrestamosBanda.RUBRO_PROVISION:
                    acumular(provision, cuentasTipo != null ? cuentasTipo[1] : null, valor, ops, tipoNombre.get(idTipo));
                    break;
                case CreditoCuotasPrestamosBanda.RUBRO_INTERES_VENCIDO:
                    acumular(interes, cuentasTipo != null ? cuentasTipo[0] : null, valor, ops, tipoNombre.get(idTipo));
                    break;
                case CreditoCuotasPrestamosBanda.RUBRO_DESGRAVAMEN:
                    acumular(seguros, cuentasTipo != null ? cuentasTipo[2] : null, valor, ops, "desgravamen");
                    break;
                case CreditoCuotasPrestamosBanda.RUBRO_SEGURO_INCENDIO:
                    acumular(seguros, cuentasTipo != null ? cuentasTipo[3] : null, valor, ops, "incendio");
                    break;
                case CreditoCuotasPrestamosBanda.RUBRO_MORA:
                    acumular(mora, tipoNombre.get(idTipo), valor, ops, null);
                    break;
                default:
                    break;
            }
        }
        Set<String> cuentasUsadas = new LinkedHashSet<>();
        cuentasUsadas.addAll(capitalPV.keySet());
        cuentasUsadas.addAll(capitalVV.keySet());
        cuentasUsadas.addAll(provision.keySet());
        cuentasUsadas.addAll(interes.keySet());
        cuentasUsadas.addAll(seguros.keySet());
        nombres.putAll(nombresCuentas(saldos.keySet(), cuentasUsadas));

        List<LineaResumenFinanciero> lineas = r.getLineas();
        BigDecimal totInforme = BigDecimal.ZERO;
        totInforme = totInforme.add(lineasPorCuenta(lineas, G_PV, capitalPV, saldos, nombres, 1, true,
                "Capital de las cuotas por vencer al corte, cada cuota en la banda de sus dias."));
        totInforme = totInforme.add(lineasPorCuenta(lineas, G_VV, capitalVV, saldos, nombres, 1, true,
                "Capital de las cuotas vencidas al corte, cada cuota en la banda de SUS dias (no los del prestamo)."));

        // Cuentas de cartera con saldo y sin línea en el informe.
        Set<String> cubiertas = new LinkedHashSet<>();
        cubiertas.addAll(capitalPV.keySet());
        cubiertas.addAll(capitalVV.keySet());
        for (Map.Entry<String, Double> s : saldos.entrySet()) {
            String cta = s.getKey();
            if (!cta.startsWith(RAIZ_CARTERA + ".") || cta.startsWith(RAIZ_PROVISION) || cubierta(cta, cubiertas)
                    || Math.abs(s.getValue()) < 0.005) {
                continue;
            }
            LineaResumenFinanciero l = linea(G_SIN, nombres.getOrDefault(cta, cta), cta, BigDecimal.ZERO,
                    redondear(s.getValue()), true, 0L,
                    "La cuenta tiene saldo y ninguna banda de producto la usa: revisar la configuracion o el asiento.");
            lineas.add(l);
        }
        BigDecimal totContable = redondear(saldoRaiz(saldos, RAIZ_CARTERA, RAIZ_PROVISION));
        lineas.add(total(G_TOT, "Total cartera bruta (13 menos 1399)", RAIZ_CARTERA, totInforme, totContable, true));

        BigDecimal totProv = lineasPorCuenta(lineas, G_PROV, provision, saldos, nombres, -1, true,
                "Provision requerida de la G48 al corte, por tipo de prestamo.");
        lineas.add(total(G_PROV, "Total provisiones", RAIZ_PROVISION, totProv,
                redondear(-saldoRaiz(saldos, RAIZ_PROVISION, null)), true));

        lineasPorCuenta(lineas, G_INT, interes, saldos, nombres, 1, true,
                "Interes pendiente de las cuotas vencidas al corte (el de la G48); la mora va aparte.");
        lineasPorCuenta(lineas, G_SEG, seguros, saldos, nombres, 1, false,
                "Pendiente de las cuotas vencidas al corte. Referencia: la cuenta se mueve con el cobro y la"
                + " facturacion del seguro, no con la tabla; la diferencia es informativa.");
        for (Map.Entry<String, Acum> m : mora.entrySet()) {
            lineas.add(linea(G_MORA, "Mora al corte " + (m.getKey() != null ? m.getKey() : "(sin tipo)"), null,
                    m.getValue().valor, null, false, m.getValue().registros,
                    "Mora calculada al corte (formula del proceso diario); no se contabiliza en ninguna cuenta."));
        }

        aportes(lineas, codigoEjecucion, saldos, nombres);
        return r;
    }

    // ------------------------------------------------------------------------------------
    // Armado de líneas
    // ------------------------------------------------------------------------------------

    /** Acumulador de una línea. */
    private static final class Acum {
        BigDecimal valor = BigDecimal.ZERO;
        long registros;
        Set<String> etiquetas = new LinkedHashSet<>();
    }

    private static void acumular(Map<String, Acum> mapa, String clave, BigDecimal valor, long ops, String etiqueta) {
        String k = clave != null ? clave : "";
        Acum a = mapa.computeIfAbsent(k, x -> new Acum());
        a.valor = a.valor.add(valor);
        a.registros += ops;
        if (etiqueta != null) {
            a.etiquetas.add(etiqueta);
        }
    }

    /**
     * Una línea por cuenta; devuelve la suma del informe. {@code signo} = 1 activo (debe - haber),
     * -1 cuentas acreedoras/provisión (haber - debe).
     */
    private BigDecimal lineasPorCuenta(List<LineaResumenFinanciero> lineas, String grupo, Map<String, Acum> mapa,
            Map<String, Double> saldos, Map<String, String> nombres, int signo, boolean comparable, String nota) {
        BigDecimal suma = BigDecimal.ZERO;
        for (Map.Entry<String, Acum> e : mapa.entrySet()) {
            String cta = e.getKey().isEmpty() ? null : e.getKey();
            Acum a = e.getValue();
            String concepto = (cta != null ? nombres.getOrDefault(cta, cta) : "(sin cuenta configurada)")
                    + (a.etiquetas.isEmpty() ? "" : " [" + String.join(", ", a.etiquetas) + "]");
            BigDecimal saldo = cta != null ? redondear(signo * saldoCuenta(saldos, cta)) : null;
            lineas.add(linea(grupo, concepto, cta, a.valor, saldo, comparable && cta != null, a.registros, nota));
            suma = suma.add(a.valor);
        }
        return suma;
    }

    private void aportes(List<LineaResumenFinanciero> lineas, Long codigoEjecucion, Map<String, Double> saldos,
            Map<String, String> nombres) {
        Query q = em.createNativeQuery(
                " SELECT tp.TPAPNMBR, SUM(NVL(c.CPRMTTAL,0)), COUNT(*) FROM RPR.CPRM c"
                + " LEFT JOIN CRD.TPAP tp ON tp.TPAPCDGO = c.TPAPCDGO"
                + " WHERE c.CPRMEJCC = :ejcc GROUP BY tp.TPAPNMBR ORDER BY 1");
        q.setParameter("ejcc", codigoEjecucion);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal pension = null;
        long regPension = 0L;
        for (Object[] f : (List<Object[]>) q.getResultList()) {
            String nombre = f[0] != null ? f[0].toString().trim() : "(sin tipo)";
            BigDecimal valor = redondear(f[1]);
            long reg = f[2] != null ? ((Number) f[2]).longValue() : 0L;
            total = total.add(valor);
            if (nombre.toUpperCase().contains("PENSION COMPLEMENTARIA")) {
                pension = (pension != null ? pension : BigDecimal.ZERO).add(valor);
                regPension += reg;
                continue;
            }
            lineas.add(linea(G_APO, nombre, null, valor, null, false, reg,
                    "Aporte por tipo; el total del grupo se compara con la cuenta 2.1."));
        }
        if (pension != null) {
            lineas.add(linea(G_APO, "Pensiones complementarias por pagar", CUENTA_PENSION_COMPLEMENTARIA, pension,
                    redondear(-saldoCuenta(saldos, CUENTA_PENSION_COMPLEMENTARIA)), true, regPension,
                    "Tipo de aporte PENSION COMPLEMENTARIA, separado del resto."));
        }
        lineas.add(total(G_APO, "Total aportes (CPRM)", RAIZ_APORTES, total,
                redondear(-saldoRaiz(saldos, RAIZ_APORTES, null)), true));
    }

    private static LineaResumenFinanciero linea(String grupo, String concepto, String cuenta, BigDecimal informe,
            BigDecimal saldo, boolean comparable, Long registros, String nota) {
        LineaResumenFinanciero l = new LineaResumenFinanciero();
        l.setGrupo(grupo);
        l.setConcepto(concepto);
        l.setCuenta(cuenta);
        l.setValorInforme(informe != null ? informe.doubleValue() : null);
        l.setSaldoContable(saldo != null ? saldo.doubleValue() : null);
        l.setComparable(comparable && saldo != null);
        l.setDiferencia(saldo != null && informe != null ? informe.subtract(saldo).doubleValue() : null);
        l.setRegistros(registros);
        l.setNota(nota);
        return l;
    }

    private static LineaResumenFinanciero total(String grupo, String concepto, String cuenta, BigDecimal informe,
            BigDecimal saldo, boolean comparable) {
        LineaResumenFinanciero l = linea(grupo, concepto, cuenta, informe, saldo, comparable, null, null);
        l.setTotal(true);
        return l;
    }

    // ------------------------------------------------------------------------------------
    // Saldos contables y estado del mes
    // ------------------------------------------------------------------------------------

    /** Saldo deudor (debe - haber) por cuenta al corte de las raíces 1.3, 1.4 y 2.1. */
    private Map<String, Double> saldosContables(Long idEmpresa, LocalDate corte) {
        Query q = em.createNativeQuery(
                " SELECT d.DTASCNTA, SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0))"
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
                + " WHERE a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig"
                + (idEmpresa != null ? " AND a.PJRQCDGO = :empresa" : "")
                + " AND (d.DTASCNTA LIKE '1.3.%' OR d.DTASCNTA LIKE '1.4.%' OR d.DTASCNTA LIKE '2.1.%')"
                + " GROUP BY d.DTASCNTA");
        q.setParameter("corteSig", java.sql.Date.valueOf(corte.plusDays(1)));
        if (idEmpresa != null) {
            q.setParameter("empresa", idEmpresa);
        }
        Map<String, Double> saldos = new TreeMap<>();
        for (Object[] f : (List<Object[]>) q.getResultList()) {
            if (f[0] != null) {
                saldos.put(f[0].toString().trim(), f[1] != null ? ((Number) f[1]).doubleValue() : 0.0);
            }
        }
        return saldos;
    }

    /** Saldo de la cuenta y sus hijas. */
    private static double saldoCuenta(Map<String, Double> saldos, String cuenta) {
        double s = 0.0;
        for (Map.Entry<String, Double> e : saldos.entrySet()) {
            if (e.getKey().equals(cuenta) || e.getKey().startsWith(cuenta + ".")) {
                s += e.getValue();
            }
        }
        return s;
    }

    /** Saldo de una raíz, sin la sub-raíz excluida. */
    private static double saldoRaiz(Map<String, Double> saldos, String raiz, String excluida) {
        double s = 0.0;
        for (Map.Entry<String, Double> e : saldos.entrySet()) {
            String k = e.getKey();
            if ((k.equals(raiz) || k.startsWith(raiz + ".")) && (excluida == null || !k.startsWith(excluida))) {
                s += e.getValue();
            }
        }
        return s;
    }

    private static boolean cubierta(String cuenta, Set<String> cubiertas) {
        for (String c : cubiertas) {
            if (!c.isEmpty() && (cuenta.equals(c) || cuenta.startsWith(c + "."))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> nombresCuentas(Set<String> conSaldo, Set<String> usadas) {
        Set<String> todas = new LinkedHashSet<>(conSaldo);
        todas.addAll(usadas);
        todas.remove("");
        Map<String, String> nombres = new HashMap<>();
        if (todas.isEmpty()) {
            return nombres;
        }
        Query q = em.createNativeQuery("SELECT PLNNCNTA, MAX(PLNNNMBR) FROM CNT.PLNN WHERE PLNNCNTA IN (:cuentas)"
                + " GROUP BY PLNNCNTA");
        q.setParameter("cuentas", new ArrayList<>(todas));
        for (Object[] f : (List<Object[]>) q.getResultList()) {
            if (f[0] != null && f[1] != null) {
                nombres.put(f[0].toString().trim(), f[0].toString().trim() + " " + f[1].toString().trim());
            }
        }
        return nombres;
    }

    /** Fechas del último cierre contable y de cartera del mes y aviso de desactualización. */
    private void estadoDelMes(ResumenFinancieroCartera r, Long idEmpresa) {
        Query qp = em.createNativeQuery(
                " SELECT MAX(PRDOFCCN), MAX(CASE WHEN NVL(PRDOCNCR,0) = 1 OR PRDOESTD = " + EstadoPeriodos.CERRADO
                + " THEN 1 ELSE 0 END) FROM CNT.PRDO WHERE PRDOANNN = :anio AND PRDOMSSS = :mes"
                + (idEmpresa != null ? " AND PJRQCDGO = :empresa" : ""));
        qp.setParameter("anio", r.getAnio());
        qp.setParameter("mes", r.getMes());
        if (idEmpresa != null) {
            qp.setParameter("empresa", idEmpresa);
        }
        Object[] p = (Object[]) qp.getSingleResult();
        r.setFechaCierreContable(aFechaHora(p[0]));
        r.setPeriodoCerrado(p[1] != null && ((Number) p[1]).intValue() == 1);

        Query qc = em.createNativeQuery(
                " SELECT MAX(GREATEST(NVL(CRCTFCMD, CRCTFCRG), CRCTFCRG)) FROM CRD.CRCT"
                + " WHERE CRCTANOO = :anio AND CRCTMESS = :mes AND CRCTESTD = 1 AND CRCTIDST = "
                + EstadoCorridaCierreCartera.EJECUTADA);
        qc.setParameter("anio", r.getAnio());
        qc.setParameter("mes", r.getMes());
        r.setFechaCierreCartera(aFechaHora(qc.getSingleResult()));

        StringBuilder aviso = new StringBuilder();
        LocalDateTime generada = r.getFechaGeneracion();
        if (!r.isPeriodoCerrado()) {
            aviso.append("El periodo contable del mes esta ABIERTO (reabierto o sin cerrar): las cifras pueden cambiar. ");
        }
        if (generada == null) {
            aviso.append("La ejecucion no tiene fecha de generacion. ");
        } else {
            if (r.getFechaCierreContable() != null && generada.isBefore(r.getFechaCierreContable())) {
                aviso.append("La ejecucion se genero el ").append(generada)
                        .append(", ANTES del ultimo cierre contable del mes (").append(r.getFechaCierreContable())
                        .append("). ");
            }
            if (r.getFechaCierreCartera() != null && generada.isBefore(r.getFechaCierreCartera())) {
                aviso.append("La ejecucion se genero ANTES de la ultima corrida del cierre de cartera del mes (")
                        .append(r.getFechaCierreCartera()).append("). ");
            }
        }
        if (aviso.length() > 0) {
            aviso.append("Regenere los informes del mes.");
            r.setDesactualizada(true);
            r.setAviso(aviso.toString());
        } else {
            r.setAviso("");
        }
    }

    // ------------------------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------------------------

    private static BigDecimal redondear(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return new BigDecimal(String.valueOf(((Number) valor).doubleValue())).setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDateTime aFechaHora(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Timestamp) {
            return ((Timestamp) o).toLocalDateTime();
        }
        if (o instanceof java.sql.Date) {
            return ((java.sql.Date) o).toLocalDate().atStartOfDay();
        }
        if (o instanceof LocalDateTime) {
            return (LocalDateTime) o;
        }
        if (o instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) o).getTime()).toLocalDateTime();
        }
        return null;
    }

    private static String texto(String actual, String nuevo) {
        return (actual == null || actual.isEmpty()) ? nuevo : actual + " " + nuevo;
    }
}
