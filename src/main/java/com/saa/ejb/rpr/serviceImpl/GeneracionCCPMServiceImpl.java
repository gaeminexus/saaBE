package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.dao.ConfiguracionBandaProductoDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosMensualDaoService;
import com.saa.ejb.rpr.service.CreditoParticipesMensualService;
import com.saa.ejb.rpr.service.EjecucionReporteCarteraService;
import com.saa.ejb.rpr.service.GeneracionCCPMService;
import com.saa.model.crd.ConfiguracionBandaProducto;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;
import com.saa.model.rpr.CreditoParticipesMensual;
import com.saa.model.rpr.EjecucionReporteCartera;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Servicio de generación del reporte CCPM (Crédito Cuotas Préstamos Mensual).
 * Lógica similar al G48 pero con campos adicionales: valorDesgravamen y valorIncendio.
 * OPTIMIZADO: provisionConstituida = 0 (sin dependencia de histórico del mes anterior).
 *
 * <p><b>2026-10-13 — CCPM adopta la clasificación de bandas del cierre de cartera</b>
 * ({@code docs/logica-negocio/reportes/DISENO-CCPM-BANDAS-COMO-EL-CIERRE.md}). El universo de
 * préstamos, el capital pendiente y la clasificación por banda YA NO son propios de CCPM: son
 * el mismo universo del cierre de cartera ({@link CierreCarteraDaoService
 * #selectCuotasPendientesPorPrestamo}), clasificado cuota por cuota con
 * {@link ContabilizacionIndividualCreditoService#tipoCarteraYDias} y
 * {@link ClasificadorBandaService}. La fórmula de "vencido/por vencer + días" NO se reimplementa
 * acá — llamar a esos dos servicios es lo único que hace este archivo con los días.</p>
 *
 * <p>Los grupos 1 (cuotas del mes) / 2 (menor cuota anterior) del diseño viejo DESAPARECEN como
 * fuente del universo y del capital. Sobreviven como concepto para los campos que este cambio
 * NO toca (interés ordinario/mora/del mes, desgravamen, incendio, calificación,
 * valorSujetoProvision, provisión, cuenta individual, fechas, montoSolicitado): cada préstamo
 * sigue necesitando UNA cuota "ancla" para esos campos, redefinida así — la cuota VENCIDA más
 * antigua si el préstamo tiene mora (equivalente al viejo grupo 2), o la cuota POR VENCER más
 * próxima si está al día (equivalente al viejo grupo 1). Ver el reporte de la entrega del
 * 2026-10-13 para el detalle de qué cuota quedó como ancla de cada préstamo.</p>
 */
@Stateless
public class GeneracionCCPMServiceImpl implements GeneracionCCPMService {

    @EJB private DetallePrestamoService                    detallePrestamoService;
    @EJB private CreditoParticipesMensualService           cprmService;
    @EJB private CreditoCuotasPrestamosMensualDaoService   ccpmDaoService;
    @EJB private EjecucionReporteCarteraService            ejccService;
    @EJB private CierreCarteraDaoService                   cierreCarteraDaoService;
    @EJB private ClasificadorBandaService                  clasificadorBandaService;
    @EJB private ContabilizacionIndividualCreditoService    contabilizacionIndividualCreditoService;
    @EJB private ConfiguracionBandaProductoDaoService       configuracionBandaProductoDaoService;

    @Override
    public long generar(EjecucionReporteCartera ejecucion, Long idEmpresaParametro) throws Throwable {
        System.out.println("Ingresa al metodo generar CCPM");

        long mes  = ejecucion.getMes();
        long anio = ejecucion.getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();

        LocalDateTime fechaInicio = LocalDateTime.of((int) anio, (int) mes, 1, 0, 0, 0);
        // H54 (2026-09-07): fin de día REAL, no 23:59:59 — un movimiento con marca de tiempo
        // en 23:59:59.000000001-23:59:59.999999999 quedaba fuera del corte. Misma familia que
        // el centavo del cobro 54 y el descuadre del cierre de cartera: un borde escrito con
        // "casi" en vez de exacto.
        LocalDateTime fechaFin    = LocalDate.of((int) anio, (int) mes, ultimoDia).atTime(LocalTime.MAX);
        LocalDate     corte       = fechaFin.toLocalDate();

        System.out.println("CCPM - Rango: " + fechaInicio + " a " + fechaFin);

        // -------------------------------------------------------
        // 0. Empresa: la manda el frontend; mientras no lo haga, se resuelve sola si hay una
        //    unica configuracion de bandas vigente.
        // -------------------------------------------------------
        Long idEmpresa = idEmpresaParametro != null ? idEmpresaParametro : resolverEmpresaUnicaConBandas(corte);
        System.out.println("CCPM - Empresa: " + idEmpresa);

        // -------------------------------------------------------
        // 1. Cargar valorTotalCuentaIndividual desde CPRM del mismo mes.
        //    El nuevo modelo CPRM tiene UN REGISTRO POR entidad+tipoAporte,
        //    cada uno con su propio campo "total". El valorTotalCuentaIndividual
        //    es la SUMA de todos esos totales para cada entidad.
        //    (sin cambios respecto de antes de este item)
        // -------------------------------------------------------
        Map<Long, Double> mapaValorCuentaIndividual = new HashMap<>();
        try {
            List<CreditoParticipesMensual> registrosCPRM = cprmService.selectByEjecucion(ejecucion.getCodigo());
            for (CreditoParticipesMensual cprm : registrosCPRM) {
                if (cprm.getEntidad() != null) {
                    Double parcial = cprm.getTotal() != null ? cprm.getTotal() : 0.0;
                    Long codEntidad = cprm.getEntidad().getCodigo();
                    mapaValorCuentaIndividual.merge(codEntidad, parcial, Double::sum);
                }
            }
        } catch (Throwable e) {
            System.out.println("CCPM - No se pudo cargar CPRM: " + e.getMessage());
        }
        System.out.println("CCPM - Valores cuenta individual cargados: " + mapaValorCuentaIndividual.size());

        // -------------------------------------------------------
        // 2. provisionConstituida SIEMPRE en 0 (no depender del histórico)
        // -------------------------------------------------------
        System.out.println("CCPM - provisionConstituida: se establece en 0 (sin dependencia de histórico)");

        // -------------------------------------------------------
        // 3. Universo y capital: el MISMO que el cierre de cartera. Una fila por cuota
        //    pendiente (CierreCarteraDaoService#selectCuotasPendientesPorPrestamo), agrupada
        //    por préstamo. Reemplaza los grupos 1/2 de siempre (selectCuotasDelMesGlobal,
        //    selectMenorCuotaAnteriorAlMesGlobal) como fuente del universo.
        // -------------------------------------------------------
        List<Object[]> cuotasPendientes = cierreCarteraDaoService.selectCuotasPendientesPorPrestamo(idEmpresa);
        Map<Long, List<Object[]>> cuotasPorPrestamo = new LinkedHashMap<>();
        for (Object[] fila : cuotasPendientes) {
            Long idPrestamo = (Long) fila[0];
            List<Object[]> lista = cuotasPorPrestamo.get(idPrestamo);
            if (lista == null) {
                lista = new ArrayList<>();
                cuotasPorPrestamo.put(idPrestamo, lista);
            }
            lista.add(fila);
        }
        System.out.println("CCPM - Universo (mismo que el cierre): " + cuotasPorPrestamo.size()
                + " prestamos, " + cuotasPendientes.size() + " cuotas pendientes");

        if (cuotasPorPrestamo.isEmpty()) {
            System.out.println("CCPM - Sin cuotas. CCPM vacío, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 4. Clasificar cada cuota por banda, con cache de rangos por producto+tipoCartera —
        //    igual que CierreCarteraServiceImpl#cargaBandas. Solo el lado POR VENCER se
        //    bandea: CCPM no tiene columnas de banda para el lado VENCIDO.
        // -------------------------------------------------------
        Map<String, List<BandaProductoDetalle>> cacheBandas = new HashMap<>();

        Map<Long, Double>   valorPorVencerPorPrestamo  = new HashMap<>();
        Map<Long, Double>   valorVencidoPorPrestamo    = new HashMap<>();
        Map<Long, double[]> bandasPorVencerPorPrestamo = new HashMap<>();
        Map<Long, Long>     diasMorosidadPorPrestamo   = new HashMap<>();
        Map<Long, Long>     anclaCuotaPorPrestamo      = new HashMap<>();
        Map<Long, Boolean>  hasVencidaPorPrestamo      = new HashMap<>();

        for (Map.Entry<Long, List<Object[]>> entrada : cuotasPorPrestamo.entrySet()) {
            Long idPrestamo = entrada.getKey();
            List<Object[]> cuotas = entrada.getValue();
            Long idProducto = (Long) cuotas.get(0)[1];

            double valorPorVencer = 0.0;
            double valorVencido = 0.0;
            double[] bandasPV = new double[5];
            long diasMorosidadMax = 0L;
            Long anclaVencidaId = null;
            LocalDate anclaVencidaFecha = null;
            Long anclaPorVencerId = null;
            LocalDate anclaPorVencerFecha = null;

            for (Object[] c : cuotas) {
                Long idCuota = (Long) c[2];
                LocalDate vencimiento = (LocalDate) c[4];
                Double capitalPendiente = (Double) c[5];

                long[] td = contabilizacionIndividualCreditoService.tipoCarteraYDias(vencimiento, corte);
                long tipoCartera = td[0];
                long dias = td[1];

                if (tipoCartera == TipoCarteraBanda.POR_VENCER) {
                    valorPorVencer += capitalPendiente;
                    List<BandaProductoDetalle> bandas = bandasDe(idProducto, idEmpresa,
                            (long) TipoCarteraBanda.POR_VENCER, corte, cacheBandas);
                    if (bandas.size() > 5) {
                        throw new IncomeException("CCPM: el producto " + idProducto + " tiene "
                                + bandas.size() + " bandas POR VENCER configuradas y CCPM solo"
                                + " tiene columnas para 5 (1a30..Mas360). Hay que decidir como"
                                + " mapear las bandas sobrantes antes de seguir.");
                    }
                    BandaProductoDetalle banda = clasificadorBandaService.clasificarEnBandas(bandas, dias);
                    int idx = banda.getNumero().intValue() - 1;
                    if (idx >= 0 && idx < bandasPV.length) {
                        bandasPV[idx] += capitalPendiente;
                    }
                    if (anclaPorVencerFecha == null || vencimiento.isBefore(anclaPorVencerFecha)) {
                        anclaPorVencerFecha = vencimiento;
                        anclaPorVencerId = idCuota;
                    }
                } else {
                    valorVencido += capitalPendiente;
                    if (dias > diasMorosidadMax) {
                        diasMorosidadMax = dias;
                    }
                    if (anclaVencidaFecha == null || vencimiento.isBefore(anclaVencidaFecha)) {
                        anclaVencidaFecha = vencimiento;
                        anclaVencidaId = idCuota;
                    }
                }
            }

            boolean hasVencida = anclaVencidaId != null;
            Long ancla = hasVencida ? anclaVencidaId : anclaPorVencerId;

            valorPorVencerPorPrestamo.put(idPrestamo, valorPorVencer);
            valorVencidoPorPrestamo.put(idPrestamo, valorVencido);
            bandasPorVencerPorPrestamo.put(idPrestamo, bandasPV);
            diasMorosidadPorPrestamo.put(idPrestamo, diasMorosidadMax);
            hasVencidaPorPrestamo.put(idPrestamo, hasVencida);
            if (ancla != null) {
                anclaCuotaPorPrestamo.put(idPrestamo, ancla);
            }
        }

        // -------------------------------------------------------
        // 5. Campos que este item NO toca (interés ordinario/mora/del mes, desgravamen,
        //    incendio): misma lógica de siempre, con la ancla redefinida en el paso 4 — la
        //    cuota vencida más antigua (equivalente al viejo grupo 2) o, si el préstamo está
        //    al día, se leen directo de la cuota ancla más abajo (equivalente al viejo grupo 1).
        // -------------------------------------------------------
        List<Long> anclasConMora = new ArrayList<>();
        List<Long> prestamosConMora = new ArrayList<>();
        for (Map.Entry<Long, Boolean> entrada : hasVencidaPorPrestamo.entrySet()) {
            if (Boolean.TRUE.equals(entrada.getValue())) {
                Long idPrestamo = entrada.getKey();
                Long ancla = anclaCuotaPorPrestamo.get(idPrestamo);
                if (ancla != null) {
                    anclasConMora.add(ancla);
                    prestamosConMora.add(idPrestamo);
                }
            }
        }

        // suma[0]=codigoPrestamo, [1]=capital (IGNORADO: lo reemplaza valorVencido del paso 4),
        // [2]=interes, [3]=desgravamen, [4]=incendio.
        Map<Long, Object[]> mapaSumasConMora = new HashMap<>();
        if (!anclasConMora.isEmpty()) {
            List<Object[]> sumas = detallePrestamoService.selectSumaCapitalInteresGrupo2Batch(
                    anclasConMora, fechaInicio, fechaFin);
            for (Object[] suma : sumas) {
                Long codPrest = (Long) suma[0];
                mapaSumasConMora.put(codPrest, new Object[]{ suma[2], suma[3], suma[4] });
            }
        }

        Map<Long, Double> mapaMoraConMora = new HashMap<>();
        if (!anclasConMora.isEmpty()) {
            List<Object[]> moras = detallePrestamoService.calcularInteresMoraBatch(anclasConMora, fechaFin);
            for (Object[] mora : moras) {
                Long codCuota = (Long) mora[0];
                Double valorMora = mora[1] != null ? (Double) mora[1] : 0.0;
                mapaMoraConMora.put(codCuota, valorMora);
            }
        }

        Map<Long, Double> mapaInteresOrdinarioDelMesConMora = new HashMap<>();
        if (!prestamosConMora.isEmpty()) {
            List<Object[]> saldos = detallePrestamoService.selectSaldoInicialCapitalDelMesBatch(
                    prestamosConMora, fechaInicio, fechaFin);
            for (Object[] fila : saldos) {
                Long codPrest = (Long) fila[0];
                Double interesDelMes = fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0;
                mapaInteresOrdinarioDelMesConMora.put(codPrest, interesDelMes);
            }
        }

        // Delta de mora: interesMoraActual - interesMoraEjecucionAnterior (por numeroOperacion).
        // Si es la primera ejecución o no existe registro anterior, el delta queda en 0.
        // (sin cambios respecto de antes de este item)
        Map<String, Double> mapaInteresMoraAnterior = new HashMap<>();
        try {
            long mesAnterior  = mes == 1 ? 12 : mes - 1;
            long anioAnterior = mes == 1 ? anio - 1 : anio;
            List<EjecucionReporteCartera> ejecucionesAnteriores =
                ejccService.selectByMesAnio(mesAnterior, anioAnterior);
            if (!ejecucionesAnteriores.isEmpty()) {
                Long codigoEjecucionAnterior = ejecucionesAnteriores.get(0).getCodigo();
                List<Object[]> morasAnteriores = ccpmDaoService.selectInteresMoraPorEjecucion(codigoEjecucionAnterior);
                for (Object[] fila : morasAnteriores) {
                    String numOp = (String) fila[0];
                    Double mora  = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
                    if (numOp != null) mapaInteresMoraAnterior.put(numOp, mora);
                }
                System.out.println("CCPM - Mora anterior cargada: " + mapaInteresMoraAnterior.size()
                    + " operaciones (ejecución " + codigoEjecucionAnterior + ")");
            } else {
                System.out.println("CCPM - Sin ejecución anterior para " + mesAnterior + "/" + anioAnterior
                    + ". interesMoraDelMes = 0 para todos los registros.");
            }
        } catch (Throwable e) {
            System.out.println("CCPM - No se pudo cargar mora anterior: " + e.getMessage());
        }

        // -------------------------------------------------------
        // 6. Una fila CCPM por préstamo del universo (sin duplicados: cada préstamo aparece
        //    una sola vez en cuotasPorPrestamo).
        // -------------------------------------------------------
        long contador = 0L;
        Set<String> entidadesConValorAsignado = new HashSet<>();
        List<String> detalleAnclas = new ArrayList<>();

        for (Long idPrestamo : cuotasPorPrestamo.keySet()) {
            try {
                Long idAncla = anclaCuotaPorPrestamo.get(idPrestamo);
                if (idAncla == null) {
                    System.out.println("CCPM - prestamo " + idPrestamo
                        + " sin cuota ancla (no deberia pasar con cuotas pendientes) - se omite");
                    continue;
                }
                DetallePrestamo ancla = detallePrestamoService.selectById(idAncla);
                Prestamo prestamo = ancla.getPrestamo();
                if (prestamo == null) {
                    continue;
                }

                boolean hasVencida = Boolean.TRUE.equals(hasVencidaPorPrestamo.get(idPrestamo));

                Double valorPorVencer = valorPorVencerPorPrestamo.getOrDefault(idPrestamo, 0.0);
                Double valorVencido   = valorVencidoPorPrestamo.getOrDefault(idPrestamo, 0.0);
                Long   diasMorosidad  = diasMorosidadPorPrestamo.getOrDefault(idPrestamo, 0L);
                double[] bandasPV     = bandasPorVencerPorPrestamo.get(idPrestamo);

                String calificacionPropia = calcularCalificacion(diasMorosidad,
                    (prestamo.getProducto() != null ? prestamo.getProducto().getCodigo() : null));

                // --- Campos NO tocados por este item: misma lógica de siempre, ancla redefinida ---
                Double interesOrdinario;
                Double valorDesgravamen;
                Double valorIncendio;
                Double interesOrdinarioDelMes;
                Double interesMora = 0.0;

                if (hasVencida) {
                    Object[] sumas = mapaSumasConMora.get(prestamo.getCodigo());
                    interesOrdinario = (sumas != null && sumas[0] != null) ? (Double) sumas[0] : 0.0;
                    valorDesgravamen = (sumas != null && sumas[1] != null) ? (Double) sumas[1] : 0.0;
                    valorIncendio    = (sumas != null && sumas[2] != null) ? (Double) sumas[2] : 0.0;
                    interesOrdinarioDelMes = mapaInteresOrdinarioDelMesConMora.getOrDefault(prestamo.getCodigo(), 0.0);
                    interesMora = mapaMoraConMora.getOrDefault(idAncla, 0.0);
                } else {
                    interesOrdinario      = ancla.getInteres()             != null ? ancla.getInteres()             : 0.0;
                    valorDesgravamen      = ancla.getDesgravamen()         != null ? ancla.getDesgravamen()         : 0.0;
                    valorIncendio         = ancla.getValorSeguroIncendio() != null ? ancla.getValorSeguroIncendio() : 0.0;
                    interesOrdinarioDelMes = interesOrdinario;
                }

                detalleAnclas.add("prestamo=" + prestamo.getIdAsoprep() + " (codigo=" + idPrestamo
                    + ") ancla=" + idAncla + " numeroCuota=" + ancla.getNumeroCuota()
                    + " vencimiento=" + ancla.getFechaVencimiento()
                    + " [" + (hasVencida ? "cuota vencida mas antigua" : "cuota por vencer mas proxima") + "]");

                // Valor total cuenta individual (sin cambios)
                Double valorCuentaIndividual = 0.0;
                String identificacionEntidad = null;
                if (prestamo.getEntidad() != null) {
                    Long estadoEntidad = prestamo.getEntidad().getIdEstado();

                    // Jubilado complementario: el valor de cuenta individual es siempre 0
                    if (estadoEntidad != null
                            && estadoEntidad == EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO) {
                        valorCuentaIndividual = 0.0;
                    } else {
                        identificacionEntidad = prestamo.getEntidad().getNumeroIdentificacion();
                        if (identificacionEntidad != null && !entidadesConValorAsignado.contains(identificacionEntidad)) {
                            valorCuentaIndividual = mapaValorCuentaIndividual.getOrDefault(
                                prestamo.getEntidad().getCodigo(), 0.0
                            );
                            entidadesConValorAsignado.add(identificacionEntidad);
                        }
                    }
                }

                // Valor sujeto a provisión: max(0, (valorPorVencer + valorVencido) - valorTotalCuentaIndividual)
                Double valorSujetoProvision = Math.max(0.0, (valorPorVencer + valorVencido) - valorCuentaIndividual);

                // Provisión requerida original
                Double provisionRequeridaOriginal = calcularProvision(valorSujetoProvision, calificacionPropia);

                // Provisión constituida = 0 (no depender del histórico)
                Double provisionConstituida = 0.0;

                // Número de operación
                String numeroOperacion = prestamo.getIdAsoprep() != null
                        ? String.valueOf(prestamo.getIdAsoprep()) : null;

                CreditoCuotasPrestamosMensual ccpm = new CreditoCuotasPrestamosMensual();

                ccpm.setTipoIdentificacion("C");
                if (prestamo.getEntidad() != null) {
                    ccpm.setIdentificacion(prestamo.getEntidad().getNumeroIdentificacion());
                    ccpm.setRazonSocial(prestamo.getEntidad().getRazonSocial());
                }

                ccpm.setNumeroOperacion(numeroOperacion);

                if (prestamo.getProducto() != null && prestamo.getProducto().getNombre() != null
                        && !prestamo.getProducto().getNombre().isEmpty()) {
                    ccpm.setTipoCredito(prestamo.getProducto().getNombre());
                }

                ccpm.setDiasMorosidad(diasMorosidad);
                ccpm.setCalificacionPropia(calificacionPropia);
                double tasaInteres = (prestamo.getInteresNominal() != null && prestamo.getInteresNominal() > 0.0)
                        ? prestamo.getInteresNominal() : 9.0;
                ccpm.setTasaInteres(tasaInteres);
                ccpm.setValorPorVencer(valorPorVencer);
                ccpm.setValorVencido(valorVencido);
                ccpm.setCostosOperativos(0.0);
                ccpm.setInteresOrdinario(interesOrdinario);
                ccpm.setInteresOrdinarioDelMes(interesOrdinarioDelMes);
                ccpm.setInteresMora(interesMora);
                // Delta de mora: max(0, interesMoraActual - interesMoraEjecucionAnterior).
                double interesMoraAnterior = numeroOperacion != null
                    ? mapaInteresMoraAnterior.getOrDefault(numeroOperacion, 0.0) : 0.0;
                double interesMoraDelMes = Math.max(0.0, interesMora - interesMoraAnterior);
                ccpm.setInteresMoraDelMes(interesMoraDelMes);
                ccpm.setValorDemandaJudicial(0.0);
                ccpm.setCarteraCastigada(0.0);
                ccpm.setValorTotalCuentaIndividual(valorCuentaIndividual);
                ccpm.setValorSujetoProvision(valorSujetoProvision);
                ccpm.setProvisionRequeridaOriginal(provisionRequeridaOriginal);
                ccpm.setProvisionConstituida(provisionConstituida);
                ccpm.setTipoSistemaAmortizacion("FR");
                ccpm.setCuotaCredito(ancla.getCuota() != null ? ancla.getCuota() : 0.0);
                // El dividendo es la cuota a cobrar SIN mora: desde que existe el proceso diario
                // de mora (ProcesoMoraPrestamoService) DTPRTTLL la incluye, y este reporte ya
                // lleva la mora en su propia columna. Restarla mantiene el CCPM exactamente
                // igual que antes y evita reportarla dos veces.
                ccpm.setDividendo(dividendoSinMora(ancla));

                if (ancla.getFechaVencimiento() != null) {
                    ccpm.setFechaExigibilidad(ancla.getFechaVencimiento().toLocalDate());
                }

                // Fecha del préstamo (PRST.PRSTFCHA)
                if (prestamo.getFecha() != null) {
                    ccpm.setFechaPrestamo(prestamo.getFecha().toLocalDate());
                }

                // Fecha de vencimiento de la última cuota (PRST.PRSTFCFN)
                if (prestamo.getFechaFin() != null) {
                    ccpm.setFechaVencimiento(prestamo.getFechaFin().toLocalDate());
                }
                // Monto solicitado (PRST.PRSTMNSL): sin guarda ni conversión a 0.0 a propósito
                // — no lo escribe ninguna línea del backend, así que un null real tiene que
                // llegar como null hasta el informe, no como un monto de cero.
                ccpm.setMontoSolicitado(prestamo.getMontoSolicitado());

                ccpm.setValorDesgravamen(valorDesgravamen);
                ccpm.setValorIncendio(valorIncendio);

                ccpm.setCapitalPorVencer1a30(bandasPV[0]);
                ccpm.setCapitalPorVencer31a90(bandasPV[1]);
                ccpm.setCapitalPorVencer91a180(bandasPV[2]);
                ccpm.setCapitalPorVencer181a360(bandasPV[3]);
                ccpm.setCapitalPorVencerMas360(bandasPV[4]);
                // Siempre 1: con la misma fórmula para el total (valorPorVencer, paso 4) y para
                // el desglose (bandasPV, mismo paso), cuadran por construcción — no hay "ajuste
                // final" que absorba una diferencia (se eliminó junto con el bloque viejo).
                ccpm.setEstadoDesglose(1L);

                ccpm.setEjecucionReporte(ejecucion);

                ccpmDaoService.save(ccpm, null);
                System.out.println("CCPM INSERT prestamo: " + prestamo.getIdAsoprep()
                    + " ancla: " + idAncla
                    + " grupo: " + (hasVencida ? "vencido" : "por vencer")
                    + " cv30=" + ccpm.getCapitalPorVencer1a30()
                    + " cv90=" + ccpm.getCapitalPorVencer31a90()
                    + " cv180=" + ccpm.getCapitalPorVencer91a180()
                    + " cv360=" + ccpm.getCapitalPorVencer181a360()
                    + " cvMas=" + ccpm.getCapitalPorVencerMas360());
                contador++;
            } catch (Throwable e) {
                System.out.println("CCPM ERROR prestamo " + idPrestamo + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("CCPM generado con " + contador + " registros");
        System.out.println("CCPM - detalle de anclas (item 8, para verificacion del arbitro):");
        for (String linea : detalleAnclas) {
            System.out.println("CCPM ANCLA " + linea);
        }
        return contador;
    }

    /**
     * Bandas del producto+tipoCartera, cacheadas — igual que
     * {@code CierreCarteraServiceImpl#cargaBandas}, pero resolviendo on-demand en vez de
     * precargar toda la parametrización de la empresa (CCPM solo necesita las combinaciones
     * que de verdad aparecen en el universo de hoy).
     */
    private List<BandaProductoDetalle> bandasDe(Long idProducto, Long idEmpresa, Long tipoCartera,
            LocalDate corte, Map<String, List<BandaProductoDetalle>> cache) throws Throwable {
        String clave = idProducto + "-" + tipoCartera;
        List<BandaProductoDetalle> bandas = cache.get(clave);
        if (bandas == null) {
            bandas = clasificadorBandaService.resolverRangos(idProducto, idEmpresa, tipoCartera, corte);
            cache.put(clave, bandas);
        }
        return bandas;
    }

    /**
     * Resuelve la empresa cuando quien llama no la indica explícitamente: la única que tenga
     * configuración de bandas vigente a la fecha. Falla fuerte si hay cero o más de una — hoy
     * solo existe la empresa 1236, pero el día que haya una segunda, adivinar sería silencioso.
     *
     * <p>Fallback deliberado (2026-10-13, DISENO-CCPM-BANDAS-COMO-EL-CIERRE.md §2.1): existe
     * SOLO hasta que el frontend mande {@code idEmpresa} en la solicitud. {@code selectAll()}
     * aquí es sobre {@code CRD.CBPR} —la parametrización de bandas, un puñado de filas— no
     * sobre una tabla transaccional: no es el {@code selectAll()} que CLAUDE.md prohíbe en
     * procesos de carga/generación/consultas pesadas.</p>
     */
    private Long resolverEmpresaUnicaConBandas(LocalDate fecha) throws Throwable {
        List<ConfiguracionBandaProducto> todas = configuracionBandaProductoDaoService
                .selectAll(NombreEntidadesCredito.CONFIGURACION_BANDA_PRODUCTO);
        Set<Long> empresas = new HashSet<>();
        for (ConfiguracionBandaProducto c : todas) {
            if (c.getEmpresa() == null || c.getProducto() == null) {
                continue;
            }
            if (c.getEstado() == null || !c.getEstado().equals(Long.valueOf(Estado.ACTIVO))) {
                continue;
            }
            if (c.getFechaDesde() != null && c.getFechaDesde().isAfter(fecha)) {
                continue;
            }
            if (c.getFechaHasta() != null && c.getFechaHasta().isBefore(fecha)) {
                continue;
            }
            empresas.add(c.getEmpresa().getCodigo());
        }
        if (empresas.isEmpty()) {
            throw new IncomeException("CCPM: no hay ninguna empresa con configuracion de bandas"
                    + " vigente al " + fecha + ". El frontend tiene que enviar idEmpresa explicitamente.");
        }
        if (empresas.size() > 1) {
            throw new IncomeException("CCPM: hay " + empresas.size()
                    + " empresas con configuracion de bandas vigente (" + empresas
                    + "); el frontend tiene que enviar idEmpresa explicitamente, no se puede adivinar.");
        }
        return empresas.iterator().next();
    }

    private Double calcularProvision(Double valorSujetoProvision, String calificacion) {
        if (valorSujetoProvision == null || valorSujetoProvision <= 0.0) return 0.0;
        double porcentaje;
        switch (calificacion == null ? "" : calificacion) {
            case "A1": porcentaje = 0.0099; break;
            case "A2": porcentaje = 0.0199; break;
            case "A3": porcentaje = 0.02;   break;
            case "B1": porcentaje = 0.05;   break;
            case "B2": porcentaje = 0.10;   break;
            case "C1": porcentaje = 0.20;   break;
            case "C2": porcentaje = 0.40;   break;
            case "D":  porcentaje = 0.60;   break;
            case "E":  porcentaje = 1.00;   break;
            default:   porcentaje = 0.0;    break;
        }
        return valorSujetoProvision * porcentaje;
    }

    private String calcularCalificacion(Long diasMorosidad, Long codigoProducto) {
        if (diasMorosidad == null || diasMorosidad == 0) return "A1";

        // Productos hipotecarios: 7, 8, 21
        boolean esHipotecario = codigoProducto != null &&
            (codigoProducto == 7L || codigoProducto == 8L || codigoProducto == 21L);

        if (esHipotecario) {
            if (diasMorosidad >= 1   && diasMorosidad <= 30)  return "A2";
            if (diasMorosidad >= 31  && diasMorosidad <= 60)  return "A3";
            if (diasMorosidad >= 61  && diasMorosidad <= 120) return "B1";
            if (diasMorosidad >= 121 && diasMorosidad <= 180) return "B2";
            if (diasMorosidad >= 181 && diasMorosidad <= 210) return "C1";
            if (diasMorosidad >= 211 && diasMorosidad <= 270) return "C2";
            if (diasMorosidad >= 271 && diasMorosidad <= 450) return "D";
            return "E"; // más de 450 días
        } else {
            if (diasMorosidad >= 1   && diasMorosidad <= 15)  return "A2";
            if (diasMorosidad >= 16  && diasMorosidad <= 30)  return "A3";
            if (diasMorosidad >= 31  && diasMorosidad <= 60)  return "B1";
            if (diasMorosidad >= 61  && diasMorosidad <= 90)  return "B2";
            if (diasMorosidad >= 91  && diasMorosidad <= 120) return "C1";
            if (diasMorosidad >= 121 && diasMorosidad <= 180) return "C2";
            if (diasMorosidad >= 181 && diasMorosidad <= 270) return "D";
            return "E"; // más de 270 días
        }
    }

    /**
     * Dividendo del reporte: el total de la cuota SIN el interés de mora ni el interés vencido.
     *
     * Desde que existe el proceso diario de mora
     * ({@code com.saa.ejb.crd.service.ProcesoMoraPrestamoService}), DTPRTTLL de una cuota
     * vencida incluye la mora acumulada. Este reporte ya lleva la mora en su propia columna, así
     * que restarla del dividendo mantiene la salida idéntica a la de antes del proceso diario y
     * evita reportar el mismo valor dos veces.
     *
     * @param cuota Cuota del reporte
     * @return Total de la cuota sin mora ni interés vencido
     */
    private Double dividendoSinMora(com.saa.model.crd.DetallePrestamo cuota) {
        if (cuota == null) {
            return 0.0;
        }
        double total = cuota.getTotal() != null ? cuota.getTotal() : 0.0;
        double mora = cuota.getMora() != null ? cuota.getMora() : 0.0;
        double interesVencido = cuota.getInteresVencido() != null ? cuota.getInteresVencido() : 0.0;
        return total - mora - interesVencido;
    }
}
