package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.dto.ControlCierre;
import com.saa.ejb.crd.daoImpl.AporteVigenteSql;
import com.saa.ejb.crd.service.ControlAportesService;
import com.saa.ejb.crd.service.ReglaControlAportes;
import com.saa.ejb.crd.service.dto.ResultadoControlAportes;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** C53: ver {@link ControlAportesService}. Solo lectura. */
@Stateless
@SuppressWarnings("unchecked")
public class ControlAportesServiceImpl implements ControlAportesService {

    /** Aportes que el texto lista; el total siempre se dice. */
    static final int MAXIMO_LISTADOS = 15;

    @PersistenceContext
    EntityManager em;

    /** REQUIRES_NEW: solo lectura; si una consulta falla no marca rollback la transaccion del cierre que la llama. */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoControlAportes consultar(Long idEmpresa, Long anio, Long mes) throws Throwable {
        System.out.println("ControlAportesService.consultar - empresa: " + idEmpresa + " - " + anio + "-" + mes);
        if (idEmpresa == null || anio == null || mes == null || mes < 1 || mes > 12) {
            throw new IncomeException("Empresa, año y mes (1 a 12) son obligatorios.");
        }
        YearMonth ym = YearMonth.of(anio.intValue(), mes.intValue());
        LocalDate inicio = ym.atDay(1);
        LocalDate corteSig = ym.atEndOfMonth().plusDays(1);
        ResultadoControlAportes r = new ResultadoControlAportes();
        sinAsiento(r, inicio, corteSig);
        contraModulo(r, idEmpresa, corteSig);
        return r;
    }

    /** Control A. Respaldo de un REGISTRO APORTE: PGAP vigente con numero de asiento, o linea (DCBC) de un cobro con asiento definitivo (CBCRASN2) vigente. */
    private void sinAsiento(ResultadoControlAportes r, LocalDate inicio, LocalDate corteSig) {
        List<Object[]> filas = em.createNativeQuery(
                " SELECT a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTFCTR FROM CRD.APRT a"
                + " WHERE " + AporteVigenteSql.VIGENTE_SQL
                + "   AND a.APRTTPMV = " + ReglaControlAportes.MOVIMIENTO_CON_VINCULO + " AND NVL(a.APRTVLRR, 0) > 0"
                + "   AND a.APRTFCTR >= :inicio AND a.APRTFCTR < :corteSig"
                + "   AND NOT EXISTS (SELECT 1 FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO AND NVL(p.PGAPIDST, 1) = 1"
                + "        AND (p.PGAPNMAS IS NOT NULL OR EXISTS (SELECT 1 FROM CRD.DCBC d JOIN CRD.CBCR c ON c.CBCRCDGO = d.CBCRCDGO"
                + "             JOIN CNT.ASNT s ON s.ASNTCDGO = c.CBCRASN2 WHERE d.PGAPCDGO = p.PGAPCDGO AND s.ASNTESTD IN (1, 3))))"
                + " ORDER BY a.APRTCDGO")
                .setParameter("inicio", java.sql.Date.valueOf(inicio)).setParameter("corteSig", java.sql.Date.valueOf(corteSig)).getResultList();
        double total = 0.0;
        for (Object[] f : filas) {
            ResultadoControlAportes.AporteSinAsiento a = new ResultadoControlAportes.AporteSinAsiento();
            a.setIdAporte(((Number) f[0]).longValue());
            a.setIdEntidad(f[1] != null ? ((Number) f[1]).longValue() : null);
            a.setIdTipoAporte(f[2] != null ? ((Number) f[2]).longValue() : null);
            a.setValor(((Number) f[3]).doubleValue());
            a.setFecha(f[4] != null ? f[4].toString().substring(0, 10) : null);
            total += a.getValor();
            r.getSinAsiento().add(a);
        }
        r.setTotalSinAsiento(Math.round(total * 100.0) / 100.0);
    }

    /** Control B: la suma neta de aportes vigentes al corte por tipo (lectura de la G42) contra el saldo acreedor de la cuenta de pasivo del tipo (CTAP), por cuenta y en total 2.1. */
    private void contraModulo(ResultadoControlAportes r, Long idEmpresa, LocalDate corteSig) {
        Map<Long, String> cuentaPorTipo = new LinkedHashMap<Long, String>();
        for (Object[] f : (List<Object[]>) em.createNativeQuery(
                " SELECT ct.TPAPCDGO, MIN(pl.PLNNCNTA) FROM CRD.CTAP ct JOIN CNT.PLNN pl ON pl.PLNNCDGO = ct.CTAPPLNP"
                + " WHERE ct.CTAPESTD = " + Estado.ACTIVO + " AND ct.PJRQCDGO = :empresa GROUP BY ct.TPAPCDGO")
                .setParameter("empresa", idEmpresa).getResultList()) {
            if (f[0] != null && f[1] != null) {
                cuentaPorTipo.put(((Number) f[0]).longValue(), f[1].toString().trim());
            }
        }
        Map<String, Double> moduloPorCuenta = new TreeMap<String, Double>();
        Map<String, StringBuilder> tiposPorCuenta = new TreeMap<String, StringBuilder>();
        double modulo21 = 0.0;
        for (Object[] f : (List<Object[]>) em.createNativeQuery(
                " SELECT a.TPAPCDGO, SUM(NVL(a.APRTVLRR, 0)) FROM CRD.APRT a WHERE " + AporteVigenteSql.VIGENTE_SQL
                + " AND a.APRTFCTR < :corteSig GROUP BY a.TPAPCDGO")
                .setParameter("corteSig", java.sql.Date.valueOf(corteSig)).getResultList()) {
            Long tipo = f[0] != null ? ((Number) f[0]).longValue() : null;
            double valor = f[1] != null ? ((Number) f[1]).doubleValue() : 0.0;
            String cuenta = tipo != null ? cuentaPorTipo.get(tipo) : null;
            if (ReglaControlAportes.enTotal21(cuenta)) {
                modulo21 += valor;
            }
            if (cuenta != null) {
                moduloPorCuenta.merge(cuenta, valor, Double::sum);
                tiposPorCuenta.computeIfAbsent(cuenta, k -> new StringBuilder()).append(tiposPorCuenta.get(cuenta).length() > 0 ? "," : "").append(tipo);
            }
        }
        for (Map.Entry<String, Double> e : moduloPorCuenta.entrySet()) {
            double saldo = saldoAcreedor(idEmpresa, corteSig, e.getKey());
            if (ReglaControlAportes.hayDiferencia(e.getValue(), saldo)) {
                r.getDiferencias().add(diferencia(e.getKey(), tiposPorCuenta.get(e.getKey()).toString(), e.getValue(), saldo));
            }
        }
        double saldo21 = saldoAcreedor(idEmpresa, corteSig, ReglaControlAportes.RAIZ_APORTES);
        if (ReglaControlAportes.hayDiferencia(modulo21, saldo21)) {
            r.getDiferencias().add(diferencia("TOTAL " + ReglaControlAportes.RAIZ_APORTES, "todos los tipos en 2.1 o sin cuenta en CTAP", modulo21, saldo21));
        }
    }

    private static ResultadoControlAportes.DiferenciaCuenta diferencia(String cuenta, String tipos, double modulo, double saldo) {
        ResultadoControlAportes.DiferenciaCuenta d = new ResultadoControlAportes.DiferenciaCuenta();
        d.setCuenta(cuenta);
        d.setTipos(tipos);
        d.setValorModulo(Math.round(modulo * 100.0) / 100.0);
        d.setSaldoContable(Math.round(saldo * 100.0) / 100.0);
        d.setDiferencia(ReglaControlAportes.diferencia(modulo, saldo));
        return d;
    }

    /** Saldo acreedor (haber - debe) de la cuenta y sus hijas al corte, asientos vigentes (1 y 3) de la empresa. */
    private double saldoAcreedor(Long idEmpresa, LocalDate corteSig, String cuenta) {
        Object r = em.createNativeQuery(
                " SELECT NVL(SUM(NVL(d.DTASHBRR,0)) - SUM(NVL(d.DTASDBEE,0)), 0) FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig AND (d.DTASCNTA = :cuenta OR d.DTASCNTA LIKE :hijas)")
                .setParameter("empresa", idEmpresa).setParameter("corteSig", java.sql.Date.valueOf(corteSig))
                .setParameter("cuenta", cuenta).setParameter("hijas", cuenta + ".%").getSingleResult();
        return r instanceof Number ? ((Number) r).doubleValue() : 0.0;
    }

    private static String textoSinAsiento(ResultadoControlAportes r) {
        if (r.getSinAsiento().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(r.getSinAsiento().size() + " aporte(s) vigente(s) del mes (REGISTRO APORTE) sin asiento ni cobro que los respalde, por "
                + ReglaControlAportes.monto(r.getTotalSinAsiento()) + ": ");
        int n = 0;
        for (ResultadoControlAportes.AporteSinAsiento a : r.getSinAsiento()) {
            if (n++ >= MAXIMO_LISTADOS) {
                sb.append(" ... y ").append(r.getSinAsiento().size() - MAXIMO_LISTADOS).append(" mas");
                break;
            }
            sb.append(n > 1 ? "; " : "").append("APRT ").append(a.getIdAporte()).append(" entidad ").append(a.getIdEntidad()).append(" tipo ").append(a.getIdTipoAporte())
                    .append(" valor ").append(ReglaControlAportes.monto(a.getValor())).append(" del ").append(a.getFecha());
        }
        return sb.toString();
    }

    private static String textoDiferencias(ResultadoControlAportes r) {
        if (r.getDiferencias().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("La 2.1 contable no coincide con los aportes vigentes del modulo (contabilidad menos modulo): ");
        int n = 0;
        for (ResultadoControlAportes.DiferenciaCuenta d : r.getDiferencias()) {
            sb.append(n++ > 0 ? "; " : "").append(d.getCuenta()).append(" modulo ").append(ReglaControlAportes.monto(d.getValorModulo()))
                    .append(" contable ").append(ReglaControlAportes.monto(d.getSaldoContable())).append(" diferencia ").append(ReglaControlAportes.monto(d.getDiferencia()));
        }
        return sb.toString();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String textoControl(Long idEmpresa, Long anio, Long mes) throws Throwable {
        ResultadoControlAportes r = consultar(idEmpresa, anio, mes);
        String a = textoSinAsiento(r);
        String b = textoDiferencias(r);
        if (a == null && b == null) {
            return null;
        }
        return "C53 " + (a != null ? a + ". " : "") + (b != null ? b + "." : "");
    }

    /** REQUIRES_NEW: si una consulta falla, solo cae esta transaccion y no la del semaforo ni la del candado. */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<ControlCierre> controlesCierre(Long idEmpresa, Long anio, Long mes) {
        ControlCierre a = new ControlCierre("APORTES", "APORTES_SIN_ASIENTO", "Aportes vigentes del mes sin asiento ni cobro que los respalde");
        ControlCierre b = new ControlCierre("APORTES", "APORTES_21_VS_MODULO", "2.1 contable contra los aportes vigentes del modulo");
        try {
            ResultadoControlAportes r = consultar(idEmpresa, anio, mes);
            String ta = textoSinAsiento(r);
            a.setValor(Double.valueOf(r.getTotalSinAsiento()));
            a.setEsperado(Double.valueOf(0));
            a.setDiferencia(Double.valueOf(r.getTotalSinAsiento()));
            a.setEstado(ta == null ? ControlCierre.OK : ControlCierre.ERROR);
            a.setDetalle(ta == null ? "Todo aporte vigente del mes de REGISTRO APORTE tiene asiento o cobro." : ta);
            String tb = textoDiferencias(r);
            double dif = 0.0;
            for (ResultadoControlAportes.DiferenciaCuenta d : r.getDiferencias()) {
                if (d.getCuenta().startsWith("TOTAL")) {
                    dif = d.getDiferencia();
                }
            }
            b.setValor(Double.valueOf(r.getDiferencias().size()));
            b.setEsperado(Double.valueOf(0));
            b.setDiferencia(Double.valueOf(dif));
            b.setEstado(tb == null ? ControlCierre.OK : ControlCierre.ERROR);
            b.setDetalle(tb == null ? "La 2.1 contable coincide con los aportes vigentes del modulo, por cuenta y en total." : tb);
        } catch (Throwable e) {
            String motivo = "No se pudo medir: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            a.setEstado(ControlCierre.ADVERTENCIA);
            a.setDetalle(motivo);
            b.setEstado(ControlCierre.ADVERTENCIA);
            b.setDetalle(motivo);
        }
        List<ControlCierre> l = new ArrayList<ControlCierre>();
        l.add(a);
        l.add(b);
        return l;
    }
}
