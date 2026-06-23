package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.ValorPagoPensionComplementariaService;
import com.saa.ejb.rpr.dao.ParticipeJubiladoG44DaoService;
import com.saa.ejb.rpr.service.GeneracionG44Service;
import com.saa.ejb.rpr.service.HistoricoG44Service;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.ValorPagoPensionComplementaria;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.HistoricoG44;
import com.saa.model.rpr.ParticipeJubiladoG44;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG44ServiceImpl implements GeneracionG44Service {

    private static final Long ESTADO_JUBILADO = 30L;

    @EJB private EntidadService                       entidadService;
    @EJB private AporteService                        aporteService;
    @EJB private DetallePrestamoService               detallePrestamoService;
    @EJB private ValorPagoPensionComplementariaService vppcService;
    @EJB private ParticipeJubiladoG44DaoService       cg44DaoService;
    @EJB private HistoricoG44Service                  historicoG44Service;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G44");

        // -------------------------------------------------------
        // 0. Calcular fechaCorte = último día del mes de ejecución a las 23:59:59
        // -------------------------------------------------------
        long mes      = detalle.getEjecucionReporte().getMes();
        long anio     = detalle.getEjecucionReporte().getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();
        LocalDateTime fechaCorte = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);
        System.out.println("G44 - fechaCorte: " + fechaCorte);

        // -------------------------------------------------------
        // 1. Obtener todas las entidades con idEstado = 30 (jubilados)
        // -------------------------------------------------------
        List<Entidad> jubilados = entidadService.selectByIdEstado(ESTADO_JUBILADO);
        System.out.println("G44 - Entidades jubiladas (idEstado=30): " + jubilados.size());

        if (jubilados.isEmpty()) {
            System.out.println("G44 - Sin jubilados. G44 vacio, OK.");
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

        System.out.println("G44 - Saldos cuenta en BD: " + mapaSaldo.size() + " entidades");

        // COUNT de imposiciones por entidad (aportes tipo 9, 11 con tipoAporte.estado=1)
        List<Object[]> listaImposiciones = aporteService.selectCountImposicionesJubilacionPorEntidad(fechaCorte);
        Map<Long, Long> mapaImposiciones = new HashMap<>();
        for (Object[] fila : listaImposiciones) {
            Long   codigoEntidad = toLong(fila[0]);
            Long   count         = toLong(fila[1]);
            if (codigoEntidad != null && count != null) {
                mapaImposiciones.put(codigoEntidad, count);
            }
        }
        System.out.println("G44 - Imposiciones contadas en BD: " + mapaImposiciones.size() + " entidades");

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
        System.out.println("G44 - Cuotas pagadas en BD: " + mapaCuotasPagadas.size() + " entidades");

        // -------------------------------------------------------
        // 3. Por cada jubilado → obtener valorPension desde VPPC e INSERT en CG44
        // -------------------------------------------------------
        long contador = 0L;

        // Fecha de referencia: ejecuciones desde enero 2026 en adelante
        LocalDate fechaLimite2026 = LocalDate.of(2026, 1, 1);
        boolean esDesde2026 = (anio > 2026) || (anio == 2026 && mes >= 1);

        for (Entidad entidad : jubilados) {
            Long codigoEntidad = entidad.getCodigo();

            // valorPension → valorPagar de VPPC
            Double valorPension = 0.0;
            List<ValorPagoPensionComplementaria> vppcList = vppcService.selectByEntidad(codigoEntidad);
            if (vppcList != null && !vppcList.isEmpty() && vppcList.get(0).getValorPagar() != null) {
                valorPension = vppcList.get(0).getValorPagar();
            }

            // Exclusión: valorPension = 0 → NO incluir
            if (valorPension == 0.0) {
                System.out.println("G44 - SKIP jubilado " + entidad.getNumeroIdentificacion() + " (valorPension=0)");
                continue;
            }

            // fechaJubilacion e imposicionesAcumuladas → buscar en HistoricoG44 por cédula
            LocalDate fechaJubilacion = null;
            Long imposicionesAcumuladas = null;
            List<HistoricoG44> historicoList = historicoG44Service.selectByIdentificacion(entidad.getNumeroIdentificacion());
            if (historicoList != null && !historicoList.isEmpty()) {
                HistoricoG44 hist = historicoList.get(0);
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
                        System.out.println("G44 - fechaJubilacion no parseable para cedula: "
                            + entidad.getNumeroIdentificacion() + " valor en BD: [" + rawFecha + "]");
                    }
                }
            }

            // Si no hay histórico o imposicionesAcumuladas es null/0, obtener del conteo de aportes
            if (imposicionesAcumuladas == null || imposicionesAcumuladas == 0L) {
                imposicionesAcumuladas = mapaImposiciones.getOrDefault(codigoEntidad, 0L);
            }

            // Regla fecha jubilacion nula:
            // - Ejecución anterior a 01/01/2026 → omitir el registro
            // - Ejecución desde 01/01/2026 en adelante → usar 01/01/2026 como fecha de jubilacion
            if (fechaJubilacion == null) {
                if (!esDesde2026) {
                    System.out.println("G44 SKIP jubilado sin fechaJubilacion (ejecución antes 2026): "
                        + entidad.getNumeroIdentificacion());
                    continue;
                }
                fechaJubilacion = fechaLimite2026;
                System.out.println("G44 - fechaJubilacion nula, se asigna 01/01/2026 para: "
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

            ParticipeJubiladoG44 jubilado = new ParticipeJubiladoG44();
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
            jubilado.setDetalleEjecucion(detalle);

            cg44DaoService.save(jubilado, null);
            System.out.println("G44 INSERT jubilado: " + entidad.getNumeroIdentificacion()
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
            exJubiladosHist = new java.util.ArrayList<>();
            System.out.println("G44 - selectExJubilados error: " + e.getMessage());
        }
        System.out.println("G44 - Ex-jubilados desde HistoricoG44: " + exJubiladosHist.size());

        if (!exJubiladosHist.isEmpty()) {
            // Los mapas mapaAportesMes y mapaCuotasPagadas ya están cargados arriba

            for (HistoricoG44 hist : exJubiladosHist) {
                String identificacion = hist.getIdentificacion();

                Entidad entidad = null;
                try {
                    entidad = entidadService.selectByNumeroIdentificacion(identificacion);
                } catch (Throwable e) {
                    System.out.println("G44 - entidad no encontrada para ex-jubilado: " + identificacion);
                    continue;
                }
                if (entidad == null) continue;

                Long codigoEntidad = entidad.getCodigo();
                Double valorPension    = Math.abs(mapaAportesMes.getOrDefault(codigoEntidad, 0.0));
                Double saldoTotal      = mapaSaldo.getOrDefault(codigoEntidad, 0.0);
                Double sumaCuotas      = mapaCuotasPagadas.getOrDefault(codigoEntidad, 0.0);

                // Exclusión: valorPension = 0 → NO incluir
                // Para ex-jubilados, también excluir si saldoCuenta = 0 y valorPension = 0
                if (valorPension == 0.0) {
                    System.out.println("G44 - SKIP ex-jubilado " + identificacion + " (valorPension=0, saldoCuenta=" + saldoTotal + ")");
                    continue;
                }

                // Calcular valoresCompensados y valorNetoRecibir según los 4 casos
                Double valoresCompensados;
                Double valorNetoRecibir;

                if (sumaCuotas == 0.0) {
                    // Caso 1: suma de cuotas = 0
                    valoresCompensados = 0.0;
                    valorNetoRecibir   = valorPension;
                } else if (sumaCuotas < valorPension) {
                    // Caso 2: suma < valorPension
                    valoresCompensados = sumaCuotas;
                    valorNetoRecibir   = valorPension - valoresCompensados;
                } else if (sumaCuotas > valorPension) {
                    // Caso 3: suma > valorPension
                    valoresCompensados = sumaCuotas - valorPension;
                    valorNetoRecibir   = 0.0;
                } else {
                    // Caso 4: suma == valorPension
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

                // Regla fecha jubilacion nula:
                // - Ejecución anterior a 01/01/2026 → omitir el registro
                // - Ejecución desde 01/01/2026 en adelante → usar 01/01/2026 como fecha de jubilacion
                if (fechaJubilacion == null) {
                    if (!esDesde2026) {
                        System.out.println("G44 SKIP ex-jubilado sin fechaJubilacion (ejecución antes 2026): "
                            + identificacion);
                        continue;
                    }
                    fechaJubilacion = fechaLimite2026;
                    System.out.println("G44 - fechaJubilacion nula, se asigna 01/01/2026 para ex-jubilado: "
                        + identificacion);
                }

                // Obtener imposicionesAcumuladas del histórico o contarlas desde aportes
                Long imposicionesAcumuladas = hist.getImposicionesAcumuladas();
                if (imposicionesAcumuladas == null || imposicionesAcumuladas == 0L) {
                    imposicionesAcumuladas = mapaImposiciones.getOrDefault(codigoEntidad, 0L);
                }

                ParticipeJubiladoG44 exJubilado = new ParticipeJubiladoG44();
                exJubilado.setIdentificacion(identificacion);
                exJubilado.setTipoIdentificacion("C");
                exJubilado.setTipoJubilacion("V");
                exJubilado.setFechaJubilacion(fechaJubilacion);
                exJubilado.setImposicionesAcumuladas(imposicionesAcumuladas != null ? imposicionesAcumuladas : 0L);
                exJubilado.setValorPension(valorPension);
                exJubilado.setValorNetoRecibir(valorNetoRecibir);
                exJubilado.setValoresCompensados(valoresCompensados);
                exJubilado.setSaldoCuenta(saldoTotal);
                exJubilado.setJubilacionIess("S");
                exJubilado.setDetalleEjecucion(detalle);

                cg44DaoService.save(exJubilado, null);
                System.out.println("G44 INSERT ex-jubilado: " + identificacion
                    + " (valorPension=" + valorPension + ", sumaCuotas=" + sumaCuotas
                    + ", valoresCompensados=" + valoresCompensados + ", valorNetoRecibir=" + valorNetoRecibir
                    + ", saldoCuenta=" + saldoTotal + ")");
                contador++;
            }
        }

        System.out.println("G44 generado TOTAL con " + contador + " registros");
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