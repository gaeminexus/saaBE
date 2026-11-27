package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.EstadoCierreService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.cnt.service.ReglaSaldoContrario;
import com.saa.ejb.cnt.service.SaldoContrarioCuentaService;
import com.saa.ejb.cnt.service.dto.ControlCierre;
import com.saa.ejb.cnt.service.dto.ResultadoRegularizacionRedondeos;
import com.saa.ejb.cnt.service.dto.SaldoContrarioCuenta;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasContabilidad;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/** C52: ver {@link SaldoContrarioCuentaService}. */
@Stateless
@SuppressWarnings("unchecked")
public class SaldoContrarioCuentaServiceImpl implements SaldoContrarioCuentaService {

    /** Cuentas que el control lista en el detalle; el total siempre se dice. */
    static final int MAXIMO_LISTADAS = 15;
    /** Prefijo de la observacion del asiento de regularizacion (llave de idempotencia). */
    static final String MARCA_REGULARIZACION = "C52 REGULARIZACION REDONDEOS ";

    @PersistenceContext
    EntityManager em;

    @EJB private PlantillaService plantillaService;
    @EJB private DetallePlantillaDaoService detallePlantillaDaoService;
    @EJB private AsientoContableService asientoContableService;
    @EJB private EstadoCierreService estadoCierreService;
    @EJB private UsuarioValidacionService usuarioValidacionService;

    // ------------------------------------------------------------------------------------------------ lectura

    @Override
    public List<SaldoContrarioCuenta> saldosContrarios(Long idEmpresa, Long anio, Long mes) throws Throwable {
        System.out.println("SaldoContrarioCuentaService.saldosContrarios - empresa: " + idEmpresa + " - " + anio + "-" + mes);
        validaPeriodo(idEmpresa, anio, mes);
        YearMonth ym = YearMonth.of(anio.intValue(), mes.intValue());
        LocalDate corteSig = ym.atEndOfMonth().plusDays(1);
        List<SaldoContrarioCuenta> lista = new ArrayList<SaldoContrarioCuenta>();

        // BALANCE: acumulado al corte (incluye el asiento de cierre del ejercicio, que ya movio los resultados a patrimonio).
        // El nodo «(» marca provision/valuacion: la cuenta o algun ancestro del plan (cuenta que la prefija) tiene un nombre que empieza con «(».
        Query qb = em.createNativeQuery(
                " SELECT d.DTASCNTA, p.PLNNNMBR, SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0)),"
                + "  (SELECT COUNT(*) FROM CNT.PLNN anc WHERE anc.PJRQCDGO = :empresa AND anc.PLNNNMBR LIKE '(%'"
                + "    AND (d.DTASCNTA = anc.PLNNCNTA OR d.DTASCNTA LIKE anc.PLNNCNTA || '.%'))"
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
                + " JOIN CNT.PLNN p ON p.PLNNCNTA = d.DTASCNTA AND p.PJRQCDGO = :empresa AND p.PLNNTPOO = 2"
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corteSig"
                + "   AND (d.DTASCNTA LIKE '1.%' OR d.DTASCNTA LIKE '2.%' OR d.DTASCNTA LIKE '3.%')"
                + " GROUP BY d.DTASCNTA, p.PLNNNMBR");
        qb.setParameter("empresa", idEmpresa);
        qb.setParameter("corteSig", java.sql.Date.valueOf(corteSig));
        for (Object[] f : (List<Object[]>) qb.getResultList()) {
            String cuenta = f[0].toString().trim();
            double saldo = ((Number) f[2]).doubleValue();
            boolean marca = f[3] != null && ((Number) f[3]).intValue() > 0;
            agrega(lista, cuenta, f[1], saldo, ReglaSaldoContrario.esProvisionOValuacion(cuenta, marca));
        }

        // RESULTADOS: acumulado del anio hasta el corte, sin el asiento de cierre del ejercicio.
        lista.addAll(resultados(idEmpresa, anio, corteSig, null));
        lista.sort((x, y) -> x.getCuenta().compareTo(y.getCuenta()));

        Set<String> redondeos = cuentasDeRedondeo(idEmpresa);
        for (SaldoContrarioCuenta s : lista) {
            s.setRegularizableAutomatico("RESULTADOS".equals(s.getTipo()) && s.getSaldo() < 0 && redondeos.contains(s.getCuenta()));
            s.setAsientoSugerido(sugerencia(idEmpresa, s));
        }
        return lista;
    }

    /** Saldo contrario de las cuentas 4 y 5 en el anio, sin el cierre; {@code soloCuentas} (nula = todas) limita a esas cuentas. */
    private List<SaldoContrarioCuenta> resultados(Long idEmpresa, Long anio, LocalDate corteSig, Set<String> soloCuentas) {
        Query qr = em.createNativeQuery(
                " SELECT d.DTASCNTA, p.PLNNNMBR, SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0))"
                + " FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
                + " JOIN CNT.PLNN p ON p.PLNNCNTA = d.DTASCNTA AND p.PJRQCDGO = :empresa AND p.PLNNTPOO = 2"
                + " WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTFCHA >= :inicio AND a.ASNTFCHA < :corteSig"
                + "   AND (d.DTASCNTA LIKE '4.%' OR d.DTASCNTA LIKE '5.%')"
                + "   AND NVL(a.ASNTOBSR, ' ') NOT LIKE '%" + ReglaSaldoContrario.MARCA_CIERRE_EJERCICIO + "%'"
                + "   AND NVL(a.ASNTOBSR, ' ') NOT LIKE '" + ReglaSaldoContrario.PREFIJO_CIERRE_MOTOR + "%'"
                + " GROUP BY d.DTASCNTA, p.PLNNNMBR");
        qr.setParameter("empresa", idEmpresa);
        qr.setParameter("inicio", java.sql.Date.valueOf(LocalDate.of(anio.intValue(), 1, 1)));
        qr.setParameter("corteSig", java.sql.Date.valueOf(corteSig));
        List<SaldoContrarioCuenta> lista = new ArrayList<SaldoContrarioCuenta>();
        for (Object[] f : (List<Object[]>) qr.getResultList()) {
            String cuenta = f[0].toString().trim();
            if (soloCuentas != null && !soloCuentas.contains(cuenta)) {
                continue;
            }
            agrega(lista, cuenta, f[1], ((Number) f[2]).doubleValue(), false);
        }
        return lista;
    }

    private static void agrega(List<SaldoContrarioCuenta> lista, String cuenta, Object nombre, double saldo, boolean provision) {
        double contrario = ReglaSaldoContrario.valorContrario(cuenta, saldo, provision);
        if (contrario <= 0.0) {
            return;
        }
        SaldoContrarioCuenta s = new SaldoContrarioCuenta();
        s.setCuenta(cuenta);
        s.setNombre(nombre != null ? nombre.toString().trim() : "");
        s.setNaturaleza(ReglaSaldoContrario.naturaleza(cuenta));
        s.setSaldo(Math.round(saldo * 100.0) / 100.0);
        s.setValorContrario(Math.round(contrario * 100.0) / 100.0);
        s.setTipo(ReglaSaldoContrario.tipo(cuenta));
        lista.add(s);
    }

    /** REQUIRES_NEW: si una consulta falla, solo cae esta transaccion y no la del semaforo ni la del candado que lo llama. */
    @Override
    @jakarta.ejb.TransactionAttribute(jakarta.ejb.TransactionAttributeType.REQUIRES_NEW)
    public List<ControlCierre> controlesCierre(Long idEmpresa, Long anio, Long mes) {
        ControlCierre c = new ControlCierre("SALDOS", "SALDO_CONTRARIO_CUENTA", "Cuentas con saldo contrario a su naturaleza (D-150 / D-222)");
        try {
            List<SaldoContrarioCuenta> lista = saldosContrarios(idEmpresa, anio, mes);
            double total = 0.0;
            for (SaldoContrarioCuenta s : lista) {
                total += s.getValorContrario();
            }
            c.setValor(Double.valueOf(lista.size()));
            c.setEsperado(Double.valueOf(0));
            c.setDiferencia(Double.valueOf(lista.size()));
            if (lista.isEmpty()) {
                c.setEstado(ControlCierre.OK);
                c.setDetalle("Ninguna cuenta de movimiento con saldo contrario al corte.");
            } else {
                c.setEstado(ControlCierre.ERROR);
                StringBuilder sb = new StringBuilder(lista.size() + " cuenta(s) con saldo contrario, suma " + String.format(Locale.US, "%.2f", total) + ": ");
                int n = 0;
                for (SaldoContrarioCuenta s : lista) {
                    if (n++ >= MAXIMO_LISTADAS) {
                        sb.append(" ... y ").append(lista.size() - MAXIMO_LISTADAS).append(" mas (GET /prdo/saldosContrarios/...).");
                        break;
                    }
                    sb.append(n > 1 ? "; " : "").append(s.getCuenta()).append(' ').append(s.getNombre()).append(" ").append(s.getTipo())
                            .append(" saldo ").append(String.format(Locale.US, "%.2f", s.getSaldo()))
                            .append(s.isRegularizableAutomatico() ? " [automatico: POST /prdo/regularizarRedondeos]"
                                    : (s.getAsientoSugerido() != null ? " [sugerido: " + s.getAsientoSugerido() + "]" : ""));
                }
                c.setDetalle(sb.toString());
            }
        } catch (Throwable e) {
            c.setEstado(ControlCierre.ADVERTENCIA);
            c.setDetalle("No se pudo medir el saldo contrario por cuenta: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
        List<ControlCierre> l = new ArrayList<ControlCierre>();
        l.add(c);
        return l;
    }

    // ------------------------------------------------------------------------------------------------ plantilla

    private Long idPlantilla(Long idEmpresa) throws Throwable {
        Long id = plantillaService.codigoByAlterno(PlantillasContabilidad.REGULARIZACION_SALDOS, idEmpresa);
        return id != null && id.longValue() != 0L ? id : null;
    }

    private DetallePlantilla linea(Long idPlantilla, int papel, int aux2) throws Throwable {
        DetallePlantilla d = detallePlantillaDaoService.selectByPlantillaYAuxiliares(idPlantilla, papel, aux2);
        return d != null && d.getPlanCuenta() != null && d.getPlanCuenta().getCuentaContable() != null ? d : null;
    }

    /** Cuentas de redondeo de la plantilla (papel 1, aux2 1..9); vacio si no hay plantilla. */
    private Set<String> cuentasDeRedondeo(Long idEmpresa) {
        Set<String> cuentas = new LinkedHashSet<String>();
        try {
            Long id = idPlantilla(idEmpresa);
            if (id != null) {
                for (int aux2 = 1; aux2 <= 9; aux2++) {
                    DetallePlantilla d = linea(id, PlantillasContabilidad.REDONDEO_ORIGEN, aux2);
                    if (d != null) {
                        cuentas.add(d.getPlanCuenta().getCuentaContable().trim());
                    }
                }
            }
        } catch (Throwable e) {
            System.out.println("C52: no se pudo leer la plantilla de redondeos: " + e.getMessage());
        }
        return cuentas;
    }

    /** Asiento sugerido (texto, no se graba): pasivo/patrimonio deudor contra el papel 3, activo acreedor contra el papel 4. */
    private String sugerencia(Long idEmpresa, SaldoContrarioCuenta s) {
        try {
            Long id = idPlantilla(idEmpresa);
            String valor = String.format(Locale.US, "%.2f", s.getValorContrario());
            char clase = ReglaSaldoContrario.clase(s.getCuenta());
            if (s.isRegularizableAutomatico()) {
                return "D " + s.getCuenta() + " / H otros ingresos por " + valor + " (automatico)";
            }
            if (id == null) {
                return null;
            }
            if (clase == '2' || clase == '3') {
                DetallePlantilla c = linea(id, PlantillasContabilidad.CONTRA_PASIVO_DEUDOR, 0);
                return c == null ? null : "D " + c.getPlanCuenta().getCuentaContable().trim() + " / H " + s.getCuenta() + " por " + valor
                        + " (reclasificar; con aprobacion y novedad C25)";
            }
            if (clase == '1') {
                DetallePlantilla c = linea(id, PlantillasContabilidad.CONTRA_ACTIVO_ACREEDOR, 0);
                return c == null ? null : "D " + s.getCuenta() + " / H " + c.getPlanCuenta().getCuentaContable().trim() + " por " + valor
                        + " (reclasificar; con aprobacion y novedad C25)";
            }
            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------ correccion automatica

    @Override
    public ResultadoRegularizacionRedondeos regularizarRedondeos(Long idEmpresa, Long anio, Long mes, String usuario, boolean simular) throws Throwable {
        System.out.println("SaldoContrarioCuentaService.regularizarRedondeos - empresa: " + idEmpresa + " - " + anio + "-" + mes + " - simular: " + simular);
        validaPeriodo(idEmpresa, anio, mes);
        YearMonth ym = YearMonth.of(anio.intValue(), mes.intValue());
        LocalDate corte = ym.atEndOfMonth();
        ResultadoRegularizacionRedondeos r = new ResultadoRegularizacionRedondeos();
        r.setIdEmpresa(idEmpresa);
        r.setAnio(anio);
        r.setMes(mes);
        r.setSimulado(simular);
        r.setFecha(corte.toString());

        Long id = idPlantilla(idEmpresa);
        if (id == null) {
            throw new IncomeException("No existe la plantilla contable con codigo alterno " + PlantillasContabilidad.REGULARIZACION_SALDOS
                    + " (regularizacion de saldos contrarios) para la empresa " + idEmpresa + ". Correr docs/logica-negocio/cnt/sql/415.");
        }
        DetallePlantilla destino = linea(id, PlantillasContabilidad.OTROS_INGRESOS_DESTINO, 0);
        if (destino == null) {
            throw new IncomeException("La plantilla " + PlantillasContabilidad.REGULARIZACION_SALDOS + " no define la cuenta de otros ingresos (papel "
                    + PlantillasContabilidad.OTROS_INGRESOS_DESTINO + ").");
        }
        List<DetallePlantilla> origenes = new ArrayList<DetallePlantilla>();
        Set<String> cuentasOrigen = new LinkedHashSet<String>();
        for (int aux2 = 1; aux2 <= 9; aux2++) {
            DetallePlantilla d = linea(id, PlantillasContabilidad.REDONDEO_ORIGEN, aux2);
            if (d != null) {
                origenes.add(d);
                cuentasOrigen.add(d.getPlanCuenta().getCuentaContable().trim());
            }
        }
        if (origenes.isEmpty()) {
            throw new IncomeException("La plantilla " + PlantillasContabilidad.REGULARIZACION_SALDOS + " no define cuentas de redondeo (papel "
                    + PlantillasContabilidad.REDONDEO_ORIGEN + ").");
        }

        String marca = MARCA_REGULARIZACION + anio + "-" + String.format("%02d", mes);
        List<Object> existente = em.createNativeQuery(
                " SELECT MIN(a.ASNTCDGO) FROM CNT.ASNT a WHERE a.PJRQCDGO = :empresa AND a.ASNTESTD IN (1, 3) AND a.ASNTOBSR LIKE :marca")
                .setParameter("empresa", idEmpresa).setParameter("marca", marca + "%").getResultList();
        if (!existente.isEmpty() && existente.get(0) != null) {
            r.setYaRegularizado(true);
            r.setIdAsiento(((Number) existente.get(0)).longValue());
            r.setMensaje("El mes " + anio + "-" + String.format("%02d", mes) + " ya tiene su asiento de regularizacion de redondeos (" + r.getIdAsiento() + "): no se repite.");
            return r;
        }

        // Saldo acreedor (acumulado del anio sin el cierre) de cada cuenta de redondeo: se lleva a otros ingresos.
        List<SaldoContrarioCuenta> contrarios = resultados(idEmpresa, anio, corte.plusDays(1), cuentasOrigen);
        List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();
        double total = 0.0;
        for (DetallePlantilla origen : origenes) {
            String cuenta = origen.getPlanCuenta().getCuentaContable().trim();
            for (SaldoContrarioCuenta s : contrarios) {
                if (s.getCuenta().equals(cuenta) && s.getSaldo() < 0) {
                    double valor = s.getValorContrario();
                    r.getLineas().add("D " + cuenta + " " + String.format(Locale.US, "%.2f", valor));
                    lineas.add(linea(origen, valor, 0.0, "Regularizacion de redondeo a otros ingresos " + anio + "-" + String.format("%02d", mes)));
                    total += valor;
                }
            }
        }
        total = Math.round(total * 100.0) / 100.0;
        r.setTotal(total);
        if (lineas.isEmpty()) {
            r.setMensaje("Ninguna cuenta de redondeo tiene saldo acreedor al " + corte + ": no hay nada que regularizar.");
            return r;
        }
        r.getLineas().add("H " + destino.getPlanCuenta().getCuentaContable().trim() + " " + String.format(Locale.US, "%.2f", total));
        lineas.add(linea(destino, 0.0, total, "Redondeos a otros ingresos " + anio + "-" + String.format("%02d", mes)));
        if (simular) {
            r.setMensaje("Simulacion: se asentaria este movimiento con fecha " + corte + ". No se grabo nada.");
            return r;
        }
        // Con el periodo contable cerrado no se graba (lanza IncomeException).
        estadoCierreService.validarContableAbierto(idEmpresa, corte, "la regularizacion de redondeos de " + anio + "-" + String.format("%02d", mes));
        String usuarioValido = usuarioValidacionService.validar(usuario);
        com.saa.model.cnt.Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.INGRESOS, corte,
                marca + ": redondeos con saldo acreedor a otros ingresos (regla del lider del 26-nov)", usuarioValido, lineas,
                Long.valueOf(ModuloSistema.CONTABILIDAD));
        r.setIdAsiento(asiento.getCodigo());
        r.setMensaje("Asiento " + asiento.getCodigo() + " registrado con fecha " + corte + ".");
        return r;
    }

    private static DetalleAsiento linea(DetallePlantilla d, double debe, double haber, String descripcion) {
        DetalleAsiento l = new DetalleAsiento();
        l.setPlanCuenta(d.getPlanCuenta());
        l.setNumeroCuenta(d.getPlanCuenta().getCuentaContable());
        l.setNombreCuenta(d.getPlanCuenta().getNombre());
        l.setDescripcion(descripcion);
        l.setValorDebe(debe);
        l.setValorHaber(haber);
        return l;
    }

    private static void validaPeriodo(Long idEmpresa, Long anio, Long mes) throws IncomeException {
        if (idEmpresa == null || anio == null || mes == null || mes < 1 || mes > 12) {
            throw new IncomeException("Empresa, año y mes (1 a 12) son obligatorios.");
        }
    }
}
