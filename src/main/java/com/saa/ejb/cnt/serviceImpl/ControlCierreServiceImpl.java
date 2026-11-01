package com.saa.ejb.cnt.serviceImpl;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.ControlCierreService;
import com.saa.ejb.cnt.service.ParametroContableService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.cnt.service.dto.GruposBandaCartera;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.rubros.CntParametroEstadoFinanciero;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.Rubros;
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

    /** P1: las cuentas puente salen de las líneas de la plantilla de apertura (no escritas en el código). */
    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    /** P2/P8/P4-A: parámetros contables del rubro 250 (grupos de nivel 1, aportes de los partícipes, cartera). */
    @EJB
    private ParametroContableService parametroContableService;

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

    private void controlesPuentes(List<ControlCierre> controles, Long idEmpresa, LocalDate corte, LocalDate corteSig)
            throws Throwable {
        // P1 (PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md): las cuatro cuentas puente se LEEN de la plantilla de
        // apertura del mes (alterno 1, PlantillasCredito.APERTURA_PLANILLA_MENSUAL), que tiene las cuatro
        // líneas del catálogo CrdLineaAsiento (aux1 1-4). Ya no hay cuentas escritas en el código ni respaldo:
        // si falta la plantilla o una línea, el control lo dice y no se evalúa.
        String cobrarAportes = cuentaPuente(idEmpresa, CrdLineaAsiento.APORTES_POR_COBRAR);
        String aplicarAportes = cuentaPuente(idEmpresa, CrdLineaAsiento.APORTES_POR_APLICAR);
        String cobrarPrestamos = cuentaPuente(idEmpresa, CrdLineaAsiento.PRESTAMOS_POR_COBRAR);
        String aplicarPrestamos = cuentaPuente(idEmpresa, CrdLineaAsiento.PRESTAMOS_POR_APLICAR);
        if (cobrarAportes == null || aplicarAportes == null || cobrarPrestamos == null || aplicarPrestamos == null) {
            ControlCierre c = new ControlCierre("PUENTES", "PUENTES_SIN_PLANTILLA",
                    "Cuentas puente en cero al " + corte + " (D-024)");
            c.setEstado(ControlCierre.ADVERTENCIA);
            c.setDetalle("No se pudo medir: la plantilla contable alterno " + PlantillasCredito.APERTURA_PLANILLA_MENSUAL
                    + " (apertura de la planilla mensual) de la empresa " + idEmpresa + " no tiene todas las líneas de las"
                    + " cuentas puente. Faltan: " + faltantes(cobrarAportes, "aportes por cobrar (aux1 " + CrdLineaAsiento.APORTES_POR_COBRAR + ")",
                            aplicarAportes, "aportes por aplicar (aux1 " + CrdLineaAsiento.APORTES_POR_APLICAR + ")",
                            cobrarPrestamos, "préstamos por cobrar (aux1 " + CrdLineaAsiento.PRESTAMOS_POR_COBRAR + ")",
                            aplicarPrestamos, "préstamos por aplicar (aux1 " + CrdLineaAsiento.PRESTAMOS_POR_APLICAR + ")")
                    + ". Corrija la plantilla; no hay cuenta de respaldo.");
            controles.add(c);
            return;
        }
        for (String cuenta : new String[] { cobrarAportes, aplicarAportes }) {
            double saldo = saldoDeudor(idEmpresa, corteSig, cuenta, null);
            ControlCierre c = new ControlCierre("PUENTES", "PUENTE_" + cuenta,
                    "Cuenta puente " + cuenta + " en cero al " + corte + " (D-024)");
            compara(c, saldo, 0D, "Saldo deudor de " + cuenta + " y subcuentas: " + fmt(saldo)
                    + ". D-024: la cuenta puente de excedentes/compensaciones debe quedar en 0 cada mes.");
            controles.add(c);
        }
        double s14 = saldoDeudor(idEmpresa, corteSig, cobrarPrestamos, null);
        double s23 = saldoDeudor(idEmpresa, corteSig, aplicarPrestamos, null);
        ControlCierre c = new ControlCierre("PUENTES", "PUENTE_" + cobrarPrestamos + "_" + aplicarPrestamos,
                cobrarPrestamos + " igual y opuesta a " + aplicarPrestamos + " al " + corte + " (D-024)");
        // valor = por cobrar (deudor), esperado = −por aplicar (deudor), es decir el saldo acreedor de la por aplicar.
        compara(c, s14, -s23, "Saldo deudor " + cobrarPrestamos + ": " + fmt(s14) + "; saldo acreedor " + aplicarPrestamos + ": "
                + fmt(-s23) + ". Deben ser iguales (excedentes D-010 por " + aplicarPrestamos + ").");
        controles.add(c);
    }

    /** Cuenta contable de la línea {@code aux1} de la plantilla de apertura de la empresa, o {@code null} si no existe. */
    private String cuentaPuente(Long idEmpresa, int aux1) throws Throwable {
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.APERTURA_PLANILLA_MENSUAL, idEmpresa);
        if (idPlantilla == null) {
            return null;
        }
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, aux1);
        if (linea == null || linea.getPlanCuenta() == null || linea.getPlanCuenta().getCuentaContable() == null
                || linea.getPlanCuenta().getCuentaContable().trim().isEmpty()) {
            return null;
        }
        return linea.getPlanCuenta().getCuentaContable().trim();
    }

    /** Lista, separada por comas, las descripciones cuyo valor leído es {@code null}. Pares (valor, descripción). */
    private static String faltantes(String... paresValorDescripcion) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < paresValorDescripcion.length; i += 2) {
            if (paresValorDescripcion[i] == null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(paresValorDescripcion[i + 1]);
            }
        }
        return sb.toString();
    }

    // =====================================================================
    // (b) Ecuación contable
    // =====================================================================

    private void controlEcuacion(List<ControlCierre> controles, Long idEmpresa, LocalDate corte, LocalDate corteSig) {
        // P2 (PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md): los grupos de nivel 1 salen del rubro 250
        // (CntParametroEstadoFinanciero.GRUPO_*), el mismo que leen los estados financieros oficiales
        // (EstadoFinancieroOficialServiceImpl). Antes eran los dígitos 1-5 escritos aquí. Sin el rubro o con un
        // grupo vacío no hay respaldo: el control lo dice y no se evalúa.
        Map<Integer, String> p = parametrosEstadosFinancieros();
        String gActivo = p.get(CntParametroEstadoFinanciero.GRUPO_ACTIVO);
        String gPasivo = p.get(CntParametroEstadoFinanciero.GRUPO_PASIVO);
        String gPatrimonio = p.get(CntParametroEstadoFinanciero.GRUPO_PATRIMONIO);
        String gIngresos = p.get(CntParametroEstadoFinanciero.GRUPO_INGRESOS);
        String gGastos = p.get(CntParametroEstadoFinanciero.GRUPO_GASTOS);
        ControlCierre c = new ControlCierre("ECUACION", "ECUACION_CONTABLE",
                "Ecuación contable al " + corte + ": activo = pasivo + patrimonio + resultado");
        if (gActivo == null || gPasivo == null || gPatrimonio == null || gIngresos == null || gGastos == null) {
            c.setEstado(ControlCierre.ADVERTENCIA);
            c.setDetalle("No se pudo medir: el rubro " + Rubros.CNT_PARAMETROS_ESTADOS_FINANCIEROS
                    + " (parámetros de los estados financieros) no tiene todos los grupos de nivel 1. Faltan: "
                    + faltantes(gActivo, "activo (GRUPO_ACTIVO " + CntParametroEstadoFinanciero.GRUPO_ACTIVO + ")",
                            gPasivo, "pasivo (GRUPO_PASIVO " + CntParametroEstadoFinanciero.GRUPO_PASIVO + ")",
                            gPatrimonio, "patrimonio (GRUPO_PATRIMONIO " + CntParametroEstadoFinanciero.GRUPO_PATRIMONIO + ")",
                            gIngresos, "ingresos (GRUPO_INGRESOS " + CntParametroEstadoFinanciero.GRUPO_INGRESOS + ")",
                            gGastos, "gastos (GRUPO_GASTOS " + CntParametroEstadoFinanciero.GRUPO_GASTOS + ")")
                    + ". Corrija el rubro; no hay grupo de respaldo.");
            controles.add(c);
            return;
        }
        double activo = saldoDeudor(idEmpresa, corteSig, gActivo, null);
        double pasivo = -saldoDeudor(idEmpresa, corteSig, gPasivo, null);
        double patrimonio = -saldoDeudor(idEmpresa, corteSig, gPatrimonio, null);
        double resultado = -saldoDeudor(idEmpresa, corteSig, gIngresos, null) - saldoDeudor(idEmpresa, corteSig, gGastos, null);
        compara(c, activo, pasivo + patrimonio + resultado,
                "Activo (" + gActivo + ") " + fmt(activo) + "; pasivo (" + gPasivo + ") " + fmt(pasivo) + "; patrimonio ("
                        + gPatrimonio + ") " + fmt(patrimonio) + "; resultado (" + gIngresos + " − " + gGastos + ") "
                        + fmt(resultado) + ". Mismo cálculo que la línea \"activo − pasivo − patrimonio − resultado\" de"
                        + " la cuadratura B17 ↔ G.");
        controles.add(c);
    }

    /** Parámetros del rubro 250 (estados financieros) activos: PDTRALTR → PDTRVLRV, sin vacíos. */
    private Map<Integer, String> parametrosEstadosFinancieros() {
        return parametroContableService.parametrosEstadosFinancieros();
    }

    // =====================================================================
    // (c) Cartera del módulo al corte vs contabilidad, por grupo (⑨)
    // =====================================================================

    private void controlesCartera(List<ControlCierre> controles, Long idEmpresa, Long anio, Long mes, LocalDate corte,
            LocalDate corteSig, EstadoPeriodo estado) {
        // P4-A: la raíz de la cartera sale del rubro 250 (CUENTA_CARTERA_CREDITOS); sin ella no se mide.
        String raiz = parametroContableService.valor(CntParametroEstadoFinanciero.CUENTA_CARTERA_CREDITOS);
        if (raiz == null) {
            controles.add(sinRaizCartera("CARTERA", "CARTERA_SIN_RAIZ", "Cartera del módulo vs contabilidad por grupo al " + corte));
            return;
        }
        String prefijoRaiz = ParametroContableService.comoPrefijo(raiz);
        // P4-B: las cuentas de la provisión salen de la plantilla 37 (líneas aux1 83); sin ellas no se mide.
        List<String> cuentasProvision = cuentasProvisionODefecto(idEmpresa);
        if (cuentasProvision.isEmpty()) {
            controles.add(sinProvisionCartera("CARTERA", "CARTERA_SIN_PROVISION",
                    "Cartera del módulo vs contabilidad por grupo al " + corte, idEmpresa));
            return;
        }
        // P4-C: el grupo Q/P/H sale de la configuración de bandas (BNDP → CBPR → PRDC.TPPRCDGO), una vez por control.
        GruposBandaCartera gruposBanda = parametroContableService.gruposBandaCartera(idEmpresa, prefijoRaiz);
        Map<String, Double> contable = capitalContablePorGrupo(idEmpresa, corteSig, prefijoRaiz, cuentasProvision, gruposBanda);
        Double contableSinGrupo = contable.remove(GruposBandaCartera.SIN_GRUPO);
        if (contableSinGrupo != null) {
            ControlCierre sg = new ControlCierre("CARTERA", "CARTERA_SIN_GRUPO",
                    "Cuentas de cartera sin grupo derivable de las bandas, con saldo contable");
            sg.setValor(redondea(contableSinGrupo));
            sg.setEstado(ControlCierre.ADVERTENCIA);
            sg.setDetalle("Hay " + fmt(contableSinGrupo) + " de saldo contable en cuentas " + raiz + " que no están en una"
                    + " banda activa de un único tipo de préstamo (CRD.BNDP/CBPR/PRDC): no entran a ningún grupo Q/P/H."
                    + " Revise la configuración de bandas.");
            controles.add(sg);
        }
        Long idCorrida = estado != null ? estado.getIdCorridaCartera() : null;
        if (idCorrida == null) {
            ControlCierre c = new ControlCierre("CARTERA", "CARTERA_NO_CERRADA",
                    "Cartera del módulo vs contabilidad por grupo al " + corte);
            c.setEstado(ControlCierre.ADVERTENCIA);
            StringBuilder sb = new StringBuilder("Cartera no cerrada: el mes " + anio + "-" + dos(mes)
                    + " no tiene corrida de cierre de cartera EJECUTADA vigente (CRD.CRCT), así que no hay"
                    + " distribución del módulo al corte con la cual comparar. Cierre la cartera antes del período"
                    + " contable (C17). Capital contable " + raiz + " (sin la provisión " + cuentasProvision + ") por grupo:");
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
            String grupo = grupoBanda(f[0] != null ? f[0].toString() : null, prefijoRaiz, cuentasProvision, gruposBanda);
            if (grupo == null) {
                sinCuenta += aDouble(f[1]);
                continue;
            }
            if (GruposBandaCartera.SIN_GRUPO.equals(grupo) && Math.abs(aDouble(f[1])) < TOLERANCIA) {
                continue; // sin grupo y sin capital: se ignora sin aviso (P4-C)
            }
            modulo.merge(grupo, aDouble(f[1]), Double::sum);
        }
        Double moduloSinGrupo = modulo.remove(GruposBandaCartera.SIN_GRUPO);
        if (moduloSinGrupo != null) {
            ControlCierre sg = new ControlCierre("CARTERA", "CARTERA_SIN_GRUPO_MODULO",
                    "Capital del módulo en bandas sin grupo derivable");
            sg.setValor(redondea(moduloSinGrupo));
            sg.setEstado(ControlCierre.ADVERTENCIA);
            sg.setDetalle("La corrida " + idCorrida + " tiene " + fmt(moduloSinGrupo) + " de capital en cuentas " + raiz
                    + " que no están en una banda activa de un único tipo de préstamo: no entran a ningún grupo Q/P/H.");
            controles.add(sg);
        }
        java.util.Set<String> grupos = new java.util.TreeSet<String>();
        grupos.addAll(modulo.keySet());
        grupos.addAll(contable.keySet());
        for (String grupo : grupos) {
            double cnt = valor(contable, grupo);
            double mod = valor(modulo, grupo);
            ControlCierre c = new ControlCierre("CARTERA", "CARTERA_GRUPO_" + grupo,
                    "Cartera grupo " + grupo + " (" + gruposBanda.nombreDe(grupo) + "): contabilidad vs módulo al " + corte);
            compara(c, cnt, mod, "Contabilidad " + raiz + " (sin la provisión) " + fmt(cnt) + "; módulo (corrida de cierre " + idCorrida
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
                    + " de capital en bandas sin cuenta " + raiz + " (CRD.BDCC.PLNNCDGO nulo o fuera de " + raiz
                    + "): parametrice las bandas.");
            controles.add(c);
        }
    }

    /**
     * Capital contable de cartera (raíz sin la provisión) por grupo Q/P/H, la misma agrupación del ⑨ mensual.
     * P4-A: la raíz ({@code prefijoRaiz}, hoy «1.3.») se lee del rubro 250. P4-B: la provisión se excluye por las
     * cuentas de la plantilla 37 ({@code cuentasProvision}). P4-C: el grupo sale de {@code gruposBanda}; las cuentas
     * sin grupo se acumulan bajo {@link GruposBandaCartera#SIN_GRUPO} solo si tienen saldo (con saldo 0 se ignoran).
     */
    private Map<String, Double> capitalContablePorGrupo(Long idEmpresa, LocalDate corteSig, String prefijoRaiz,
            List<String> cuentasProvision, GruposBandaCartera gruposBanda) {
        Query q = em.createNativeQuery(
                " SELECT d.DTASCNTA, SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) "
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO "
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig "
                + " AND d.DTASCNTA LIKE :raiz AND d.DTASCNTA NOT IN (:provision) GROUP BY d.DTASCNTA");
        q.setParameter("empresa", idEmpresa);
        q.setParameter("corteSig", Date.valueOf(corteSig));
        q.setParameter("raiz", prefijoRaiz + "%");
        q.setParameter("provision", cuentasProvision);
        Map<String, Double> grupos = new TreeMap<String, Double>();
        for (Object fila : q.getResultList()) {
            Object[] f = (Object[]) fila;
            String grupo = grupoBanda(f[0] != null ? f[0].toString() : null, prefijoRaiz, cuentasProvision, gruposBanda);
            if (grupo != null) {
                if (GruposBandaCartera.SIN_GRUPO.equals(grupo) && Math.abs(aDouble(f[1])) < TOLERANCIA) {
                    continue; // sin grupo y con saldo 0 (p. ej. 1.3.07): se ignora sin aviso
                }
                grupos.merge(grupo, aDouble(f[1]), Double::sum);
            }
        }
        return grupos;
    }

    /**
     * Grupo de la cuenta de banda, el mismo del ⑨ mensual ({@code CierreCarteraServiceImpl.grupoBanda}): la letra que
     * da {@code gruposBanda} (P4-C, BNDP → CBPR → PRDC.TPPRCDGO) o {@link GruposBandaCartera#SIN_GRUPO}; {@code null}
     * si la cuenta no es de la raíz de cartera o es de la provisión (P4-A, P4-B).
     */
    private static String grupoBanda(String cuenta, String prefijoRaiz, List<String> cuentasProvision,
            GruposBandaCartera gruposBanda) {
        if (cuenta == null || !cuenta.startsWith(prefijoRaiz) || cuentasProvision.contains(cuenta)) {
            return null;
        }
        return gruposBanda.grupoDe(cuenta);
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
        // la 13 es la raíz de la cartera y la 1399 son las cuentas de provisión de la plantilla 37 (verificado contra
        // el B17 de mayo 2025; hoy 1.3 y 1.3.99.xx, leídas del rubro 250 y de la plantilla, no escritas en el código).
        // Hasta que el B17 se genere en el motor de balances (C19b), se compara con la contabilidad directa.
        ControlCierre oficial = new ControlCierre("B17_G", "B17_OFICIAL",
                "B17 oficial: suma de control, padres = hijas, cuentas excluidas");
        oficial.setEstado(ControlCierre.ADVERTENCIA);
        oficial.setDetalle("Pendiente C19b: la integridad del archivo B17 (líneas, suma de control, padres = suma de"
                + " hijas, cuentas sin renglón) se valida cuando el B17 se genere en el motor de balances. Mientras"
                + " tanto, los controles B17 ↔ G de abajo usan la contabilidad del SAA con las cuentas del catálogo"
                + " (21 = " + textoParametro(CntParametroEstadoFinanciero.CUENTA_APORTES_PARTICIPES) + ", 13 = "
                + textoParametro(CntParametroEstadoFinanciero.CUENTA_CARTERA_CREDITOS) + ", leídas del rubro "
                + Rubros.CNT_PARAMETROS_ESTADOS_FINANCIEROS + ").");
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
            // P8 (PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md): la cuenta 21 del B17 (cuentas individuales de los
            // partícipes) se LEE del rubro 250, detalle CUENTA_APORTES_PARTICIPES; antes era el literal "2.1.".
            // Sin el detalle no se mide: no hay cuenta de respaldo.
            String cuenta21 = parametrosEstadosFinancieros().get(CntParametroEstadoFinanciero.CUENTA_APORTES_PARTICIPES);
            if (cuenta21 == null) {
                ControlCierre c = new ControlCierre("B17_G", "B17_21_VS_G42",
                        "Cuenta 21 (cuentas individuales de los partícipes) = saldo total de la G42");
                c.setEstado(ControlCierre.ADVERTENCIA);
                c.setDetalle("No se pudo medir: el rubro " + Rubros.CNT_PARAMETROS_ESTADOS_FINANCIEROS
                        + " no tiene el detalle CUENTA_APORTES_PARTICIPES (" + CntParametroEstadoFinanciero.CUENTA_APORTES_PARTICIPES
                        + ") con la cuenta de nivel 2 de los aportes de los partícipes (B17 cuenta 21). Cárguelo; no hay"
                        + " cuenta de respaldo.");
                controles.add(c);
            } else {
                // mismo criterio de prefijo que el literal anterior ("2.1." con punto): la cuenta y sus subcuentas
                String prefijo21 = cuenta21.endsWith(".") ? cuenta21 : cuenta21 + ".";
                double c21 = -saldoDeudor(idEmpresa, corteSig, prefijo21, null);
                double g42 = suma(" SELECT SUM(NVL(CG42SAPP, 0) + NVL(CG42SAPE, 0) + NVL(CG42SAVL, 0) + NVL(CG42RNDM, 0)) "
                        + " FROM RPR.CG42 WHERE CG42EJRD = :ejrd", ejrdG42);
                ControlCierre c = new ControlCierre("B17_G", "B17_21_VS_G42",
                        "Cuenta 21 (" + cuenta21 + ", cuentas individuales) = saldo total de la G42");
                compara(c, c21, g42, "Saldo acreedor " + cuenta21 + " al " + corte + ": " + fmt(c21) + "; G42 (ejecución detalle "
                        + ejrdG42 + ", saldos patronal + personal + voluntario + rendimiento): " + fmt(g42) + ".");
                controles.add(c);
            }
        } else {
            controles.add(sinEstructura("B17_21_VS_G42", "G42", anio, mes));
        }

        String raizCartera = ejrdG48 != null
                ? parametroContableService.valor(CntParametroEstadoFinanciero.CUENTA_CARTERA_CREDITOS) : null;
        if (ejrdG48 != null && raizCartera == null) {
            // P4-A: sin la raíz de la cartera en el rubro 250 no se mide (sin respaldo escrito).
            controles.add(sinRaizCartera("B17_G", "B17_13_VS_G48", "Cuadratura B17 ↔ G48 (cartera bruta y provisión)"));
        } else if (ejrdG48 != null) {
            // P4-A: la raíz se lee del rubro 250. P4-B: la provisión = las cuentas de las líneas aux1 83 de la
            // plantilla 37; la cartera bruta es la raíz sin ellas. Sin esas cuentas no se mide.
            List<String> cuentasProvision = cuentasProvisionODefecto(idEmpresa);
            if (cuentasProvision.isEmpty()) {
                controles.add(sinProvisionCartera("B17_G", "B17_13_VS_G48", "Cuadratura B17 ↔ G48 (cartera bruta y provisión)",
                        idEmpresa));
                return;
            }
            double bruta = saldoDeudorExcluyendo(idEmpresa, corteSig, ParametroContableService.comoPrefijo(raizCartera),
                    cuentasProvision);
            double provision = -saldoDeudorCuentas(idEmpresa, corteSig, cuentasProvision);
            Object[] g48 = fila(" SELECT SUM(NVL(CG48VPVN, 0) + NVL(CG48VLVN, 0)), SUM(NVL(CG48PRCN, 0)), COUNT(*) "
                    + " FROM RPR.CG48 WHERE CG48EJRD = :ejrd", ejrdG48);
            double gBruta = aDouble(g48[0]);
            double gProv = aDouble(g48[1]);
            ControlCierre c = new ControlCierre("B17_G", "B17_13_VS_G48",
                    "Cartera bruta (13 − 1399 del B17 = " + raizCartera + " sin la provisión) = por vencer + vencido de la G48");
            compara(c, bruta, gBruta, "Contabilidad " + raizCartera + " sin la provisión (" + cuentasProvision + ") al " + corte + ": " + fmt(bruta) + "; G48 (ejecución"
                    + " detalle " + ejrdG48 + ", " + aLong(g48[2]) + " operaciones): " + fmt(gBruta) + ".");
            controles.add(c);
            ControlCierre p = new ControlCierre("B17_G", "B17_1399_VS_G48",
                    "Provisión (−1399 del B17 = −" + cuentasProvision + ") = provisión constituida de la G48");
            compara(p, provision, gProv, "Saldo acreedor " + cuentasProvision + " al " + corte + ": " + fmt(provision)
                    + "; provisión constituida G48: " + fmt(gProv) + ". Con el ⑧ (modo al corte) deben coincidir (D-015).");
            controles.add(p);
        } else {
            controles.add(sinEstructura("B17_13_VS_G48", "G48", anio, mes));
        }
    }

    /** Cuentas de la provisión de cartera (plantilla 37, aux1 83); lista vacía si no están. */
    private List<String> cuentasProvisionODefecto(Long idEmpresa) {
        try {
            return parametroContableService.cuentasProvisionCartera(idEmpresa);
        } catch (Throwable e) {
            System.err.println("ControlCierre.cuentasProvisionCartera: " + e.getMessage());
            return new ArrayList<String>();
        }
    }

    /** P4-B: control «No se pudo medir» porque la plantilla 37 no tiene la provisión de cartera (aux1 83). */
    private ControlCierre sinProvisionCartera(String grupo, String codigo, String descripcion, Long idEmpresa) {
        ControlCierre c = new ControlCierre(grupo, codigo, descripcion);
        c.setEstado(ControlCierre.ADVERTENCIA);
        c.setDetalle("No se pudo medir: la plantilla contable alterno " + PlantillasCredito.PROVISION_CARTERA
                + " (provisión de cartera) de la empresa " + idEmpresa + " no existe o no tiene la línea "
                + CrdLineaAsiento.PROVISION_CARTERA + " con la cuenta de la provisión. Cárguela; no hay cuenta de respaldo.");
        return c;
    }

    /** Valor del detalle del rubro 250 para un mensaje, o «(sin cargar)» si falta. */
    private String textoParametro(int alternoDetalle) {
        String v = parametroContableService.valor(alternoDetalle);
        return v != null ? v : "(sin cargar)";
    }

    /** P4-A: control «No se pudo medir» porque el rubro 250 no tiene la raíz de la cartera de créditos. */
    private ControlCierre sinRaizCartera(String grupo, String codigo, String descripcion) {
        ControlCierre c = new ControlCierre(grupo, codigo, descripcion);
        c.setEstado(ControlCierre.ADVERTENCIA);
        c.setDetalle("No se pudo medir: el rubro " + Rubros.CNT_PARAMETROS_ESTADOS_FINANCIEROS + " no tiene el detalle"
                + " CUENTA_CARTERA_CREDITOS (" + CntParametroEstadoFinanciero.CUENTA_CARTERA_CREDITOS + ") con la cuenta de la"
                + " cartera de créditos (B17 cuenta 13). Cárguelo; no hay cuenta de respaldo.");
        return c;
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

    /** Saldo deudor de las cuentas del prefijo, excluyendo una lista de cuentas exactas (P4-B). */
    private double saldoDeudorExcluyendo(Long idEmpresa, LocalDate corteSig, String prefijo, List<String> excluidas) {
        Query q = em.createNativeQuery(
                " SELECT SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) "
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO "
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig "
                + " AND d.DTASCNTA LIKE :prefijo AND d.DTASCNTA NOT IN (:excluidas)");
        q.setParameter("empresa", idEmpresa);
        q.setParameter("corteSig", Date.valueOf(corteSig));
        q.setParameter("prefijo", prefijo + "%");
        q.setParameter("excluidas", excluidas);
        return aDouble(q.getSingleResult());
    }

    /** Saldo deudor de una lista de cuentas exactas (P4-B: las de la provisión). */
    private double saldoDeudorCuentas(Long idEmpresa, LocalDate corteSig, List<String> cuentas) {
        Query q = em.createNativeQuery(
                " SELECT SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) "
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO "
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig "
                + " AND d.DTASCNTA IN (:cuentas)");
        q.setParameter("empresa", idEmpresa);
        q.setParameter("corteSig", Date.valueOf(corteSig));
        q.setParameter("cuentas", cuentas);
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
