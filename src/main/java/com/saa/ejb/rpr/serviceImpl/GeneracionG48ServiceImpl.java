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

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.CalificacionRiesgoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.dto.ResultadoCalificacionRiesgo;
import com.saa.ejb.rpr.dao.CancelacionG49DaoService;
import com.saa.ejb.rpr.dao.SaldoOperacionG48DaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.EjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG48Service;
import com.saa.ejb.rpr.service.HistoricoG48Service;
import com.saa.ejb.rpr.service.SaldoCuentaG42Service;
import com.saa.ejb.rpr.service.SaldoOperacionG48Service;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.EjecucionReporte;
import com.saa.model.rpr.HistoricoG48;
import com.saa.model.rpr.SaldoCuentaG42;
import com.saa.model.rpr.SaldoOperacionG48;
import com.saa.rubros.EstadoParticipeEntidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG48ServiceImpl implements GeneracionG48Service {

    @EJB private DetallePrestamoService         detallePrestamoService;
    @EJB private SaldoCuentaG42Service          saldoCuentaG42Service;
    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private SaldoOperacionG48DaoService    cg48DaoService;
    @EJB private SaldoOperacionG48Service       saldoG48Service;
    @EJB private EjecucionReporteService        ejrcService;
    @EJB private HistoricoG48Service            historicoG48Service;
    @EJB private CancelacionG49DaoService       cg49DaoService;
    /** PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md: la escala de calificación/provisión ya no
     * está cableada acá — se resuelve contra CRD.CFCR/CRD.ESCR. */
    @EJB private CalificacionRiesgoService       calificacionRiesgoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G48");

        long mes  = detalle.getEjecucionReporte().getMes();
        long anio = detalle.getEjecucionReporte().getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();

        LocalDateTime fechaInicio = LocalDateTime.of((int) anio, (int) mes, 1, 0, 0, 0);
        LocalDateTime fechaFin    = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);
        LocalDate     fechaFinDate = fechaFin.toLocalDate();

        System.out.println("G48 - Rango: " + fechaInicio + " a " + fechaFin);

        // -------------------------------------------------------
        // 1. Cargar valorTotalCuentaIndividual desde G42 del mismo mes
        // -------------------------------------------------------
        Map<Long, Double> mapaValorCuentaIndividual = new HashMap<>();
        try {
            DetalleEjecucionReporte ejrdG42 = ejrdService.selectByEjecucionYTipo(
                detalle.getEjecucionReporte().getCodigo(), "G42"
            );
            if (ejrdG42 != null) {
                List<SaldoCuentaG42> registrosG42 = saldoCuentaG42Service.selectByDetalle(ejrdG42.getCodigo());
                for (SaldoCuentaG42 g42 : registrosG42) {
                    if (g42.getEntidad() != null) {
                        Double patronal    = g42.getSaldoAportePatronal()  != null ? g42.getSaldoAportePatronal()  : 0.0;
                        Double personal    = g42.getSaldoAportePersonal()  != null ? g42.getSaldoAportePersonal()  : 0.0;
                        Double rendimiento = g42.getRendimiento()           != null ? g42.getRendimiento()           : 0.0;
                        mapaValorCuentaIndividual.put(g42.getEntidad().getCodigo(), patronal + personal + rendimiento);
                    }
                }
            }
        } catch (Throwable e) {
            System.out.println("G48 - No se pudo cargar G42: " + e.getMessage());
        }
        System.out.println("G48 - Valores cuenta individual cargados: " + mapaValorCuentaIndividual.size());

        // -------------------------------------------------------
        // 2. Cargar provisionRequeridaOriginal del período anterior
        //    en un Map<numeroOperacion, provisionRequeridaOriginal>
        //    Solo aplica si hay EJRC anterior con G48; si no, se consultará
        //    individualmente desde HistoricoG48 por cada operación.
        // -------------------------------------------------------
        Map<String, Double> mapaProvisionAnterior = new HashMap<>();
        boolean usarHistorico = cargarProvisionAnterior(
            detalle.getEjecucionReporte(), mes, anio, mapaProvisionAnterior
        );
        System.out.println("G48 - Provisiones período anterior cargadas: " + mapaProvisionAnterior.size()
            + (usarHistorico ? " (se consultará HistoricoG48 por operación)" : ""));

        // -------------------------------------------------------
        // 3. GRUPO 1: Cuotas con fechaVencimiento en el mes, estado != 7
        // -------------------------------------------------------
        List<DetallePrestamo> grupo1 = detallePrestamoService.selectCuotasDelMesGlobal(fechaInicio, fechaFin);
        System.out.println("G48 - Grupo 1 (cuotas del mes): " + grupo1.size());

        // -------------------------------------------------------
        // 4. GRUPO 2: Menor cuota por préstamo anterior al mes,
        //    estado 2,3,5,6 y préstamo en estado 2,8,11
        // -------------------------------------------------------
        List<DetallePrestamo> grupo2 = detallePrestamoService.selectMenorCuotaAnteriorAlMesGlobal(fechaInicio, fechaFin);
        System.out.println("G48 - Grupo 2 (menor cuota anterior): " + grupo2.size());

        // -------------------------------------------------------
        // 5. Consolidar evitando duplicados (mismo préstamo en ambos grupos)
        //    Del grupo 2 excluimos los préstamos que ya están en grupo 1
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
        System.out.println("G48 - Total cuotas a procesar: " + cuotasAProcesar.size());

        if (cuotasAProcesar.isEmpty()) {
            System.out.println("G48 - Sin cuotas. G48 vacío, OK.");
            return 0L;
        }

        // PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md §5: falla UNA sola vez con el listado
        // completo de productos sin configurar, antes de arrancar — no producto por producto
        // a mitad de una corrida que ya lleva rato. Punto único y fácil de invertir (a "caer a
        // la escala general") si el usuario prefiriera eso más adelante: todavía no respondió.
        List<Producto> productosSinConfiguracion = calificacionRiesgoService.productosSinConfiguracion(fechaFinDate);
        if (!productosSinConfiguracion.isEmpty()) {
            StringBuilder listado = new StringBuilder();
            for (Producto producto : productosSinConfiguracion) {
                if (listado.length() > 0) {
                    listado.append(", ");
                }
                listado.append(producto.getCodigo()).append(" (").append(producto.getNombre()).append(")");
            }
            throw new IncomeException("No se puede generar el G48: " + productosSinConfiguracion.size()
                + " producto(s) sin configuración de calificación de riesgo vigente al " + fechaFinDate
                + " en CRD.CFCR: " + listado + ". Configure la calificación de esos productos antes"
                + " de generar el reporte.");
        }

        // -------------------------------------------------------
        // 5b. Cargar datos del Grupo 2 en batch (igual que CCPM).
        // -------------------------------------------------------
        List<DetallePrestamo> cuotasGrupo2 = new ArrayList<>();
        for (DetallePrestamo d : cuotasAProcesar) {
            if (d.getPrestamo() != null && !prestamosEnGrupo1.contains(d.getPrestamo().getCodigo())) {
                cuotasGrupo2.add(d);
            }
        }
        System.out.println("G48 - Cuotas grupo 2 para optimizar: " + cuotasGrupo2.size());

        // Recolectar PKs de las cuotas origen del grupo 2 (sirve para ambos batch)
        List<Long> codigosCuotasMora = new ArrayList<>();
        for (DetallePrestamo cuota2 : cuotasGrupo2) {
            if (cuota2.getPrestamo() != null) {
                codigosCuotasMora.add(cuota2.getCodigo());
            }
        }

        // Batch suma capital/interés grupo 2
        Map<Long, Object[]> mapaSumasGrupo2 = new HashMap<>();
        if (!codigosCuotasMora.isEmpty()) {
            List<Object[]> sumasGrupo2 = detallePrestamoService.selectSumaCapitalInteresGrupo2Batch(
                codigosCuotasMora, fechaInicio, fechaFin
            );
            for (Object[] suma : sumasGrupo2) {
                Long codPrest = (Long) suma[0];
                mapaSumasGrupo2.put(codPrest, new Object[]{suma[1], suma[2]});
            }
            System.out.println("G48 - Sumas grupo 2 cargadas en batch: " + mapaSumasGrupo2.size());
        }

        // Batch mora grupo 2
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
            System.out.println("G48 - Moras grupo 2 cargadas en batch: " + mapaMoraGrupo2.size());
        }

        // Batch valorPorVencer Grupo 2:
        // saldoInicialCapital - capital de la cuota que cae DENTRO del mes de ejecución,
        // no de la cuota en mora que se está insertando.
        Map<Long, Double> mapaValorPorVencerGrupo2 = new HashMap<>();
        List<Long> codsPrestamosGrupo2 = new ArrayList<>();
        for (DetallePrestamo d : cuotasGrupo2) {
            if (d.getPrestamo() != null) codsPrestamosGrupo2.add(d.getPrestamo().getCodigo());
        }
        if (!codsPrestamosGrupo2.isEmpty()) {
            List<Object[]> saldosDelMes = detallePrestamoService.selectSaldoInicialCapitalDelMesBatch(
                codsPrestamosGrupo2, fechaInicio, fechaFin
            );
            for (Object[] fila : saldosDelMes) {
                Long   codPrest        = (Long)   fila[0];
                Double saldoInicialCap = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
                // CORRECCIÓN: valorPorVencer = saldoInicialCapital completo de la cuota del mes,
                // ya que esa cuota NO va a vencido (por política del fondo no está en mora hasta
                // que pase el último día del mes). El capital del mes queda en "por vencer".
                mapaValorPorVencerGrupo2.put(codPrest, saldoInicialCap);
            }
            System.out.println("G48 - ValorPorVencer Grupo 2 cargados en batch: " + mapaValorPorVencerGrupo2.size());
        }

        // -------------------------------------------------------
        // 6. Por cada cuota → generar un registro en G48
        // -------------------------------------------------------
        long contador = 0L;
        
        // Set para controlar que solo la primera cuota de cada entidad tenga valorTotalCuentaIndividual
        Set<String> entidadesConValorAsignado = new HashSet<>();

        for (DetallePrestamo cuota : cuotasAProcesar) {
            try {
                Prestamo prestamo = cuota.getPrestamo();
                if (prestamo == null) continue;

                // ¿Viene del grupo 1 o del grupo 2?
                boolean esGrupo1 = prestamosEnGrupo1.contains(prestamo.getCodigo());

                // Calcular días de morosidad (solo si estado != 4 pagada)
                Long diasMorosidad = 0L;
                if (cuota.getEstado() != null && !cuota.getEstado().equals(4L)) {
                    if (cuota.getFechaVencimiento() != null) {
                        LocalDate fechaVencimiento = cuota.getFechaVencimiento().toLocalDate();
                        diasMorosidad = ChronoUnit.DAYS.between(fechaVencimiento, fechaFinDate);
                        if (diasMorosidad < 0) diasMorosidad = 0L;
                    }
                }

                Long idProductoCuota = prestamo.getProducto() != null ? prestamo.getProducto().getCodigo() : null;
                ResultadoCalificacionRiesgo resultadoCalificacion = calificacionRiesgoService.calificar(
                    idProductoCuota, null, diasMorosidad, fechaFinDate);
                String calificacionPropia = resultadoCalificacion.getCalificacion();
                double porcentajeProvisionPropia = resultadoCalificacion.getPorcentajeProvision() != null
                    ? resultadoCalificacion.getPorcentajeProvision() : 0.0;

                // ----- VALOR POR VENCER -----
                // Grupo 1: saldoInicialCapital - capital de la cuota del mes (la cuota que se procesa).
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

                // ----- VALOR VENCIDO e INTERÉS ORDINARIO -----
                // Grupo 1 → 0 para ambos
                // Grupo 2 → suma de capital e interés desde la cuota incluida hasta la máxima con fechaVencimiento <= fechaFin
                Double valorVencido = 0.0;
                Double interesOrdinario = 0.0;
                
                if (esGrupo1) {
                    valorVencido = 0.0;
                    interesOrdinario = cuota.getInteres() != null ? cuota.getInteres() : 0.0;
                } else {
                    // Obtener del mapa pre-cargado en batch (grupo 2)
                    Object[] sumas = mapaSumasGrupo2.get(prestamo.getCodigo());
                    if (sumas != null) {
                        valorVencido     = sumas[0] != null ? (Double) sumas[0] : 0.0;
                        interesOrdinario = sumas[1] != null ? (Double) sumas[1] : 0.0;
                    }
                }

                // ----- VALOR TOTAL CUENTA INDIVIDUAL -----
                // Si el estado de la entidad es 30 (jubilado voluntario) → siempre 0
                // Solo se asigna en la primera cuota de cada entidad, el resto debe ser 0
                Double valorCuentaIndividual = 0.0;
                String identificacionEntidad = null;
                if (prestamo.getEntidad() != null) {
                    Long estadoEntidad = prestamo.getEntidad().getIdEstado();
                    
                    // Si es jubilado complementario, el valor es siempre 0
                    if (estadoEntidad != null
                            && estadoEntidad == EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO) {
                        valorCuentaIndividual = 0.0;
                    } else {
                        // Para otros estados, aplicar la lógica de primera cuota
                        identificacionEntidad = prestamo.getEntidad().getNumeroIdentificacion();
                        if (identificacionEntidad != null && !entidadesConValorAsignado.contains(identificacionEntidad)) {
                            // Primera cuota de esta entidad → asignar el valor
                            valorCuentaIndividual = mapaValorCuentaIndividual.getOrDefault(
                                prestamo.getEntidad().getCodigo(), 0.0
                            );
                            entidadesConValorAsignado.add(identificacionEntidad);
                        }
                        // Si ya se asignó antes, valorCuentaIndividual queda en 0
                    }
                }

                // ----- VALOR SUJETO A PROVISIÓN -----
                // max(0, (valorPorVencer + valorVencido) - valorTotalCuentaIndividual)
                Double valorSujetoProvision = Math.max(0.0, (valorPorVencer + valorVencido) - valorCuentaIndividual);

                // ----- PROVISIÓN REQUERIDA ORIGINAL -----
                Double provisionRequeridaOriginal = (valorSujetoProvision != null && valorSujetoProvision > 0.0)
                    ? valorSujetoProvision * porcentajeProvisionPropia : 0.0;
                
                // Log de depuración para identificar el problema
                if (valorCuentaIndividual == 0.0 && valorSujetoProvision > 0.0) {
                    System.out.println("G48 DEBUG Provision - Op: " + (prestamo.getIdAsoprep() != null ? prestamo.getIdAsoprep() : "null")
                        + " | diasMorosidad: " + diasMorosidad
                        + " | calificacion: " + calificacionPropia
                        + " | valorSujetoProvision: " + valorSujetoProvision
                        + " | provisionRequeridaOriginal: " + provisionRequeridaOriginal
                        + " | esperado: " + (valorSujetoProvision * 0.0099));
                }

                // ----- PROVISIÓN CONSTITUIDA -----
                // = provisionRequeridaOriginal del período anterior para esta operación
                String numeroOperacion = prestamo.getIdAsoprep() != null
                        ? String.valueOf(prestamo.getIdAsoprep()) : null;
                Double provisionConstituida = 0.0;
                if (numeroOperacion != null) {
                    if (mapaProvisionAnterior.containsKey(numeroOperacion)) {
                        provisionConstituida = mapaProvisionAnterior.get(numeroOperacion);
                    } else if (usarHistorico) {
                        // Consulta individual al histórico — evita cargar miles de registros
                        try {
                            HistoricoG48 hist = historicoG48Service.selectByNumeroOperacion(numeroOperacion);
                            if (hist != null && hist.getProvisionRequeridaOriginal() != null) {
                                provisionConstituida = hist.getProvisionRequeridaOriginal();
                                // Cachear para no repetir la consulta si la misma op. aparece de nuevo
                                mapaProvisionAnterior.put(numeroOperacion, provisionConstituida);
                            }
                        } catch (Throwable eh) {
                            System.out.println("G48 - HistoricoG48 no encontrado para op: " + numeroOperacion);
                        }
                    }
                }

                // Si provisionConstituida > valorSujetoProvision → limitar al valorSujetoProvision
                if (provisionConstituida > valorSujetoProvision) {
                    System.out.println("G48 - Op: " + numeroOperacion
                        + " provisionConstituida (" + provisionConstituida
                        + ") > valorSujetoProvision (" + valorSujetoProvision
                        + "), se ajusta al valorSujetoProvision");
                    provisionConstituida = valorSujetoProvision;
                }

                SaldoOperacionG48 g48 = new SaldoOperacionG48();

                g48.setTipoIdentificacion("C");
                if (prestamo.getEntidad() != null) {
                    g48.setIdentificacion(prestamo.getEntidad().getNumeroIdentificacion());
                }

                g48.setNumeroOperacion(numeroOperacion);

                if (prestamo.getProducto() != null && prestamo.getProducto().getNombre() != null
                        && !prestamo.getProducto().getNombre().isEmpty()) {
                    char inicialProducto = Character.toUpperCase(prestamo.getProducto().getNombre().charAt(0));
                    g48.setTipoCredito(String.valueOf(inicialProducto == 'E' ? 'Q' : inicialProducto));
                }

                g48.setDiasMorosidad(diasMorosidad);
                g48.setCalificacionPropia(calificacionPropia);
                // Si la tasa nominal es 0 o nula, se usa 9 como valor por defecto
                double tasaInteres = (prestamo.getInteresNominal() != null && prestamo.getInteresNominal() > 0.0)
                        ? prestamo.getInteresNominal() : 9.0;
                g48.setTasaInteres(tasaInteres);
                g48.setValorPorVencer(valorPorVencer);
                g48.setValorVencido(valorVencido);
                g48.setCostosOperativos(0.0);
                g48.setInteresOrdinario(interesOrdinario);

                // ----- INTERÉS MORA -----
                // Grupo 1 → 0 (cuotas del mes, no están vencidas aún)
                // Grupo 2 → del mapa pre-cargado en batch
                Double interesMora = 0.0;
                if (!esGrupo1) {
                    interesMora = mapaMoraGrupo2.getOrDefault(cuota.getCodigo(), 0.0);
                }
                g48.setInteresMora(interesMora);
                g48.setValorDemandaJudicial(0.0);
                g48.setCarteraCastigada(0.0);
                g48.setValorTotalCuentaIndividual(valorCuentaIndividual);
                g48.setValorSujetoProvision(valorSujetoProvision);
                g48.setProvisionRequeridaOriginal(provisionRequeridaOriginal);
                g48.setProvisionConstituida(provisionConstituida);
                g48.setTipoSistemaAmortizacion("FR");
                g48.setCuotaCredito(cuota.getCuota() != null ? cuota.getCuota() : 0.0);
                // El dividendo es la cuota a cobrar SIN mora: desde que existe el proceso diario
                // de mora (ProcesoMoraPrestamoService) DTPRTTLL la incluye, y este reporte ya
                // lleva la mora en su propia columna (mapaMoraGrupo2). Restarla mantiene el G48
                // exactamente igual que antes y evita reportarla dos veces.
                g48.setDividendo(dividendoSinMora(cuota));

                if (cuota.getFechaVencimiento() != null) {
                    g48.setFechaExigibilidad(cuota.getFechaVencimiento().toLocalDate());
                }

                g48.setDetalleEjecucion(detalle);

                cg48DaoService.save(g48, null);
                System.out.println("G48 INSERT prestamo: " + prestamo.getIdAsoprep()
                    + " cuota: " + cuota.getNumeroCuota()
                    + " grupo: " + (esGrupo1 ? "1" : "2")
                    + " diasMora: " + diasMorosidad
                    + " calif: " + calificacionPropia
                    + " vsap: " + valorSujetoProvision
                    + " prov: " + provisionRequeridaOriginal);
                contador++;

            } catch (Throwable e) {
                System.out.println("G48 ERROR cuota " + cuota.getCodigo() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("G48 generado con " + contador + " registros");
        return contador;
    }

    /**
     * Carga en el mapa el campo provisionRequeridaOriginal del G48 del período anterior,
     * indexado por numeroOperacion.
     * @return false si el mapa fue poblado desde el EJRC anterior (no necesita histórico),
     *         true si no hay EJRC anterior y se debe consultar HistoricoG48 individualmente.
     */
    private boolean cargarProvisionAnterior(EjecucionReporte ejrcActual, long mes, long anio,
            Map<String, Double> mapa) {
        try {
            long mesPrevio  = mes == 1 ? 12 : mes - 1;
            long anioPrevio = mes == 1 ? anio - 1 : anio;

            List<EjecucionReporte> listaPrev = ejrcService.selectByMesAnio(mesPrevio, anioPrevio);
            if (listaPrev != null && !listaPrev.isEmpty()) {
                EjecucionReporte ejrcPrev = listaPrev.get(0);
                DetalleEjecucionReporte ejrdG48Prev = ejrdService.selectByEjecucionYTipo(
                    ejrcPrev.getCodigo(), "G48"
                );
                if (ejrdG48Prev != null) {
                    List<SaldoOperacionG48> prevRegistros = saldoG48Service.selectByDetalle(ejrdG48Prev.getCodigo());
                    for (SaldoOperacionG48 prev : prevRegistros) {
                        if (prev.getNumeroOperacion() != null && prev.getProvisionRequeridaOriginal() != null) {
                            mapa.put(prev.getNumeroOperacion(), prev.getProvisionRequeridaOriginal());
                        }
                    }
                    System.out.println("G48 - Provision anterior cargada desde G48 previo: " + mapa.size());
                    return false; // mapa completo, no usar histórico
                }
            }
        } catch (Throwable e) {
            System.out.println("G48 - No se pudo cargar provision del EJRC anterior: " + e.getMessage());
        }
        // No hay EJRC anterior con G48 → consultar HistoricoG48 individualmente
        System.out.println("G48 - Sin EJRC anterior, se consultará HistoricoG48 por operación.");
        return true;
    }

    // calcularProvision/calcularCalificacion (las dos tablas cableadas, literal 7/8/21 para
    // hipotecario) se eliminaron el 2026-09-02 — PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md.
    // La escala y el porcentaje de provisión ahora se resuelven contra CRD.CFCR/CRD.ESCR vía
    // CalificacionRiesgoService (ver el punto único arriba, en el bucle principal). El
    // contenido exacto de las dos tablas que tenían estos métodos quedó cargado tal cual en
    // sql/177 — están en el historial de git si hiciera falta compararlas letra por letra.

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
