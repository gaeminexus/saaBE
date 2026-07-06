package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.ValorPagoPensionComplementariaService;
import com.saa.ejb.rpr.dao.CreditoJubiladosMensualDaoService;
import com.saa.ejb.rpr.service.GeneracionCJBMService;
import com.saa.ejb.rpr.service.HistoricoG44Service;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.ValorPagoPensionComplementaria;
import com.saa.model.rpr.CreditoJubiladosMensual;
import com.saa.model.rpr.EjecucionReporteCartera;
import com.saa.model.rpr.HistoricoG44;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Servicio de generación del reporte CJBM (Crédito Jubilados Mensual).
 * Lógica idéntica al G44 - calcula datos de jubilados.
 */
@Stateless
public class GeneracionCJBMServiceImpl implements GeneracionCJBMService {

    private static final Long ESTADO_JUBILADO = 30L;

    @EJB private EntidadService                       entidadService;
    @EJB private AporteService                        aporteService;
    @EJB private DetallePrestamoService               detallePrestamoService;
    @EJB private ValorPagoPensionComplementariaService vppcService;
    @EJB private CreditoJubiladosMensualDaoService    cjbmDaoService;
    @EJB private HistoricoG44Service                  historicoG44Service;

    @Override
    public long generar(EjecucionReporteCartera ejecucion) throws Throwable {
        System.out.println("Ingresa al metodo generar CJBM");

        // -------------------------------------------------------
        // 0. Calcular fechaCorte = último día del mes de ejecución a las 23:59:59
        // -------------------------------------------------------
        long mes      = ejecucion.getMes();
        long anio     = ejecucion.getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();
        LocalDateTime fechaCorte = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);
        System.out.println("CJBM - fechaCorte: " + fechaCorte);

        // -------------------------------------------------------
        // 1. Obtener todas las entidades con idEstado = 30 (jubilados)
        // -------------------------------------------------------
        List<Entidad> jubilados = entidadService.selectByIdEstado(ESTADO_JUBILADO);
        System.out.println("CJBM - Entidades jubiladas (idEstado=30): " + jubilados.size());

        if (jubilados.isEmpty()) {
            System.out.println("CJBM - Sin jubilados. CJBM vacio, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Ejecutar SELECT con SUM en BD filtrado por fechaCorte
        // -------------------------------------------------------

        // SUM de aportes.valor con tipoAporte.codigo = 23 → saldoCuenta
        List<Object[]> listaSaldo = aporteService.selectSumaSaldoCuentaJubilacionPorEntidad(fechaCorte);
        Map<Long, Double> mapaSaldo = new HashMap<>();
        for (Object[] fila : listaSaldo) {
            Long   codigoEntidad = toLong(fila[0]);
            Double suma          = toDouble(fila[1]);
            if (codigoEntidad != null && suma != null) {
                mapaSaldo.put(codigoEntidad, suma);
            }
        }

        System.out.println("CJBM - Saldos cuenta en BD: " + mapaSaldo.size() + " entidades");

        // Calcular fechaInicio del mes (necesario para cuotas y aportes del mes)
        LocalDateTime fechaInicio = LocalDateTime.of((int) anio, (int) mes, 1, 0, 0, 0);

        // SUM aportes tipo 23 en el mes (para valorPension de ex-jubilados y cuotas)
        List<Object[]> sumaAportesMesList = aporteService.selectSumaAportesTipo23EnRango(fechaInicio, fechaCorte);
        Map<Long, Double> mapaAportesMes = new HashMap<>();
        for (Object[] fila : sumaAportesMesList) {
            Long cod = toLong(fila[0]); Double suma = toDouble(fila[1]);
            if (cod != null && suma != null) mapaAportesMes.put(cod, suma);
        }

        // SUM cuotas pagadas (estado=4) con fechaVencimiento en el mes
        List<Object[]> sumaCuotasList = detallePrestamoService.selectSumaCuotasPagadasPorEntidad(fechaInicio, fechaCorte);
        Map<Long, Double> mapaCuotasPagadas = new HashMap<>();
        for (Object[] fila : sumaCuotasList) {
            Long cod = toLong(fila[0]); Double suma = toDouble(fila[1]);
            if (cod != null && suma != null) mapaCuotasPagadas.put(cod, suma);
        }
        System.out.println("CJBM - Cuotas pagadas en BD: " + mapaCuotasPagadas.size() + " entidades");

        // -------------------------------------------------------
        // 2b. Cargar todos los VPPC y HistoricoCJBM en batch (optimización N+1)
        // -------------------------------------------------------
        List<Long> codigosEntidadesJubilados = new ArrayList<>();
        List<String> identificacionesJubilados = new ArrayList<>();
        for (Entidad j : jubilados) {
            codigosEntidadesJubilados.add(j.getCodigo());
            identificacionesJubilados.add(j.getNumeroIdentificacion());
        }

        // Cargar todos los VPPC de una vez
        List<ValorPagoPensionComplementaria> vppcList = vppcService.selectByEntidadesIn(codigosEntidadesJubilados);
        Map<Long, Double> mapaValorPension = new HashMap<>();
        Map<Long, Double> mapaValorSeguro  = new HashMap<>();
        for (ValorPagoPensionComplementaria vppc : vppcList) {
            if (vppc.getEntidad() != null) {
                Long codEntidad = vppc.getEntidad().getCodigo();
                // Guardar solo el primer registro por entidad
                if (!mapaValorPension.containsKey(codEntidad)) {
                    mapaValorPension.put(codEntidad, vppc.getValorPagar() != null ? vppc.getValorPagar() : 0.0);
                    mapaValorSeguro.put(codEntidad,  vppc.getValorSeguro() != null ? vppc.getValorSeguro() : 0.0);
                }
            }
        }
        System.out.println("CJBM - VPPC cargados en batch: " + mapaValorPension.size() + " entidades");

        // Cargar todos los HistoricoG44 de una vez (misma tabla que usa G44)
        List<HistoricoG44> historicoList = historicoG44Service.selectByIdentificacionesIn(identificacionesJubilados);
        Map<String, HistoricoG44> mapaHistorico = new HashMap<>();
        for (HistoricoG44 hist : historicoList) {
            if (hist.getIdentificacion() != null) {
                // Guardar solo el primero por identificación
                if (!mapaHistorico.containsKey(hist.getIdentificacion())) {
                    mapaHistorico.put(hist.getIdentificacion(), hist);
                }
            }
        }
        System.out.println("CJBM - HistoricoG44 cargados en batch: " + mapaHistorico.size() + " identificaciones");

        // -------------------------------------------------------
        // 3. Por cada jubilado → obtener valorPension desde VPPC e INSERT en CJBM
        // -------------------------------------------------------
        long contador = 0L;

        // Fecha de referencia: ejecuciones desde enero 2026 en adelante
        LocalDate fechaLimite2026 = LocalDate.of(2026, 1, 1);
        boolean esDesde2026 = (anio > 2026) || (anio == 2026 && mes >= 1);

        for (Entidad entidad : jubilados) {
            Long codigoEntidad = entidad.getCodigo();

            // valorPension → obtener del mapa pre-cargado
            Double valorPension = mapaValorPension.getOrDefault(codigoEntidad, 0.0);

            // fechaJubilacion e imposicionesAcumuladas → buscar en el mapa pre-cargado
            LocalDate fechaJubilacion = null;
            Long imposicionesAcumuladas = null;
            HistoricoG44 hist = mapaHistorico.get(entidad.getNumeroIdentificacion());
            if (hist != null) {
                imposicionesAcumuladas = hist.getImposicionesAcumuladas();
                if (hist.getFechaJubilacion() != null) {
                    String rawFecha = hist.getFechaJubilacion().trim();
                    java.time.format.DateTimeFormatter[] formatos = {
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                        java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy")
                    };
                    for (java.time.format.DateTimeFormatter fmt : formatos) {
                        try { fechaJubilacion = LocalDate.parse(rawFecha, fmt); break; }
                        catch (Exception ex) { /* siguiente formato */ }
                    }
                    if (fechaJubilacion == null) {
                        System.out.println("CJBM - fechaJubilacion no parseable para cedula: "
                            + entidad.getNumeroIdentificacion() + " valor en BD: [" + rawFecha + "]");
                    }
                }
            }

            // Regla fecha jubilacion nula:
            // - Ejecución anterior a 01/01/2026 → omitir el registro
            // - Ejecución desde 01/01/2026 en adelante → usar 01/01/2026 como fecha de jubilacion
            if (fechaJubilacion == null) {
                if (!esDesde2026) {
                    System.out.println("CJBM SKIP jubilado sin fechaJubilacion (ejecución antes 2026): "
                        + entidad.getNumeroIdentificacion());
                    continue;
                }
                fechaJubilacion = fechaLimite2026;
                System.out.println("CJBM - fechaJubilacion nula, se asigna 01/01/2026 para: "
                    + entidad.getNumeroIdentificacion());
            }

            // Aplicar 4 casos de valoresCompensados y valorNetoRecibir
            Double sumaCuotas = mapaCuotasPagadas.getOrDefault(codigoEntidad, 0.0);
            Double valoresCompensados;
            Double valorNetoRecibir;

            if (sumaCuotas == 0.0) {
                valoresCompensados = 0.0;
                valorNetoRecibir   = valorPension;
            } else if (sumaCuotas < valorPension) {
                valoresCompensados = sumaCuotas;
                valorNetoRecibir   = valorPension - valoresCompensados;
            } else if (sumaCuotas > valorPension) {
                valoresCompensados = sumaCuotas - valorPension;
                valorNetoRecibir   = 0.0;
            } else {
                valoresCompensados = sumaCuotas;
                valorNetoRecibir   = 0.0;
            }

            CreditoJubiladosMensual jubilado = new CreditoJubiladosMensual();
            jubilado.setIdentificacion(entidad.getNumeroIdentificacion());
            jubilado.setTipoIdentificacion("C");
            jubilado.setTipoJubilacion("V");
            jubilado.setFechaJubilacion(fechaJubilacion);
            jubilado.setImposicionesAcumuladas(imposicionesAcumuladas != null ? imposicionesAcumuladas : 0L);
            jubilado.setValorPension(valorPension);
            jubilado.setValorNetoRecibir(valorNetoRecibir);
            jubilado.setValoresCompensados(valoresCompensados);
            jubilado.setSaldoCuenta(mapaSaldo.getOrDefault(codigoEntidad, 0.0));
            jubilado.setJubilacionIess("S");
            jubilado.setValorJubilacion(mapaValorPension.getOrDefault(codigoEntidad, 0.0));
            jubilado.setValorSeguro(mapaValorSeguro.getOrDefault(codigoEntidad, 0.0));
            jubilado.setEjecucionReporte(ejecucion);

            cjbmDaoService.save(jubilado, null);
            System.out.println("CJBM INSERT jubilado: " + entidad.getNumeroIdentificacion()
                + " (valorPension=" + valorPension + ", sumaCuotas=" + sumaCuotas
                + ", valoresCompensados=" + valoresCompensados + ", valorNetoRecibir=" + valorNetoRecibir + ")");
            contador++;
        }

        // -------------------------------------------------------
        // 4. Ex-jubilados: en HistoricoG44 con estado != 30 en ENTD
        // -------------------------------------------------------
        List<HistoricoG44> exJubiladosHist = null;
        try {
            exJubiladosHist = historicoG44Service.selectExJubilados();
        } catch (Throwable e) {
            exJubiladosHist = new ArrayList<>();
            System.out.println("CJBM - selectExJubilados error: " + e.getMessage());
        }
        System.out.println("CJBM - Ex-jubilados desde HistoricoG44: " + exJubiladosHist.size());

        if (!exJubiladosHist.isEmpty()) {
            for (HistoricoG44 hist : exJubiladosHist) {
                String identificacion = hist.getIdentificacion();

                Entidad entidad = null;
                try {
                    entidad = entidadService.selectByNumeroIdentificacion(identificacion);
                } catch (Throwable e) {
                    System.out.println("CJBM - entidad no encontrada para ex-jubilado: " + identificacion);
                    continue;
                }
                if (entidad == null) continue;

                Long codigoEntidad = entidad.getCodigo();
                Double valorPension    = Math.abs(mapaAportesMes.getOrDefault(codigoEntidad, 0.0));
                Double saldoTotal      = mapaSaldo.getOrDefault(codigoEntidad, 0.0);
                Double sumaCuotas      = mapaCuotasPagadas.getOrDefault(codigoEntidad, 0.0);

                // Exclusión: saldoCuenta = 0 y valorPension = 0 → NO incluir
                if (saldoTotal == 0.0 && valorPension == 0.0) {
                    System.out.println("CJBM - SKIP ex-jubilado " + identificacion + " (saldoCuenta=0 y valorPension=0)");
                    continue;
                }

                // Calcular valoresCompensados y valorNetoRecibir según los 4 casos
                Double valoresCompensados;
                Double valorNetoRecibir;

                if (sumaCuotas == 0.0) {
                    valoresCompensados = 0.0;
                    valorNetoRecibir   = valorPension;
                } else if (sumaCuotas < valorPension) {
                    valoresCompensados = sumaCuotas;
                    valorNetoRecibir   = valorPension - valoresCompensados;
                } else if (sumaCuotas > valorPension) {
                    valoresCompensados = sumaCuotas - valorPension;
                    valorNetoRecibir   = 0.0;
                } else {
                    valoresCompensados = sumaCuotas;
                    valorNetoRecibir   = 0.0;
                }

                // Parsear fechaJubilacion
                LocalDate fechaJubilacion = null;
                if (hist.getFechaJubilacion() != null) {
                    String rawFecha = hist.getFechaJubilacion().trim();
                    java.time.format.DateTimeFormatter[] formatos = {
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                        java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy")
                    };
                    for (java.time.format.DateTimeFormatter fmt : formatos) {
                        try { fechaJubilacion = LocalDate.parse(rawFecha, fmt); break; }
                        catch (Exception ex) { /* siguiente formato */ }
                    }
                }

                if (fechaJubilacion == null) {
                    if (!esDesde2026) {
                        System.out.println("CJBM SKIP ex-jubilado sin fechaJubilacion (ejecución antes 2026): "
                            + identificacion);
                        continue;
                    }
                    fechaJubilacion = fechaLimite2026;
                }

                CreditoJubiladosMensual exJubilado = new CreditoJubiladosMensual();
                exJubilado.setIdentificacion(identificacion);
                exJubilado.setTipoIdentificacion("C");
                exJubilado.setTipoJubilacion("V");
                exJubilado.setFechaJubilacion(fechaJubilacion);
                exJubilado.setImposicionesAcumuladas(hist.getImposicionesAcumuladas() != null ? hist.getImposicionesAcumuladas() : 0L);
                exJubilado.setValorPension(valorPension);
                exJubilado.setValorNetoRecibir(valorNetoRecibir);
                exJubilado.setValoresCompensados(valoresCompensados);
                exJubilado.setSaldoCuenta(saldoTotal);
                exJubilado.setJubilacionIess("S");
                exJubilado.setValorJubilacion(mapaValorPension.getOrDefault(codigoEntidad, 0.0));
                exJubilado.setValorSeguro(mapaValorSeguro.getOrDefault(codigoEntidad, 0.0));
                exJubilado.setEjecucionReporte(ejecucion);

                cjbmDaoService.save(exJubilado, null);
                System.out.println("CJBM INSERT ex-jubilado: " + identificacion);
                contador++;
            }
        }

        System.out.println("CJBM generado TOTAL con " + contador + " registros");
        return contador;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        return ((Number) obj).longValue();
    }

    private Double toDouble(Object obj) {
        if (obj == null) return null;
        return ((Number) obj).doubleValue();
    }
}
