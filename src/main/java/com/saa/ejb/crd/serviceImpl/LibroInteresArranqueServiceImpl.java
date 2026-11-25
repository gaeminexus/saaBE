package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.crd.dao.DeclaracionPlazoVencidoDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.MovimientoInteresCuotaDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.LibroInteresArranqueService;
import com.saa.ejb.crd.service.ModoCorteCartera;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.TopeProvisionMora;
import com.saa.ejb.crd.service.dto.ResultadoAjusteLibroProvision;
import com.saa.ejb.crd.service.dto.ResultadoSemillaDevengoInteres;
import com.saa.ejb.crd.service.dto.SolicitudLibroInteresArranque;
import com.saa.model.cnt.Asiento;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.MovimientoInteresCuota;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.ComponenteMovimientoInteresCuota;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.TipoMovimientoInteresCuota;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Lote 21 (A2 y A1). Ver {@link LibroInteresArranqueService} y el doc de diseño de la provisión de intereses (Lote 21).
 */
@Stateless
public class LibroInteresArranqueServiceImpl implements LibroInteresArranqueService {

    private static final double TOLERANCIA = 0.005;
    public static final String ORIGEN_SEMILLA = "SEMILLA_DEVENGO_INTERES";
    public static final String ORIGEN_AJUSTE_LIBRO = "AJUSTE_LIBRO_PROVISION";

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    @EJB
    private MovimientoInteresCuotaDaoService movimientoInteresCuotaDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private DeclaracionPlazoVencidoDaoService declaracionPlazoVencidoDaoService;

    /** Una cuota del universo del ⑦ al corte con lo devengado y no cobrado. */
    private static final class FilaCuota {
        DetallePrestamo cuota;
        Prestamo prestamo;
        Long idTipoPrestamo;
        double interesPendiente;
        double moraPendiente;
    }

    // =====================================================================
    // A2 — semilla del libro de devengo de interés (MVIC 11)
    // =====================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoSemillaDevengoInteres sembrarDevengoInteres(SolicitudLibroInteresArranque solicitud) throws Throwable {
        System.out.println("Ingresa al metodo (sembrarDevengoInteres) LibroInteresArranqueService - referencia: "
            + (solicitud != null ? solicitud.getIdAsientoReferencia() : null) + " - corte: "
            + (solicitud != null ? solicitud.getCorte() : null) + " - grabar: " + (solicitud != null ? solicitud.getGrabar() : null));
        LocalDate corte = validaSolicitud(solicitud);
        String usuario = usuarioValidacionService.validar(solicitud.getUsuario());
        boolean grabar = Boolean.TRUE.equals(solicitud.getGrabar());
        Long referencia = solicitud.getIdAsientoReferencia();

        ResultadoSemillaDevengoInteres resultado = new ResultadoSemillaDevengoInteres();
        resultado.setCorte(corte.toString());
        resultado.setIdAsientoReferencia(referencia);

        // Idempotencia: la referencia ya tiene su semilla.
        List<MovimientoInteresCuota> existentes = movimientoInteresCuotaDaoService.selectVigentesPorOrigen(ORIGEN_SEMILLA, referencia,
            java.util.Collections.singletonList(TipoMovimientoInteresCuota.DEVENGO_INTERES));
        if (!existentes.isEmpty()) {
            Map<Long, double[]> porTipo = new TreeMap<>();
            double total = 0.0;
            for (MovimientoInteresCuota m : existentes) {
                double valor = m.getValor() != null ? m.getValor().doubleValue() : 0.0;
                double[] acumulado = porTipo.computeIfAbsent(claveTipo(idTipoDe(m.getPrestamo())), k -> new double[2]);
                acumulado[0] += 1;
                acumulado[1] += valor;
                total += valor;
            }
            llenaPorTipoSemilla(resultado, porTipo);
            resultado.setYaGrabado(true);
            resultado.setCuotasSembradas(existentes.size());
            resultado.setTotal(redondear(total));
            resultado.getAdvertencias().add("La referencia " + referencia + " ya tiene " + existentes.size()
                + " movimiento(s) tipo 11 vigentes: no se repite.");
            return resultado;
        }
        validaAsientoReferencia(referencia);

        List<FilaCuota> universo = universoAlCorte(corte, false);
        List<Long> ids = new ArrayList<>();
        for (FilaCuota f : universo) {
            ids.add(f.cuota.getCodigo());
        }
        // Las cuotas que el ④ nuevo ya alimentó (MVIC 11 o 12 vigente con fecha <= corte) no se siembran.
        Set<Long> yaAlimentadas = movimientoInteresCuotaDaoService.selectCuotasConMovimientoHasta(ids,
            java.util.Arrays.asList(TipoMovimientoInteresCuota.DEVENGO_INTERES, TipoMovimientoInteresCuota.REVERSO_DEVENGO_INTERES), corte);

        Map<Long, double[]> porTipo = new TreeMap<>();
        List<FilaCuota> sembrar = new ArrayList<>();
        double total = 0.0;
        for (FilaCuota f : universo) {
            if (f.interesPendiente <= TOLERANCIA) {
                continue;
            }
            if (yaAlimentadas.contains(f.cuota.getCodigo())) {
                resultado.setCuotasYaAlimentadas(resultado.getCuotasYaAlimentadas() + 1);
                continue;
            }
            sembrar.add(f);
            double[] acumulado = porTipo.computeIfAbsent(f.idTipoPrestamo, k -> new double[2]);
            acumulado[0] += 1;
            acumulado[1] += f.interesPendiente;
            total += f.interesPendiente;
        }
        llenaPorTipoSemilla(resultado, porTipo);
        resultado.setCuotasSembradas(sembrar.size());
        resultado.setTotal(redondear(total));
        if (sembrar.isEmpty()) {
            resultado.getAdvertencias().add("No hay interés devengado y no cobrado por sembrar al " + corte + ".");
        }
        if (resultado.getCuotasYaAlimentadas() > 0) {
            resultado.getAdvertencias().add(resultado.getCuotasYaAlimentadas() + " cuota(s) ya tienen MVIC 11 o 12 con fecha <= "
                + corte + " (las alimentó el ④ nuevo): no se siembran.");
        }

        if (grabar && !sembrar.isEmpty()) {
            LocalDateTime ahora = LocalDateTime.now();
            for (FilaCuota f : sembrar) {
                MovimientoInteresCuota mov = new MovimientoInteresCuota();
                Prestamo p = new Prestamo();
                p.setCodigo(f.prestamo.getCodigo());
                mov.setPrestamo(p);
                mov.setIdCuota(f.cuota.getCodigo());
                mov.setTipoMovimiento(TipoMovimientoInteresCuota.DEVENGO_INTERES);
                mov.setComponente(ComponenteMovimientoInteresCuota.INTERES);
                mov.setValor(redondear(f.interesPendiente));
                mov.setFechaContable(corte);
                mov.setOrigen(ORIGEN_SEMILLA);
                mov.setIdOrigen(referencia);
                mov.setAsiento(referencia);
                mov.setAnulado(0L);
                mov.setUsuarioRegistro(usuario);
                mov.setFechaRegistro(ahora);
                movimientoInteresCuotaDaoService.save(mov, null);
            }
            resultado.setGrabado(true);
        }
        return resultado;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int anularSemilla(Long idAsientoReferencia, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo (anularSemilla) LibroInteresArranqueService - referencia: " + idAsientoReferencia);
        if (idAsientoReferencia == null) {
            throw new IncomeException("idAsientoReferencia es obligatorio.");
        }
        usuarioValidacionService.validar(usuario);
        return movimientoInteresCuotaDaoService.anularByOrigenYIdOrigen(ORIGEN_SEMILLA, idAsientoReferencia);
    }

    // =====================================================================
    // A1 — ajuste del libro de provisión por cuota (MVIC 13 baja, 14 alta)
    // =====================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoAjusteLibroProvision ajustarLibroProvision(SolicitudLibroInteresArranque solicitud) throws Throwable {
        System.out.println("Ingresa al metodo (ajustarLibroProvision) LibroInteresArranqueService - referencia: "
            + (solicitud != null ? solicitud.getIdAsientoReferencia() : null) + " - corte: "
            + (solicitud != null ? solicitud.getCorte() : null) + " - grabar: " + (solicitud != null ? solicitud.getGrabar() : null));
        LocalDate corte = validaSolicitud(solicitud);
        String usuario = usuarioValidacionService.validar(solicitud.getUsuario());
        boolean grabar = Boolean.TRUE.equals(solicitud.getGrabar());
        Long referencia = solicitud.getIdAsientoReferencia();

        ResultadoAjusteLibroProvision resultado = new ResultadoAjusteLibroProvision();
        resultado.setCorte(corte.toString());
        resultado.setIdAsientoReferencia(referencia);

        List<MovimientoInteresCuota> existentes = movimientoInteresCuotaDaoService.selectVigentesPorOrigen(ORIGEN_AJUSTE_LIBRO, referencia,
            java.util.Arrays.asList(TipoMovimientoInteresCuota.AJUSTE_LIBRO_PROVISION_BAJA,
                TipoMovimientoInteresCuota.AJUSTE_LIBRO_PROVISION_ALTA));
        if (!existentes.isEmpty()) {
            Map<Long, ResultadoAjusteLibroProvision.Tipo> porTipo = new TreeMap<>();
            for (MovimientoInteresCuota m : existentes) {
                boolean baja = m.getTipoMovimiento() != null
                    && m.getTipoMovimiento().longValue() == TipoMovimientoInteresCuota.AJUSTE_LIBRO_PROVISION_BAJA;
                boolean interes = m.getComponente() != null && m.getComponente().longValue() == ComponenteMovimientoInteresCuota.INTERES;
                acumula(porTipo, claveTipo(idTipoDe(m.getPrestamo())), interes, baja, m.getValor() != null ? m.getValor().doubleValue() : 0.0);
            }
            cierraResultado(resultado, porTipo);
            resultado.setYaGrabado(true);
            resultado.setCuotasAjustadas(existentes.size());
            resultado.getAdvertencias().add("La referencia " + referencia + " ya tiene " + existentes.size()
                + " ajuste(s) del libro vigentes: no se repite (deCuotasCerradas y deNegativas no se reconstruyen).");
            return resultado;
        }
        validaAsientoReferencia(referencia);

        // Universo: las cuotas del ⑦ al corte (con su interés y su mora pendientes) más las que tienen saldo de provisión y ya no lo son.
        List<FilaCuota> universo = universoAlCorte(corte, true);
        Map<Long, FilaCuota> porCuota = new LinkedHashMap<>();
        for (FilaCuota f : universo) {
            porCuota.put(f.cuota.getCodigo(), f);
        }
        List<Object[]> saldos = movimientoInteresCuotaDaoService.selectSaldosProvisionNoCeroAlCorte(corte);
        Map<Long, double[]> saldoPorCuota = new HashMap<>();
        Map<Long, Long> prestamoPorCuota = new HashMap<>();
        for (Object[] fila : saldos) {
            Long idCuota = ((Number) fila[0]).longValue();
            prestamoPorCuota.put(idCuota, ((Number) fila[1]).longValue());
            double[] acumulado = saldoPorCuota.computeIfAbsent(idCuota, k -> new double[2]);
            if (((Number) fila[2]).longValue() == ComponenteMovimientoInteresCuota.INTERES) {
                acumulado[0] = ((Number) fila[3]).doubleValue();
            } else {
                acumulado[1] = ((Number) fila[3]).doubleValue();
            }
        }
        for (Long idCuota : saldoPorCuota.keySet()) {
            if (!porCuota.containsKey(idCuota)) {
                porCuota.put(idCuota, null);
            }
        }

        Map<Long, Prestamo> prestamos = new HashMap<>();
        Map<Long, ResultadoAjusteLibroProvision.Tipo> porTipo = new TreeMap<>();
        List<Object[]> filas = new ArrayList<>(); // {idCuota, idPrestamo, componente, baja(Boolean), valor}
        double deCerradas = 0.0;
        double deNegativas = 0.0;
        for (Map.Entry<Long, FilaCuota> entrada : porCuota.entrySet()) {
            Long idCuota = entrada.getKey();
            FilaCuota f = entrada.getValue();
            double[] saldo = saldoPorCuota.getOrDefault(idCuota, new double[2]);
            boolean enUniverso = f != null;
            double objetivoInteres = enUniverso ? f.interesPendiente : 0.0;
            double objetivoMora = enUniverso ? f.moraPendiente : 0.0;
            Long idPrestamo = enUniverso ? f.prestamo.getCodigo() : prestamoPorCuota.get(idCuota);
            Prestamo prestamo = enUniverso ? f.prestamo : prestamos.computeIfAbsent(idPrestamo, k -> {
                try {
                    return prestamoDaoService.find(new Prestamo(), k);
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            });
            Long idTipo = idTipoDe(prestamo);
            if (idTipo == null) {
                throw new IncomeException("La cuota " + idCuota + " (préstamo " + idPrestamo + ") tiene provisión o deuda al corte pero su producto no"
                    + " tiene tipo de préstamo: no se puede agrupar por tipo.");
            }
            for (int componente = 0; componente < 2; componente++) {
                double actual = saldo[componente];
                double objetivo = componente == 0 ? objetivoInteres : objetivoMora;
                double delta = redondear(objetivo - actual);
                if (Math.abs(delta) < TOLERANCIA) {
                    continue;
                }
                boolean baja = delta < 0;
                double valor = Math.abs(delta);
                if (baja && !enUniverso) {
                    deCerradas += valor;
                }
                if (!baja && actual < -TOLERANCIA) {
                    deNegativas += Math.min(valor, -actual);
                }
                filas.add(new Object[]{ idCuota, idPrestamo, Long.valueOf(componente == 0
                    ? ComponenteMovimientoInteresCuota.INTERES : ComponenteMovimientoInteresCuota.MORA), Boolean.valueOf(baja), valor });
                acumula(porTipo, idTipo, componente == 0, baja, valor);
            }
        }
        cierraResultado(resultado, porTipo);
        resultado.setCuotasAjustadas(filas.size());
        resultado.setDeCuotasCerradas(redondear(deCerradas));
        resultado.setDeNegativas(redondear(deNegativas));
        if (filas.isEmpty()) {
            resultado.getAdvertencias().add("El libro de provisión ya está al día al " + corte + ": nada que ajustar.");
        }

        if (grabar && !filas.isEmpty()) {
            LocalDateTime ahora = LocalDateTime.now();
            for (Object[] fila : filas) {
                MovimientoInteresCuota mov = new MovimientoInteresCuota();
                Prestamo p = new Prestamo();
                p.setCodigo((Long) fila[1]);
                mov.setPrestamo(p);
                mov.setIdCuota((Long) fila[0]);
                mov.setTipoMovimiento(Boolean.TRUE.equals(fila[3]) ? TipoMovimientoInteresCuota.AJUSTE_LIBRO_PROVISION_BAJA
                    : TipoMovimientoInteresCuota.AJUSTE_LIBRO_PROVISION_ALTA);
                mov.setComponente((Long) fila[2]);
                mov.setValor(redondear((Double) fila[4]));
                mov.setFechaContable(corte);
                mov.setOrigen(ORIGEN_AJUSTE_LIBRO);
                mov.setIdOrigen(referencia);
                mov.setAsiento(referencia);
                mov.setAnulado(0L);
                mov.setUsuarioRegistro(usuario);
                mov.setFechaRegistro(ahora);
                movimientoInteresCuotaDaoService.save(mov, null);
            }
            resultado.setGrabado(true);
        }
        return resultado;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int anularAjusteLibro(Long idAsientoReferencia, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo (anularAjusteLibro) LibroInteresArranqueService - referencia: " + idAsientoReferencia);
        if (idAsientoReferencia == null) {
            throw new IncomeException("idAsientoReferencia es obligatorio.");
        }
        usuarioValidacionService.validar(usuario);
        return movimientoInteresCuotaDaoService.anularByOrigenYIdOrigen(ORIGEN_AJUSTE_LIBRO, idAsientoReferencia);
    }

    // =====================================================================
    // Cálculo común: lo devengado y no cobrado al corte, por cuota
    // =====================================================================

    /**
     * Las cuotas que ve el ⑦ al corte (vencidas impagas y la que vence el día del corte y no se pagó), sin las cerradas (estado 7) ni las
     * anuladas. {@code interesPendiente} es la MISMA cuenta del ⑦: interés + interés vencido de la cuota − interés pagado − interés vencido pagado
     * (pagos con fecha efectiva ≤ corte), con piso en 0 y en 0 si el interés está condonado por plazo vencido. {@code moraPendiente} (solo con
     * {@code conMora}) es el objetivo del ⑦ para la mora: la mora contabilizada pendiente del préstamo repartida entre sus cuotas si el tope de la
     * mora está activo al corte, y la mora calculada pendiente si no.
     */
    private List<FilaCuota> universoAlCorte(LocalDate corte, boolean conMora) throws Throwable {
        List<DetallePrestamo> cuotas = new ArrayList<>();
        for (DetallePrestamo cuota : detallePrestamoDaoService.selectCuotasProvisionablesAlCorte(corte)) {
            long estado = cuota.getEstado() != null ? cuota.getEstado().longValue() : 0L;
            if (estado == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA || estado == EstadoCuotaPrestamo.ANULADA) {
                continue;
            }
            cuotas.add(cuota);
        }
        List<Long> ids = new ArrayList<>();
        List<Long> idsPrestamoEn8 = new ArrayList<>();
        for (DetallePrestamo cuota : cuotas) {
            ids.add(cuota.getCodigo());
            if (cuota.getPrestamo() != null && cuota.getPrestamo().getIdEstado() != null
                    && cuota.getPrestamo().getIdEstado() == EstadoPrestamo.DE_PLAZO_VENCIDO) {
                idsPrestamoEn8.add(cuota.getPrestamo().getCodigo());
            }
        }
        // Pagos al corte por cuota: [1] mora, [2] interés vencido pagado, [3] interés pagado (mismo orden que el ⑦).
        Map<Long, double[]> pagos = new HashMap<>();
        for (Object[] fila : pagoPrestamoDaoService.selectDatosPagosVigentesAlCorte(ids, corte)) {
            double[] acumulado = pagos.computeIfAbsent((Long) fila[0], k -> new double[4]);
            acumulado[1] += nvl(fila[2]);
            acumulado[2] += nvl(fila[3]);
            acumulado[3] += nvl(fila[4]);
        }
        Map<Long, LocalDate> declaracion = declaracionPlazoVencidoDaoService.selectFechaCorteVivaByPrestamos(idsPrestamoEn8);

        Map<Long, Double> moraObjetivo = new HashMap<>();
        if (conMora) {
            boolean conTope = ModoCorteCartera.topeProvisionMoraActivo(corte);
            Map<Long, Double> calculada = new HashMap<>();
            List<TopeProvisionMora.CuotaTope> candidatas = new ArrayList<>();
            for (DetallePrestamo cuota : cuotas) {
                double[] pg = pagos.get(cuota.getCodigo());
                double tasa = procesoMoraPrestamoService.tasaDiariaDelPrestamo(cuota.getPrestamo(), false);
                double pendiente = Math.max(0.0, redondear(procesoMoraPrestamoService.calcularMoraCuota(cuota, tasa, corte)
                    - (pg != null ? pg[1] : 0.0)));
                calculada.put(cuota.getCodigo(), pendiente);
                candidatas.add(new TopeProvisionMora.CuotaTope(cuota.getCodigo(),
                    cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null,
                    cuota.getFechaVencimiento() != null ? cuota.getFechaVencimiento().toLocalDate() : null, pendiente));
            }
            if (conTope) {
                moraObjetivo.putAll(TopeProvisionMora.repartir(
                    movimientoInteresCuotaDaoService.selectMoraContabilizadaPendientePorPrestamo(corte), candidatas));
            } else {
                moraObjetivo.putAll(calculada);
            }
        }

        List<FilaCuota> filas = new ArrayList<>();
        for (DetallePrestamo cuota : cuotas) {
            Prestamo prestamo = cuota.getPrestamo();
            double[] pg = pagos.get(cuota.getCodigo());
            boolean condonado = false;
            if (prestamo != null && prestamo.getIdEstado() != null && prestamo.getIdEstado() == EstadoPrestamo.DE_PLAZO_VENCIDO
                    && cuota.getFechaVencimiento() != null) {
                LocalDate fechaDeclaracion = declaracion.get(prestamo.getCodigo());
                condonado = fechaDeclaracion != null && !fechaDeclaracion.isAfter(corte)
                    && cuota.getFechaVencimiento().toLocalDate().isAfter(fechaDeclaracion);
            }
            FilaCuota f = new FilaCuota();
            f.cuota = cuota;
            f.prestamo = prestamo;
            f.idTipoPrestamo = idTipoDe(prestamo);
            f.interesPendiente = condonado ? 0.0 : Math.max(0.0, redondear(nvl(cuota.getInteres()) + nvl(cuota.getInteresVencido())
                - (pg != null ? pg[3] : 0.0) - (pg != null ? pg[2] : 0.0)));
            f.moraPendiente = conMora ? moraObjetivo.getOrDefault(cuota.getCodigo(), 0.0) : 0.0;
            if (f.idTipoPrestamo == null && (f.interesPendiente > TOLERANCIA || f.moraPendiente > TOLERANCIA)) {
                throw new IncomeException("El préstamo " + (prestamo != null ? prestamo.getCodigo() : null) + " (cuota " + cuota.getCodigo()
                    + ") tiene interés o mora pendiente pero su producto no tiene tipo de préstamo: no se puede agrupar por tipo.");
            }
            filas.add(f);
        }
        return filas;
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    private LocalDate validaSolicitud(SolicitudLibroInteresArranque solicitud) throws Throwable {
        if (solicitud == null || solicitud.getIdEmpresa() == null || solicitud.getIdAsientoReferencia() == null
                || solicitud.getCorte() == null || solicitud.getCorte().trim().isEmpty()) {
            throw new IncomeException("idEmpresa, corte (AAAA-MM-DD) e idAsientoReferencia son obligatorios.");
        }
        try {
            return LocalDate.parse(solicitud.getCorte().trim());
        } catch (RuntimeException e) {
            throw new IncomeException("El corte '" + solicitud.getCorte() + "' no es una fecha AAAA-MM-DD.");
        }
    }

    /** El asiento de referencia tiene que existir y estar ACTIVO. No se exige que su fecha sea el corte: el del cuadre puede ir fechado al día siguiente. */
    private void validaAsientoReferencia(Long idAsiento) throws Throwable {
        Asiento asiento = asientoService.selectById(idAsiento);
        if (asiento == null) {
            throw new IncomeException("No existe el asiento de referencia " + idAsiento + ".");
        }
        if (asiento.getEstado() == null || asiento.getEstado().longValue() != EstadoAsiento.ACTIVO) {
            throw new IncomeException("El asiento de referencia " + idAsiento + " no está ACTIVO (estado " + asiento.getEstado() + ").");
        }
    }

    /** Clave de agrupación para un préstamo sin tipo (solo al resumir lo ya grabado). */
    private Long claveTipo(Long idTipo) {
        return idTipo != null ? idTipo : Long.valueOf(0L);
    }

    private Long idTipoDe(Prestamo prestamo) {
        return prestamo != null && prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
            ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
    }

    private void acumula(Map<Long, ResultadoAjusteLibroProvision.Tipo> porTipo, Long idTipo, boolean interes, boolean baja, double valor) {
        ResultadoAjusteLibroProvision.Tipo t = porTipo.get(idTipo);
        if (t == null) {
            t = new ResultadoAjusteLibroProvision.Tipo();
            t.setIdTipoPrestamo(idTipo);
            porTipo.put(idTipo, t);
        }
        if (interes) {
            if (baja) {
                t.setBajaInteres(t.getBajaInteres() + valor);
            } else {
                t.setAltaInteres(t.getAltaInteres() + valor);
            }
        } else if (baja) {
            t.setBajaMora(t.getBajaMora() + valor);
        } else {
            t.setAltaMora(t.getAltaMora() + valor);
        }
    }

    private void cierraResultado(ResultadoAjusteLibroProvision resultado, Map<Long, ResultadoAjusteLibroProvision.Tipo> porTipo) {
        double baja = 0.0;
        double alta = 0.0;
        for (ResultadoAjusteLibroProvision.Tipo t : porTipo.values()) {
            t.setBajaInteres(redondear(t.getBajaInteres()));
            t.setAltaInteres(redondear(t.getAltaInteres()));
            t.setBajaMora(redondear(t.getBajaMora()));
            t.setAltaMora(redondear(t.getAltaMora()));
            t.setNetoInteres(redondear(t.getAltaInteres() - t.getBajaInteres()));
            t.setNetoMora(redondear(t.getAltaMora() - t.getBajaMora()));
            baja += t.getBajaInteres() + t.getBajaMora();
            alta += t.getAltaInteres() + t.getAltaMora();
            resultado.getPorTipo().add(t);
        }
        resultado.setTotalBaja(redondear(baja));
        resultado.setTotalAlta(redondear(alta));
    }

    private void llenaPorTipoSemilla(ResultadoSemillaDevengoInteres resultado, Map<Long, double[]> porTipo) {
        for (Map.Entry<Long, double[]> e : porTipo.entrySet()) {
            ResultadoSemillaDevengoInteres.Tipo t = new ResultadoSemillaDevengoInteres.Tipo();
            t.setIdTipoPrestamo(e.getKey());
            t.setCuotas((int) e.getValue()[0]);
            t.setValor(redondear(e.getValue()[1]));
            resultado.getPorTipo().add(t);
        }
    }

    private static double nvl(Object valor) {
        return valor instanceof Number ? ((Number) valor).doubleValue() : 0.0;
    }

    private static double redondear(double valor) {
        return java.math.BigDecimal.valueOf(valor).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
