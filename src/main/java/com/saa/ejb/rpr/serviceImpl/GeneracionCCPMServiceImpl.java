package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosMensualDaoService;
import com.saa.ejb.rpr.service.CreditoParticipesMensualService;
import com.saa.ejb.rpr.service.EjecucionReporteCarteraService;
import com.saa.ejb.rpr.service.GeneracionCCPMService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;
import com.saa.model.rpr.CreditoParticipesMensual;
import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Servicio de generación del reporte CCPM (Crédito Cuotas Préstamos Mensual).
 * Lógica similar al G48 pero con campos adicionales: valorDesgravamen y valorIncendio.
 * OPTIMIZADO: provisionConstituida = 0 (sin dependencia de histórico del mes anterior).
 */
@Stateless
public class GeneracionCCPMServiceImpl implements GeneracionCCPMService {

    @EJB private DetallePrestamoService                    detallePrestamoService;
    @EJB private CreditoParticipesMensualService           cprmService;
    @EJB private CreditoCuotasPrestamosMensualDaoService   ccpmDaoService;
    @EJB private EjecucionReporteCarteraService            ejccService;

    @Override
    public long generar(EjecucionReporteCartera ejecucion) throws Throwable {
        System.out.println("Ingresa al metodo generar CCPM");

        long mes  = ejecucion.getMes();
        long anio = ejecucion.getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();

        LocalDateTime fechaInicio = LocalDateTime.of((int) anio, (int) mes, 1, 0, 0, 0);
        LocalDateTime fechaFin    = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);
        LocalDate     fechaFinDate = fechaFin.toLocalDate();

        System.out.println("CCPM - Rango: " + fechaInicio + " a " + fechaFin);

        // -------------------------------------------------------
        // 1. Cargar valorTotalCuentaIndividual desde CPRM del mismo mes.
        //    El nuevo modelo CPRM tiene UN REGISTRO POR entidad+tipoAporte,
        //    cada uno con su propio campo "total". El valorTotalCuentaIndividual
        //    es la SUMA de todos esos totales para cada entidad.
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
        // 3. GRUPO 1: Cuotas con fechaVencimiento en el mes, estado != 7
        // -------------------------------------------------------
        List<DetallePrestamo> grupo1 = detallePrestamoService.selectCuotasDelMesGlobal(fechaInicio, fechaFin);
        System.out.println("CCPM - Grupo 1 (cuotas del mes): " + grupo1.size());

        // -------------------------------------------------------
        // 4. GRUPO 2: Menor cuota por préstamo anterior al mes
        // -------------------------------------------------------
        List<DetallePrestamo> grupo2 = detallePrestamoService.selectMenorCuotaAnteriorAlMesGlobal(fechaInicio, fechaFin);
        System.out.println("CCPM - Grupo 2 (menor cuota anterior): " + grupo2.size());

        // -------------------------------------------------------
        // 5. Consolidar evitando duplicados
        // -------------------------------------------------------
        Set<Long> prestamosEnGrupo1 = new HashSet<>();
        for (DetallePrestamo d : grupo1) {
            if (d.getPrestamo() != null) prestamosEnGrupo1.add(d.getPrestamo().getCodigo());
        }

        List<DetallePrestamo> cuotasAProcesar = new ArrayList<>(grupo1);
        for (DetallePrestamo d : grupo2) {
            if (d.getPrestamo() != null && !prestamosEnGrupo1.contains(d.getPrestamo().getCodigo())) {
                cuotasAProcesar.add(d);
            }
        }
        System.out.println("CCPM - Total cuotas a procesar: " + cuotasAProcesar.size());

        if (cuotasAProcesar.isEmpty()) {
            System.out.println("CCPM - Sin cuotas. CCPM vacío, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 5b. Cargar datos del grupo 2 en batch (optimización N+1)
        // -------------------------------------------------------
        List<DetallePrestamo> cuotasGrupo2 = new ArrayList<>();
        for (DetallePrestamo d : cuotasAProcesar) {
            if (d.getPrestamo() != null && !prestamosEnGrupo1.contains(d.getPrestamo().getCodigo())) {
                cuotasGrupo2.add(d);
            }
        }
        System.out.println("CCPM - Cuotas grupo 2 para optimizar: " + cuotasGrupo2.size());

        // Recolectar PKs de las cuotas origen del grupo 2 (sirve para ambos batch)
        List<Long> codigosCuotasMora = new ArrayList<>();
        for (DetallePrestamo cuota : cuotasGrupo2) {
            if (cuota.getPrestamo() != null) {
                codigosCuotasMora.add(cuota.getCodigo());
            }
        }

        // Ejecutar consultas batch
        Map<Long, Object[]> mapaSumasGrupo2 = new HashMap<>();
        if (!codigosCuotasMora.isEmpty()) {
            List<Object[]> sumasGrupo2 = detallePrestamoService.selectSumaCapitalInteresGrupo2Batch(
                codigosCuotasMora, fechaInicio, fechaFin
            );
            for (Object[] suma : sumasGrupo2) {
                Long codPrest = (Long) suma[0];
                mapaSumasGrupo2.put(codPrest, new Object[]{suma[1], suma[2], suma[3], suma[4]});
            }
            System.out.println("CCPM - Sumas grupo 2 cargadas en batch: " + mapaSumasGrupo2.size());
        }

        Map<Long, Double> mapaMoraGrupo2 = new HashMap<>();
        if (!codigosCuotasMora.isEmpty()) {
            List<Object[]> morasGrupo2 = detallePrestamoService.calcularInteresMoraBatch(
                codigosCuotasMora, fechaFin
            );
            for (Object[] mora : morasGrupo2) {
                Long codCuota = (Long) mora[0];
                Double valorMora = mora[1] != null ? (Double) mora[1] : 0.0;
                mapaMoraGrupo2.put(codCuota, valorMora);
            }
            System.out.println("CCPM - Moras grupo 2 cargadas en batch: " + mapaMoraGrupo2.size());
        }

        // Carga batch de la cuota del mes de ejecución para el Grupo 2:
        // el valorPorVencer del Grupo 2 debe calcularse con saldoInicialCapital - capital
        // de la cuota que cae DENTRO del mes de ejecución, no de la cuota en mora.
        // Map<codigoPrestamo, valorPorVencer = saldoInicialCapital - capital>
        Map<Long, Double> mapaValorPorVencerGrupo2 = new HashMap<>();
        Map<Long, Double> mapaInteresOrdinarioDelMesGrupo2 = new HashMap<>();
        if (!cuotasGrupo2.isEmpty()) {
            List<Long> codsPrestamosGrupo2 = new ArrayList<>();
            for (DetallePrestamo d : cuotasGrupo2) {
                if (d.getPrestamo() != null) codsPrestamosGrupo2.add(d.getPrestamo().getCodigo());
            }
            List<Object[]> saldosDelMes = detallePrestamoService.selectSaldoInicialCapitalDelMesBatch(
                codsPrestamosGrupo2, fechaInicio, fechaFin
            );
            for (Object[] fila : saldosDelMes) {
                Long   codPrest          = (Long)   fila[0];
                Double saldoInicialCap   = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
                Double interesDelMes     = fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0;
                // CORRECCIÓN: valorPorVencer = saldoInicialCapital completo de la cuota del mes,
                // ya que esa cuota NO va a vencido (por política del fondo no está en mora hasta
                // que pase el último día del mes). El capital del mes queda en "por vencer".
                mapaValorPorVencerGrupo2.put(codPrest, saldoInicialCap);
                mapaInteresOrdinarioDelMesGrupo2.put(codPrest, interesDelMes);
            }
            System.out.println("CCPM - ValorPorVencer Grupo 2 cargados en batch: " + mapaValorPorVencerGrupo2.size());
        }

        // Delta de mora: interesMoraActual - interesMoraEjecucionAnterior (por numeroOperacion).
        // Si es la primera ejecución o no existe registro anterior, el delta queda en 0.
        // Map<numeroOperacion, interesMoraAnterior>
        Map<String, Double> mapaInteresMoraAnterior = new HashMap<>();
        try {
            // Calcular mes/año de la ejecución anterior
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
        // 5c. Desglose de capital por vencer en 5 períodos.
        //     GRUPO 1: cuotas con fechaVencimiento > fechaFin (posteriores al mes de ejecución).
        //              Índice 0 = próxima cuota tras el cierre del mes = bucket 1-30 días.
        //     GRUPO 2: cuotas con fechaVencimiento >= fechaInicio (desde el inicio del mes inclusive),
        //              porque la cuota del mes NO está en mora y debe ir al bucket 1-30 días (cv30).
        //              Índice 0 = cuota del mes de ejecución = bucket 1-30 días.
        // -------------------------------------------------------
        List<Long> codsPrestamosGrupo1Desglose = new ArrayList<>();
        List<Long> codsPrestamosGrupo2Desglose = new ArrayList<>();
        for (DetallePrestamo d : cuotasAProcesar) {
            if (d.getPrestamo() == null) continue;
            if (prestamosEnGrupo1.contains(d.getPrestamo().getCodigo())) {
                codsPrestamosGrupo1Desglose.add(d.getPrestamo().getCodigo());
            } else {
                codsPrestamosGrupo2Desglose.add(d.getPrestamo().getCodigo());
            }
        }

        Map<Long, List<Double>> mapaCapitalesFuturos = new HashMap<>();
        Map<Long, Double> mapaSaldoOtrosFuturos = new HashMap<>();

        // Grupo 1: cuotas posteriores al mes de ejecución
        if (!codsPrestamosGrupo1Desglose.isEmpty()) {
            List<Object[]> filas = detallePrestamoService.selectCapitalCuotasFuturasBatch(
                codsPrestamosGrupo1Desglose, fechaFin
            );
            for (Object[] fila : filas) {
                Long   codPrest   = (Long)   fila[0];
                Double capital    = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
                Double saldoOtros = fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0;
                mapaCapitalesFuturos.computeIfAbsent(codPrest, k -> new ArrayList<>()).add(capital);
                mapaSaldoOtrosFuturos.merge(codPrest, saldoOtros, Double::sum);
            }
            System.out.println("CCPM - Desglose capital Grupo 1 cargado: " + codsPrestamosGrupo1Desglose.size() + " préstamos");
        }

        // Grupo 2: desde el inicio del mes inclusive (la cuota del mes cae en cv30)
        if (!codsPrestamosGrupo2Desglose.isEmpty()) {
            List<Object[]> filas = detallePrestamoService.selectCapitalCuotasDesdeInicioMesBatch(
                codsPrestamosGrupo2Desglose, fechaInicio
            );
            for (Object[] fila : filas) {
                Long   codPrest   = (Long)   fila[0];
                Double capital    = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
                Double saldoOtros = fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0;
                mapaCapitalesFuturos.computeIfAbsent(codPrest, k -> new ArrayList<>()).add(capital);
                mapaSaldoOtrosFuturos.merge(codPrest, saldoOtros, Double::sum);
            }
            System.out.println("CCPM - Desglose capital Grupo 2 cargado (desde inicio mes): " + codsPrestamosGrupo2Desglose.size() + " préstamos");
        }

        // -------------------------------------------------------
        // 6. Por cada cuota → generar un registro en CCPM
        // -------------------------------------------------------
        long contador = 0L;
        Set<String> entidadesConValorAsignado = new HashSet<>();

        for (DetallePrestamo cuota : cuotasAProcesar) {
            try {
                Prestamo prestamo = cuota.getPrestamo();
                if (prestamo == null) continue;

                boolean esGrupo1 = prestamosEnGrupo1.contains(prestamo.getCodigo());

                // Calcular días de morosidad
                Long diasMorosidad = 0L;
                if (cuota.getEstado() != null && !cuota.getEstado().equals(4L)) {
                    if (cuota.getFechaVencimiento() != null) {
                        LocalDate fechaVencimiento = cuota.getFechaVencimiento().toLocalDate();
                        diasMorosidad = ChronoUnit.DAYS.between(fechaVencimiento, fechaFinDate);
                        if (diasMorosidad < 0) diasMorosidad = 0L;
                    }
                }
                System.out.println("CCPM LLEGA 10 ");
                String calificacionPropia = calcularCalificacion(diasMorosidad,
                    (prestamo.getProducto() != null ? prestamo.getProducto().getCodigo() : null));

                // Valor por vencer:
                // Grupo 1: saldoInicialCapital - capital de la cuota del mes (la cuota que se está procesando).
                // Grupo 2: saldoInicialCapital - capital de la cuota que cae DENTRO del mes de ejecución,
                //          no de la cuota en mora que se está insertando.
                Double valorPorVencer;
                if (esGrupo1) {
                    Double saldoInicialCapital = cuota.getSaldoInicialCapital() != null ? cuota.getSaldoInicialCapital() : 0.0;
                    Double capitalCuota        = cuota.getCapital()             != null ? cuota.getCapital()             : 0.0;
                    valorPorVencer = Math.max(0.0, saldoInicialCapital - capitalCuota);
                } else {
                    // Tomar del mapa pre-cargado; si no hay cuota en el mes para ese préstamo → 0
                    valorPorVencer = mapaValorPorVencerGrupo2.getOrDefault(prestamo.getCodigo(), 0.0);
                }

                // Valor vencido, interés ordinario, desgravamen e incendio
                Double valorVencido = 0.0;
                Double interesOrdinario = 0.0;
                Double valorDesgravamen = 0.0;
                Double valorIncendio    = 0.0;
                Double interesOrdinarioDelMes = 0.0;
                System.out.println("CCPM LLEGA 9 ");
                if (esGrupo1) {
                    valorVencido          = 0.0;
                    interesOrdinario      = cuota.getInteres()             != null ? cuota.getInteres()             : 0.0;
                    valorDesgravamen      = cuota.getDesgravamen()         != null ? cuota.getDesgravamen()         : 0.0;
                    valorIncendio         = cuota.getValorSeguroIncendio() != null ? cuota.getValorSeguroIncendio() : 0.0;
                    interesOrdinarioDelMes = interesOrdinario; // Grupo 1: la cuota ya es la del mes
                } else {
                    // Obtener del mapa pre-cargado (grupo 2): sumas acumuladas de todas las cuotas vencidas
                    Object[] sumas = mapaSumasGrupo2.get(prestamo.getCodigo());
                    if (sumas != null) {
                        valorVencido     = sumas[0] != null ? (Double) sumas[0] : 0.0;
                        interesOrdinario = sumas[1] != null ? (Double) sumas[1] : 0.0;
                        valorDesgravamen = sumas[2] != null ? (Double) sumas[2] : 0.0;
                        valorIncendio    = sumas[3] != null ? (Double) sumas[3] : 0.0;
                    }
                    // interesOrdinarioDelMes: el interés de la cuota con fechaVencimiento dentro del mes
                    interesOrdinarioDelMes = mapaInteresOrdinarioDelMesGrupo2.getOrDefault(prestamo.getCodigo(), 0.0);
                }
                System.out.println("CCPM LLEGA 8 ");

                // Valor total cuenta individual
                Double valorCuentaIndividual = 0.0;
                String identificacionEntidad = null;
                if (prestamo.getEntidad() != null) {
                    Long estadoEntidad = prestamo.getEntidad().getIdEstado();
                    
                    if (estadoEntidad != null && estadoEntidad == 30L) {
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
                System.out.println("CCPM LLEGA 7 ");
                // Valor sujeto a provisión
                // max(0, (valorPorVencer + valorVencido) - valorTotalCuentaIndividual)
                Double valorSujetoProvision = Math.max(0.0, (valorPorVencer + valorVencido) - valorCuentaIndividual);

                // Provisión requerida original
                Double provisionRequeridaOriginal = calcularProvision(valorSujetoProvision, calificacionPropia);

                // Provisión constituida = 0 (no depender del histórico)
                Double provisionConstituida = 0.0;

                // Interés mora
                Double interesMora = 0.0;
                if (!esGrupo1) {
                    // Obtener del mapa pre-cargado (grupo 2)
                    interesMora = mapaMoraGrupo2.getOrDefault(cuota.getCodigo(), 0.0);
                }
                System.out.println("CCPM LLEGA 6 ");
                // Número de operación
                String numeroOperacion = prestamo.getIdAsoprep() != null
                        ? String.valueOf(prestamo.getIdAsoprep()) : null;

                CreditoCuotasPrestamosMensual ccpm = new CreditoCuotasPrestamosMensual();

                ccpm.setTipoIdentificacion("C");
                if (prestamo.getEntidad() != null) {
                    ccpm.setIdentificacion(prestamo.getEntidad().getNumeroIdentificacion());
                }

                ccpm.setNumeroOperacion(numeroOperacion);

                if (prestamo.getProducto() != null && prestamo.getProducto().getNombre() != null
                        && !prestamo.getProducto().getNombre().isEmpty()) {
                    ccpm.setTipoCredito(prestamo.getProducto().getNombre());
                }
                System.out.println("CCPM LLEGA 5 ");
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
                // Si es la primera ejecución o no existe registro anterior, queda en 0.
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
                ccpm.setCuotaCredito(cuota.getCuota() != null ? cuota.getCuota() : 0.0);
                ccpm.setDividendo(cuota.getTotal() != null ? cuota.getTotal() : 0.0);

                if (cuota.getFechaVencimiento() != null) {
                    ccpm.setFechaExigibilidad(cuota.getFechaVencimiento().toLocalDate());
                }

                // Fecha del préstamo (PRST.PRSTFCHA)
                if (prestamo.getFecha() != null) {
                    ccpm.setFechaPrestamo(prestamo.getFecha().toLocalDate());
                }
                System.out.println("CCPM LLEGA 4 ");
                // ** ASIGNAR NUEVOS CAMPOS: desgravamen e incendio **
                ccpm.setValorDesgravamen(valorDesgravamen);
                ccpm.setValorIncendio(valorIncendio);

                // ** ASIGNAR DESGLOSE DE CAPITAL POR VENCER EN PERÍODOS **
                // Para ambos grupos: cuotas con fechaVencimiento > fechaFin (siguiente al mes de ejecución).
                // Índice 0 = próxima cuota tras el cierre del mes = bucket 1-30 días.
                List<Double> capitales = mapaCapitalesFuturos.getOrDefault(prestamo.getCodigo(), new ArrayList<>());
                Double cv30  = sumarRango(capitales, 0,  0);
                Double cv90  = sumarRango(capitales, 1,  3);
                Double cv180 = sumarRango(capitales, 4,  6);
                Double cv360 = sumarRango(capitales, 7,  11);
                Double cvMas = sumarRango(capitales, 12, Integer.MAX_VALUE - 1);
                System.out.println("CCPM LLEGA 3 ");
                // Sumar saldoOtros a la última banda con valor
                Double saldoOtrosFuturos = mapaSaldoOtrosFuturos.getOrDefault(prestamo.getCodigo(), 0.0);
                if (saldoOtrosFuturos > 0.0) {
                    if      (cvMas > 0.0)  cvMas += saldoOtrosFuturos;
                    else if (cv360 > 0.0)  cv360 += saldoOtrosFuturos;
                    else if (cv180 > 0.0)  cv180 += saldoOtrosFuturos;
                    else if (cv90  > 0.0)  cv90  += saldoOtrosFuturos;
                    else                   cv30  += saldoOtrosFuturos;
                }

                // -- CONTROL DE CONSISTENCIA Y AJUSTE FINAL --
                // Ajustar cualquier diferencia (redondeo, etc.) en la última banda para forzar el cuadre.
                double sumaDesglose = cv30 + cv90 + cv180 + cv360 + cvMas;
                double diferencia   = valorPorVencer - sumaDesglose;
                long   estadoDesglose = 1L; // Asumir que siempre cuadra tras el ajuste

                if (diferencia != 0.0) {
                    if (Math.abs(diferencia) >= 1.0) {
                        System.out.println("CCPM ADVERTENCIA prestamo=" + prestamo.getIdAsoprep()
                            + " | valorPorVencer=" + valorPorVencer
                            + " | sumaDesglose=" + sumaDesglose
                            + " | diferencia=" + diferencia + " (se ajustará)");
                    }
                    // Absorber la diferencia en el bucket de mayor plazo con saldo > 0.
                    if      (cvMas > 0.0)  cvMas += diferencia;
                    else if (cv360 > 0.0)  cv360 += diferencia;
                    else if (cv180 > 0.0)  cv180 += diferencia;
                    else if (cv90  > 0.0)  cv90  += diferencia;
                    else                   cv30  += diferencia;
                }
                System.out.println("CCPM LLEGA 2 ");

                ccpm.setCapitalPorVencer1a30(cv30);
                ccpm.setCapitalPorVencer31a90(cv90);
                ccpm.setCapitalPorVencer91a180(cv180);
                ccpm.setCapitalPorVencer181a360(cv360);
                ccpm.setCapitalPorVencerMas360(cvMas);
                ccpm.setEstadoDesglose(estadoDesglose);

                ccpm.setEjecucionReporte(ejecucion);

                ccpmDaoService.save(ccpm, null);
                System.out.println("CCPM INSERT prestamo: " + prestamo.getIdAsoprep()
                    + " cuota: " + cuota.getNumeroCuota()
                    + " grupo: " + (esGrupo1 ? "1" : "2")
                    + " cv30=" + ccpm.getCapitalPorVencer1a30()
                    + " cv90=" + ccpm.getCapitalPorVencer31a90()
                    + " cv180=" + ccpm.getCapitalPorVencer91a180()
                    + " cv360=" + ccpm.getCapitalPorVencer181a360()
                    + " cvMas=" + ccpm.getCapitalPorVencerMas360());
                contador++;
                System.out.println("CCPM LLEGA 1 ");
            } catch (Throwable e) {
                System.out.println("CCPM ERROR cuota " + cuota.getCodigo() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("CCPM generado con " + contador + " registros");
        return contador;
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

    /**
     * Suma los elementos de la lista en el rango [desdeIdx, hastaIdx] (ambos inclusivos).
     * Si el rango excede el tamaño de la lista, suma solo hasta el último elemento disponible.
     * @param lista    Lista de capitales ordenada por numeroCuota ASC (índice 0 = siguiente cuota tras ejecución)
     * @param desdeIdx índice de inicio (0-based)
     * @param hastaIdx índice de fin (0-based, inclusive); usar Integer.MAX_VALUE - 1 para "hasta el final"
     * @return suma de los capitales en el rango, 0.0 si no hay elementos
     */
    private Double sumarRango(List<Double> lista, int desdeIdx, int hastaIdx) {
        if (lista == null || lista.isEmpty() || desdeIdx >= lista.size()) return 0.0;
        int fin = Math.min(hastaIdx, lista.size() - 1);
        double suma = 0.0;
        for (int i = desdeIdx; i <= fin; i++) {
            Double v = lista.get(i);
            if (v != null) suma += v;
        }
        return suma;
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
}
