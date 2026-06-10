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
                codigosCuotasMora, fechaFin
            );
            for (Object[] suma : sumasGrupo2) {
                Long codPrest = (Long) suma[0];
                mapaSumasGrupo2.put(codPrest, new Object[]{suma[1], suma[2]});
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
                Double capitalDelMes     = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
                mapaValorPorVencerGrupo2.put(codPrest, Math.max(0.0, saldoInicialCap - capitalDelMes));
            }
            System.out.println("CCPM - ValorPorVencer Grupo 2 cargados en batch: " + mapaValorPorVencerGrupo2.size());
        }

        // -------------------------------------------------------
        // 5c. Desglose de capital por vencer en 5 períodos.
        //     Para AMBOS grupos el desglose parte de la cuota SIGUIENTE al mes de
        //     ejecución (fechaVencimiento > fechaFin), estado != 4 y != 7.
        //     Índice 0 = próxima cuota tras el cierre del mes = bucket 1-30 días.
        //     Map<codigoPrestamo, List<capital>> ordenado por numeroCuota ASC.
        // -------------------------------------------------------
        List<Long> codigosPrestamosDesglose = new ArrayList<>();
        for (DetallePrestamo d : cuotasAProcesar) {
            if (d.getPrestamo() != null) codigosPrestamosDesglose.add(d.getPrestamo().getCodigo());
        }
        Map<Long, List<Double>> mapaCapitalesFuturos = new HashMap<>();
        if (!codigosPrestamosDesglose.isEmpty()) {
            List<Object[]> filas = detallePrestamoService.selectCapitalCuotasFuturasBatch(
                codigosPrestamosDesglose, fechaFin
            );
            for (Object[] fila : filas) {
                Long   codPrest = (Long)   fila[0];
                Double capital  = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
                mapaCapitalesFuturos.computeIfAbsent(codPrest, k -> new ArrayList<>()).add(capital);
            }
            System.out.println("CCPM - Desglose capital (ambos grupos) cargado: " + mapaCapitalesFuturos.size() + " préstamos");
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

                // Valor vencido e interés ordinario
                Double valorVencido = 0.0;
                Double interesOrdinario = 0.0;
                
                if (esGrupo1) {
                    valorVencido = 0.0;
                    interesOrdinario = cuota.getInteres() != null ? cuota.getInteres() : 0.0;
                } else {
                    // Obtener del mapa pre-cargado (grupo 2)
                    Object[] sumas = mapaSumasGrupo2.get(prestamo.getCodigo());
                    if (sumas != null) {
                        valorVencido = sumas[0] != null ? (Double) sumas[0] : 0.0;
                        interesOrdinario = sumas[1] != null ? (Double) sumas[1] : 0.0;
                    }
                }

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

                // Valor sujeto a provisión
                Double valorSujetoProvision = Math.max(0.0, valorPorVencer - valorCuentaIndividual);

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

                // ** NUEVOS CAMPOS: valorDesgravamen y valorIncendio **
                Double valorDesgravamen = cuota.getDesgravamen() != null ? cuota.getDesgravamen() : 0.0;
                Double valorIncendio = cuota.getValorSeguroIncendio() != null ? cuota.getValorSeguroIncendio() : 0.0;

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

                ccpm.setDiasMorosidad(diasMorosidad);
                ccpm.setCalificacionPropia(calificacionPropia);
                double tasaInteres = (prestamo.getInteresNominal() != null && prestamo.getInteresNominal() > 0.0)
                        ? prestamo.getInteresNominal() : 9.0;
                ccpm.setTasaInteres(tasaInteres);
                ccpm.setValorPorVencer(valorPorVencer);
                ccpm.setValorVencido(valorVencido);
                ccpm.setCostosOperativos(0.0);
                ccpm.setInteresOrdinario(interesOrdinario);
                ccpm.setInteresMora(interesMora);
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

                // ** ASIGNAR NUEVOS CAMPOS: desgravamen e incendio **
                ccpm.setValorDesgravamen(valorDesgravamen);
                ccpm.setValorIncendio(valorIncendio);

                // ** ASIGNAR DESGLOSE DE CAPITAL POR VENCER EN PERÍODOS **
                // Para ambos grupos: cuotas con fechaVencimiento > fechaFin (siguiente al mes de ejecución).
                // Índice 0 = próxima cuota tras el cierre del mes = bucket 1-30 días.
                List<Double> capitales = mapaCapitalesFuturos.getOrDefault(prestamo.getCodigo(), new ArrayList<>());
                Double cv30  = sumarRango(capitales, 0,  0);   // offset 1      → índice 0
                Double cv90  = sumarRango(capitales, 1,  3);   // offsets 2-4   → índices 1-3
                Double cv180 = sumarRango(capitales, 4,  6);   // offsets 5-7   → índices 4-6
                Double cv360 = sumarRango(capitales, 7,  11);  // offsets 8-12  → índices 7-11
                Double cvMas = sumarRango(capitales, 12, Integer.MAX_VALUE - 1); // offset 13+ → índices 12+

                // -- CONTROL DE CONSISTENCIA --
                // estadoDesglose = 1 → suma de los 5 períodos == valorPorVencer (diferencia <= 0.01 por redondeo, se absorbe).
                // estadoDesglose = 2 → diferencia > 0.01; problema de datos, revisar DTPR vs saldoPorVencer.
                double sumaDesglose = cv30 + cv90 + cv180 + cv360 + cvMas;
                double diferencia   = valorPorVencer - sumaDesglose;
                long   estadoDesglose;
                if (Math.abs(diferencia) > 0.01) {
                    estadoDesglose = 2L;
                    System.out.println("CCPM ADVERTENCIA estadoDesglose=2 prestamo=" + prestamo.getIdAsoprep()
                        + " valorPorVencer=" + valorPorVencer
                        + " sumaDesglose=" + sumaDesglose
                        + " diferencia=" + diferencia
                        + " | Revisar saldo DTPR vs saldoPorVencer en Prestamo.");
                } else {
                    estadoDesglose = 1L;
                    // Diferencia de redondeo (abs <= 0.01): absorber en el bucket de mayor plazo con saldo > 0.
                    if (diferencia != 0.0) {
                        if      (cvMas > 0.0)  cvMas += diferencia;
                        else if (cv360 > 0.0)  cv360 += diferencia;
                        else if (cv180 > 0.0)  cv180 += diferencia;
                        else if (cv90  > 0.0)  cv90  += diferencia;
                        else                   cv30  += diferencia;
                    }
                }

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
