package com.saa.ejb.cnt.serviceImpl;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.ControlCierreService;
import com.saa.ejb.cnt.service.EstadoCierreService;
import com.saa.ejb.cnt.service.dto.ControlCierre;
import com.saa.ejb.cnt.service.dto.EstadoPeriodo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación de {@link ControlCierreService}. Consultas nativas de solo lectura con la misma regla de
 * saldo que la cuadratura C14 y el cierre de cartera: asientos activos (1) o reversados (3) con fecha
 * {@code <= fin de mes}; saldo DEUDOR (debe − haber). docs/logica-negocio/cnt/CONTROL-CIERRE-MES.md.
 */
@Stateless
public class ControlCierreServiceImpl implements ControlCierreService {

    /** Tolerancia de redondeo (medio centavo), la misma de los scripts del expediente. */
    private static final double TOLERANCIA = 0.005D;

    private static final Locale ES_EC = Locale.forLanguageTag("es-EC");

    @PersistenceContext
    private EntityManager em;

    @EJB
    private EstadoCierreService estadoCierreService;

    @Override
    public List<ControlCierre> controlar(Long idEmpresa, Long anio, Long mes) throws Throwable {
        if (idEmpresa == null || anio == null || mes == null || mes < 1 || mes > 12) {
            throw new IncomeException("Empresa, año y mes (1 a 12) son obligatorios para los controles de cierre.");
        }
        LocalDate inicio = LocalDate.of(anio.intValue(), mes.intValue(), 1);
        LocalDate corte = inicio.withDayOfMonth(inicio.lengthOfMonth());
        LocalDate corteSig = corte.plusDays(1);

        List<ControlCierre> controles = new ArrayList<ControlCierre>();
        controlesPuentes(controles, idEmpresa, corte, corteSig);
        controlEcuacion(controles, idEmpresa, corte, corteSig);
        EstadoPeriodo estado = estadoCierreService.estadoPeriodo(idEmpresa, anio, mes);
        controlesCartera(controles, idEmpresa, anio, mes, corte, corteSig, estado);
        controlesBancos(controles, idEmpresa, anio, mes, corte, corteSig);
        controlesAsientos(controles, idEmpresa, inicio, corteSig);
        controlesB17G(controles, idEmpresa, anio, mes, corte, corteSig);
        return controles;
    }

    // =====================================================================
    // (a) Cuentas puente (D-024)
    // =====================================================================

    private void controlesPuentes(List<ControlCierre> controles, Long idEmpresa, LocalDate corte, LocalDate corteSig) {
        for (String cuenta : new String[] { "1.4.05.05", "2.3.02.05" }) {
            double saldo = saldoDeudor(idEmpresa, corteSig, cuenta, null);
            ControlCierre c = new ControlCierre("PUENTES", "PUENTE_" + cuenta,
                    "Cuenta puente " + cuenta + " en cero al " + corte + " (D-024)");
            compara(c, saldo, 0D, "Saldo deudor de " + cuenta + " y subcuentas: " + fmt(saldo)
                    + ". D-024: la cuenta puente de excedentes/compensaciones debe quedar en 0 cada mes.");
            controles.add(c);
        }
        double s14 = saldoDeudor(idEmpresa, corteSig, "1.4.05.10", null);
        double s23 = saldoDeudor(idEmpresa, corteSig, "2.3.02.10", null);
        ControlCierre c = new ControlCierre("PUENTES", "PUENTE_1.4.05.10_2.3.02.10",
                "1.4.05.10 igual y opuesta a 2.3.02.10 al " + corte + " (D-024)");
        // valor = 1.4.05.10 (deudor), esperado = −2.3.02.10 (deudor), es decir el saldo acreedor de la 2.3.02.10.
        compara(c, s14, -s23, "Saldo deudor 1.4.05.10: " + fmt(s14) + "; saldo acreedor 2.3.02.10: " + fmt(-s23)
                + ". Deben ser iguales (excedentes D-010 por 2.3.02.10).");
        controles.add(c);
    }

    // =====================================================================
    // (b) Ecuación contable
    // =====================================================================

    private void controlEcuacion(List<ControlCierre> controles, Long idEmpresa, LocalDate corte, LocalDate corteSig) {
        Query q = em.createNativeQuery(
                " SELECT SUBSTR(d.DTASCNTA, 1, 1), SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) "
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO "
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig "
                + " GROUP BY SUBSTR(d.DTASCNTA, 1, 1)");
        q.setParameter("empresa", idEmpresa);
        q.setParameter("corteSig", Date.valueOf(corteSig));
        Map<String, Double> porNaturaleza = new TreeMap<String, Double>();
        for (Object fila : q.getResultList()) {
            Object[] f = (Object[]) fila;
            porNaturaleza.put(String.valueOf(f[0]), aDouble(f[1]));
        }
        double activo = valor(porNaturaleza, "1");
        double pasivo = -valor(porNaturaleza, "2");
        double patrimonio = -valor(porNaturaleza, "3");
        double resultado = -valor(porNaturaleza, "5") - valor(porNaturaleza, "4");
        ControlCierre c = new ControlCierre("ECUACION", "ECUACION_CONTABLE",
                "Ecuación contable al " + corte + ": activo = pasivo + patrimonio + resultado");
        compara(c, activo, pasivo + patrimonio + resultado,
                "Activo (1) " + fmt(activo) + "; pasivo (2) " + fmt(pasivo) + "; patrimonio (3) " + fmt(patrimonio)
                        + "; resultado (5 − 4) " + fmt(resultado) + ". Mismo cálculo que la línea \"1 − 2 − 3 − (5 − 4)\" de"
                        + " la cuadratura B17 ↔ G.");
        controles.add(c);
    }

    // =====================================================================
    // (c) Cartera del módulo al corte vs contabilidad, por grupo (⑨)
    // =====================================================================

    private void controlesCartera(List<ControlCierre> controles, Long idEmpresa, Long anio, Long mes, LocalDate corte,
            LocalDate corteSig, EstadoPeriodo estado) {
        Map<String, Double> contable = capitalContablePorGrupo(idEmpresa, corteSig);
        Long idCorrida = estado != null ? estado.getIdCorridaCartera() : null;
        if (idCorrida == null) {
            ControlCierre c = new ControlCierre("CARTERA", "CARTERA_NO_CERRADA",
                    "Cartera del módulo vs contabilidad por grupo al " + corte);
            c.setEstado(ControlCierre.ADVERTENCIA);
            StringBuilder sb = new StringBuilder("Cartera no cerrada: el mes " + anio + "-" + dos(mes)
                    + " no tiene corrida de cierre de cartera EJECUTADA vigente (CRD.CRCT), así que no hay"
                    + " distribución del módulo al corte con la cual comparar. Cierre la cartera antes del período"
                    + " contable (C17). Capital contable 1.3 (sin 1.3.99) por grupo:");
            for (Map.Entry<String, Double> e : contable.entrySet()) {
                sb.append(' ').append(e.getKey()).append(' ').append(fmt(e.getValue())).append(';');
            }
            c.setDetalle(sb.toString());
            controles.add(c);
            return;
        }
        // Distribución del módulo que la corrida vigente dejó al corte (CRD.BDCC = snapshot de distNueva).
        Query q = em.createNativeQuery(
                " SELECT p.PLNNCNTA, SUM(NVL(b.BDCCCPTL, 0)) FROM CRD.BDCC b "
                + " LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = b.PLNNCDGO "
                + " WHERE b.CRCTCDGO = :corrida GROUP BY p.PLNNCNTA");
        q.setParameter("corrida", idCorrida);
        Map<String, Double> modulo = new TreeMap<String, Double>();
        double sinCuenta = 0D;
        for (Object fila : q.getResultList()) {
            Object[] f = (Object[]) fila;
            String grupo = grupoBanda(f[0] != null ? f[0].toString() : null);
            if (grupo == null) {
                sinCuenta += aDouble(f[1]);
                continue;
            }
            modulo.merge(grupo, aDouble(f[1]), Double::sum);
        }
        java.util.Set<String> grupos = new java.util.TreeSet<String>();
        grupos.addAll(modulo.keySet());
        grupos.addAll(contable.keySet());
        for (String grupo : grupos) {
            double cnt = valor(contable, grupo);
            double mod = valor(modulo, grupo);
            ControlCierre c = new ControlCierre("CARTERA", "CARTERA_GRUPO_" + grupo,
                    "Cartera grupo " + grupo + " (" + nombreGrupo(grupo) + "): contabilidad vs módulo al " + corte);
            compara(c, cnt, mod, "Contabilidad 1.3 (sin 1.3.99) " + fmt(cnt) + "; módulo (corrida de cierre " + idCorrida
                    + ", CRD.BDCC) " + fmt(mod) + ". La diferencia es la que el ⑨ deja en la banda .25 del grupo hasta"
                    + " explicarla o decidirla (D-011).");
            controles.add(c);
        }
        if (Math.abs(sinCuenta) >= TOLERANCIA) {
            ControlCierre c = new ControlCierre("CARTERA", "CARTERA_SIN_CUENTA",
                    "Capital del módulo en bandas sin cuenta contable");
            c.setValor(redondea(sinCuenta));
            c.setEstado(ControlCierre.ADVERTENCIA);
            c.setDetalle("La corrida " + idCorrida + " tiene " + fmt(sinCuenta)
                    + " de capital en bandas sin cuenta 1.3 (CRD.BDCC.PLNNCDGO nulo o fuera de 1.3): parametrice las bandas.");
            controles.add(c);
        }
    }

    /** Capital contable de cartera (1.3 sin 1.3.99) por grupo Q/P/H, la misma agrupación del ⑨ mensual. */
    private Map<String, Double> capitalContablePorGrupo(Long idEmpresa, LocalDate corteSig) {
        Query q = em.createNativeQuery(
                " SELECT d.DTASCNTA, SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) "
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO "
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig "
                + " AND d.DTASCNTA LIKE '1.3.%' AND d.DTASCNTA NOT LIKE '1.3.99%' GROUP BY d.DTASCNTA");
        q.setParameter("empresa", idEmpresa);
        q.setParameter("corteSig", Date.valueOf(corteSig));
        Map<String, Double> grupos = new TreeMap<String, Double>();
        for (Object fila : q.getResultList()) {
            Object[] f = (Object[]) fila;
            String grupo = grupoBanda(f[0] != null ? f[0].toString() : null);
            if (grupo != null) {
                grupos.merge(grupo, aDouble(f[1]), Double::sum);
            }
        }
        return grupos;
    }

    /**
     * Grupo de la cuenta de banda, igual que {@code CierreCarteraServiceImpl.grupoBanda} en el cierre mensual:
     * 1.3.01–04 = Q (quirografarios), 1.3.05–08 = P (prendarios), 1.3.09–12 = H (hipotecarios); otra madre
     * queda como su propio grupo; 1.3.99 (provisión) no entra.
     */
    private static String grupoBanda(String cuenta) {
        if (cuenta == null || !cuenta.startsWith("1.3.") || cuenta.startsWith("1.3.99") || cuenta.length() < 6) {
            return null;
        }
        String madre = cuenta.substring(0, 6);
        int nn;
        try {
            nn = Integer.parseInt(cuenta.substring(4, 6));
        } catch (NumberFormatException e) {
            return madre;
        }
        if (nn >= 1 && nn <= 4) {
            return "Q";
        }
        if (nn >= 5 && nn <= 8) {
            return "P";
        }
        if (nn >= 9 && nn <= 12) {
            return "H";
        }
        return madre;
    }

    private static String nombreGrupo(String grupo) {
        if ("Q".equals(grupo)) {
            return "1.3.01–1.3.04";
        }
        if ("P".equals(grupo)) {
            return "1.3.05–1.3.08";
        }
        if ("H".equals(grupo)) {
            return "1.3.09–1.3.12";
        }
        return grupo;
    }

    // =====================================================================
    // (d) Bancos: saldo contable vs último saldo del extracto importado del mes
    // =====================================================================

    private void controlesBancos(List<ControlCierre> controles, Long idEmpresa, Long anio, Long mes, LocalDate corte,
            LocalDate corteSig) {
        // Cuentas bancarias reales (C12: TSR.BNCO.BNCOREAL = 1) activas, con su cuenta contable. El extracto
        // vigente (EXBCESTD = 1) del período del mes con la fecha "hasta" más reciente; a igualdad, el último cargado.
        Query q = em.createNativeQuery(
                " SELECT c.CNBCCDGO, p.PLNNCNTA, "
                + "  (SELECT SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) FROM CNT.DTAS d "
                + "     JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO "
                + "    WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig "
                + "      AND d.DTASCNTA = p.PLNNCNTA), "
                + "  (SELECT MAX(e.EXBCSLFN) KEEP (DENSE_RANK LAST ORDER BY e.EXBCFHST NULLS FIRST, e.EXBCCDGO) "
                + "     FROM TSR.EXBC e JOIN CNT.PRDO r ON r.PRDOCDGO = e.PRDOCDGO "
                + "    WHERE e.CNBCCDGO = c.CNBCCDGO AND e.EXBCESTD = 1 AND r.PRDOANNN = :anio AND r.PRDOMSSS = :mes), "
                + "  (SELECT MAX(e.EXBCCDGO) KEEP (DENSE_RANK LAST ORDER BY e.EXBCFHST NULLS FIRST, e.EXBCCDGO) "
                + "     FROM TSR.EXBC e JOIN CNT.PRDO r ON r.PRDOCDGO = e.PRDOCDGO "
                + "    WHERE e.CNBCCDGO = c.CNBCCDGO AND e.EXBCESTD = 1 AND r.PRDOANNN = :anio AND r.PRDOMSSS = :mes) "
                + " FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO "
                + " JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO "
                + " WHERE b.PJRQCDGO = :empresa AND NVL(b.BNCOREAL, 1) = 1 AND c.CNBCESTD = 1 "
                + " ORDER BY p.PLNNCNTA, c.CNBCCDGO");
        q.setParameter("empresa", idEmpresa);
        q.setParameter("corteSig", Date.valueOf(corteSig));
        q.setParameter("anio", anio);
        q.setParameter("mes", mes);
        List<?> filas = q.getResultList();
        if (filas.isEmpty()) {
            ControlCierre c = new ControlCierre("BANCOS", "BANCOS_SIN_CUENTAS", "Bancos contra extracto al " + corte);
            c.setEstado(ControlCierre.ADVERTENCIA);
            c.setDetalle("No hay cuentas bancarias reales activas con cuenta contable (TSR.CNBC / BNCOREAL = 1).");
            controles.add(c);
            return;
        }
        for (Object fila : filas) {
            Object[] f = (Object[]) fila;
            Long idCuenta = aLong(f[0]);
            String cuenta = f[1] != null ? f[1].toString() : "?";
            double saldo = aDouble(f[2]);
            ControlCierre c = new ControlCierre("BANCOS", "BANCO_" + cuenta,
                    "Banco " + cuenta + " (cuenta bancaria " + idCuenta + "): saldo contable vs extracto al " + corte);
            if (f[3] == null) {
                c.setValor(redondea(saldo));
                c.setEstado(ControlCierre.ADVERTENCIA);
                c.setDetalle("Sin extracto importado para " + anio + "-" + dos(mes) + " (TSR.EXBC vigente del período)."
                        + " Saldo contable " + fmt(saldo) + ". Importe el extracto en Tesorería > Extractos bancarios.");
            } else {
                double extracto = aDouble(f[3]);
                compara(c, saldo, extracto, "Saldo contable " + fmt(saldo) + "; saldo final del extracto " + aLong(f[4])
                        + ": " + fmt(extracto) + ". Una diferencia son partidas en tránsito o sin registrar:"
                        + " revisar la conciliación del mes.");
            }
            controles.add(c);
        }
    }

    // =====================================================================
    // (e) Asientos del mes descuadrados, incompletos (borrador) o sin líneas
    // =====================================================================

    private void controlesAsientos(List<ControlCierre> controles, Long idEmpresa, LocalDate inicio, LocalDate corteSig) {
        Query q = em.createNativeQuery(
                " SELECT a.ASNTCDGO, a.ASNTNMRO, SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) "
                + " FROM CNT.ASNT a JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO "
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) "
                + "   AND a.ASNTFCHA >= :inicio AND a.ASNTFCHA < :corteSig "
                + " GROUP BY a.ASNTCDGO, a.ASNTNMRO "
                + " HAVING ABS(SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0))) >= 0.005 ORDER BY a.ASNTCDGO");
        q.setParameter("empresa", idEmpresa);
        q.setParameter("inicio", Date.valueOf(inicio));
        q.setParameter("corteSig", Date.valueOf(corteSig));
        List<?> descuadrados = q.getResultList();
        double total = 0D;
        StringBuilder ids = new StringBuilder();
        int n = 0;
        for (Object fila : descuadrados) {
            Object[] f = (Object[]) fila;
            total += Math.abs(aDouble(f[2]));
            if (n++ < 20) {
                ids.append(ids.length() > 0 ? ", " : "").append(aLong(f[0])).append(" (").append(fmt(aDouble(f[2]))).append(')');
            }
        }
        ControlCierre c = new ControlCierre("ASIENTOS", "ASIENTOS_DESCUADRADOS",
                "Asientos vigentes del mes con debe ≠ haber");
        c.setValor(Double.valueOf(descuadrados.size()));
        c.setEsperado(Double.valueOf(0D));
        c.setDiferencia(Double.valueOf(descuadrados.size()));
        c.setEstado(descuadrados.isEmpty() ? ControlCierre.OK : ControlCierre.ERROR);
        c.setDetalle(descuadrados.isEmpty() ? "Todos los asientos activos o reversados del mes cuadran."
                : descuadrados.size() + " asiento(s) descuadrado(s) por " + fmt(total) + " en total. Códigos: " + ids
                        + (descuadrados.size() > 20 ? ", …" : "") + ".");
        controles.add(c);

        long incompletos = contar(" SELECT COUNT(*) FROM CNT.ASNT a WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD = 4 "
                + " AND a.ASNTFCHA >= :inicio AND a.ASNTFCHA < :corteSig", idEmpresa, inicio, corteSig);
        ControlCierre b = new ControlCierre("ASIENTOS", "ASIENTOS_BORRADOR", "Asientos del mes en borrador (incompletos)");
        b.setValor(Double.valueOf(incompletos));
        b.setEsperado(Double.valueOf(0D));
        b.setDiferencia(Double.valueOf(incompletos));
        b.setEstado(incompletos == 0 ? ControlCierre.OK : ControlCierre.ADVERTENCIA);
        b.setDetalle(incompletos == 0 ? "No hay asientos incompletos (estado 4) en el mes."
                : incompletos + " asiento(s) en estado INCOMPLETO (4) con fecha del mes: no suman en los saldos;"
                        + " completarlos o anularlos antes de cerrar.");
        controles.add(b);

        long vacios = contar(" SELECT COUNT(*) FROM CNT.ASNT a WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) "
                + " AND a.ASNTFCHA >= :inicio AND a.ASNTFCHA < :corteSig "
                + " AND NOT EXISTS (SELECT 1 FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ASNTCDGO)", idEmpresa, inicio, corteSig);
        ControlCierre v = new ControlCierre("ASIENTOS", "ASIENTOS_SIN_LINEAS", "Asientos vigentes del mes sin líneas");
        v.setValor(Double.valueOf(vacios));
        v.setEsperado(Double.valueOf(0D));
        v.setDiferencia(Double.valueOf(vacios));
        v.setEstado(vacios == 0 ? ControlCierre.OK : ControlCierre.ADVERTENCIA);
        v.setDetalle(vacios == 0 ? "Todos los asientos vigentes del mes tienen líneas."
                : vacios + " asiento(s) activo(s) sin ninguna línea: no afectan saldos, pero ocupan número;"
                        + " revisar si deben anularse.");
        controles.add(v);
    }

    private long contar(String sql, Long idEmpresa, LocalDate inicio, LocalDate corteSig) {
        Query q = em.createNativeQuery(sql);
        q.setParameter("empresa", idEmpresa);
        q.setParameter("inicio", Date.valueOf(inicio));
        q.setParameter("corteSig", Date.valueOf(corteSig));
        Object r = q.getSingleResult();
        return r instanceof Number ? ((Number) r).longValue() : 0L;
    }

    // =====================================================================
    // (f) Cuadratura B17 ↔ G (lógica de 10-cuadratura-b17-g.js)
    // =====================================================================

    private void controlesB17G(List<ControlCierre> controles, Long idEmpresa, Long anio, Long mes, LocalDate corte,
            LocalDate corteSig) {
        // El plan de cuentas del SAA sigue el catálogo de la Superintendencia: la cuenta 21 del B17 es la 2.1,
        // la 13 es la 1.3 (neta de la 1.3.99) y la 1399 es la 1.3.99 (verificado contra el B17 de mayo 2025).
        // Hasta que el B17 se genere en el motor de balances (C19b), se compara con la contabilidad directa.
        ControlCierre oficial = new ControlCierre("B17_G", "B17_OFICIAL",
                "B17 oficial: suma de control, padres = hijas, cuentas excluidas");
        oficial.setEstado(ControlCierre.ADVERTENCIA);
        oficial.setDetalle("Pendiente C19b: la integridad del archivo B17 (líneas, suma de control, padres = suma de"
                + " hijas, cuentas sin renglón) se valida cuando el B17 se genere en el motor de balances. Mientras"
                + " tanto, los controles B17 ↔ G de abajo usan la contabilidad del SAA con las cuentas del catálogo"
                + " (21 = 2.1, 13 = 1.3, 1399 = 1.3.99).");
        controles.add(oficial);

        Long ejrdG42 = ultimaEstructura(anio, mes, "G42");
        Long ejrdG48 = ultimaEstructura(anio, mes, "G48");
        if (ejrdG42 == null && ejrdG48 == null) {
            ControlCierre c = new ControlCierre("B17_G", "G_NO_GENERADAS", "Estructuras G42 y G48 del mes");
            c.setEstado(ControlCierre.ADVERTENCIA);
            c.setDetalle("No hay G42 ni G48 generadas para " + anio + "-" + dos(mes)
                    + " (RPR.EJRC/EJRD). Generarlas desde Reportes > Superintendencia (exige la cartera cerrada).");
            controles.add(c);
            return;
        }

        if (ejrdG42 != null) {
            double c21 = -saldoDeudor(idEmpresa, corteSig, "2.1.", null);
            double g42 = suma(" SELECT SUM(NVL(CG42SAPP, 0) + NVL(CG42SAPE, 0) + NVL(CG42SAVL, 0) + NVL(CG42RNDM, 0)) "
                    + " FROM RPR.CG42 WHERE CG42EJRD = :ejrd", ejrdG42);
            ControlCierre c = new ControlCierre("B17_G", "B17_21_VS_G42",
                    "Cuenta 21 (2.1, cuentas individuales) = saldo total de la G42");
            compara(c, c21, g42, "Saldo acreedor 2.1 al " + corte + ": " + fmt(c21) + "; G42 (ejecución detalle " + ejrdG42
                    + ", saldos patronal + personal + voluntario + rendimiento): " + fmt(g42) + ".");
            controles.add(c);
        } else {
            controles.add(sinEstructura("B17_21_VS_G42", "G42", anio, mes));
        }

        if (ejrdG48 != null) {
            double bruta = saldoDeudor(idEmpresa, corteSig, "1.3.", "1.3.99");
            double provision = -saldoDeudor(idEmpresa, corteSig, "1.3.99", null);
            Object[] g48 = fila(" SELECT SUM(NVL(CG48VPVN, 0) + NVL(CG48VLVN, 0)), SUM(NVL(CG48PRCN, 0)), COUNT(*) "
                    + " FROM RPR.CG48 WHERE CG48EJRD = :ejrd", ejrdG48);
            double gBruta = aDouble(g48[0]);
            double gProv = aDouble(g48[1]);
            ControlCierre c = new ControlCierre("B17_G", "B17_13_VS_G48",
                    "Cartera bruta (13 − 1399 del B17 = 1.3 sin 1.3.99) = por vencer + vencido de la G48");
            compara(c, bruta, gBruta, "Contabilidad 1.3 sin 1.3.99 al " + corte + ": " + fmt(bruta) + "; G48 (ejecución"
                    + " detalle " + ejrdG48 + ", " + aLong(g48[2]) + " operaciones): " + fmt(gBruta) + ".");
            controles.add(c);
            ControlCierre p = new ControlCierre("B17_G", "B17_1399_VS_G48",
                    "Provisión (−1399 del B17 = −1.3.99) = provisión constituida de la G48");
            compara(p, provision, gProv, "Saldo acreedor 1.3.99 al " + corte + ": " + fmt(provision)
                    + "; provisión constituida G48: " + fmt(gProv) + ". Con el ⑧ (modo al corte) deben coincidir (D-015).");
            controles.add(p);
        } else {
            controles.add(sinEstructura("B17_13_VS_G48", "G48", anio, mes));
        }
    }

    private ControlCierre sinEstructura(String codigo, String estructura, Long anio, Long mes) {
        ControlCierre c = new ControlCierre("B17_G", codigo, "Cuadratura B17 ↔ " + estructura);
        c.setEstado(ControlCierre.ADVERTENCIA);
        c.setDetalle("La " + estructura + " de " + anio + "-" + dos(mes) + " no está generada (o ninguna ejecución salió OK).");
        return c;
    }

    /** Detalle (RPR.EJRD) más reciente y OK de la estructura en el mes: la vigente tras regenerar o corregir. */
    private Long ultimaEstructura(Long anio, Long mes, String tipo) {
        Query q = em.createNativeQuery(
                " SELECT MAX(d.EJRDCDGO) FROM RPR.EJRD d JOIN RPR.EJRC c ON c.EJRCCDGO = d.EJRCCDGO "
                + " WHERE c.EJRCANOO = :anio AND c.EJRCMESS = :mes AND d.EJRDTPRP = :tipo AND d.EJRDESTD = 1");
        q.setParameter("anio", anio);
        q.setParameter("mes", mes);
        q.setParameter("tipo", tipo);
        Object r = q.getSingleResult();
        return r != null ? aLong(r) : null;
    }

    private double suma(String sql, Long ejrd) {
        Object[] f = fila(sql, ejrd);
        return aDouble(f[0]);
    }

    private Object[] fila(String sql, Long ejrd) {
        Query q = em.createNativeQuery(sql);
        q.setParameter("ejrd", ejrd);
        Object r = q.getSingleResult();
        return r instanceof Object[] ? (Object[]) r : new Object[] { r };
    }

    // =====================================================================
    // Utilitarios
    // =====================================================================

    /** Saldo deudor de las cuentas que empiezan con {@code prefijo} (y no con {@code excluido}) al corte. */
    private double saldoDeudor(Long idEmpresa, LocalDate corteSig, String prefijo, String excluido) {
        Query q = em.createNativeQuery(
                " SELECT SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) "
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO "
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig "
                + " AND d.DTASCNTA LIKE :prefijo " + (excluido != null ? " AND d.DTASCNTA NOT LIKE :excluido " : ""));
        q.setParameter("empresa", idEmpresa);
        q.setParameter("corteSig", Date.valueOf(corteSig));
        q.setParameter("prefijo", prefijo + "%");
        if (excluido != null) {
            q.setParameter("excluido", excluido + "%");
        }
        return aDouble(q.getSingleResult());
    }

    private static void compara(ControlCierre c, double valor, double esperado, String detalle) {
        double dif = valor - esperado;
        c.setValor(redondea(valor));
        c.setEsperado(redondea(esperado));
        c.setDiferencia(redondea(dif));
        c.setEstado(Math.abs(dif) < TOLERANCIA ? ControlCierre.OK : ControlCierre.ERROR);
        c.setDetalle(detalle + (Math.abs(dif) < TOLERANCIA ? "" : " Diferencia: " + fmt(dif) + "."));
    }

    private static double valor(Map<String, Double> mapa, String clave) {
        Double v = mapa.get(clave);
        return v != null ? v.doubleValue() : 0D;
    }

    private static Double redondea(double v) {
        double r = Math.round(v * 100D) / 100D;
        return Double.valueOf(r == 0D ? 0D : r); // sin "-0.0"
    }

    private static String fmt(double v) {
        return String.format(ES_EC, "%,.2f", redondea(v));
    }

    private static String dos(Long mes) {
        return mes != null && mes < 10 ? "0" + mes : String.valueOf(mes);
    }

    private static double aDouble(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0D;
    }

    private static Long aLong(Object o) {
        return o instanceof Number ? Long.valueOf(((Number) o).longValue()) : null;
    }
}
