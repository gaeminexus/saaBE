package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.AporteService;
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

        // -------------------------------------------------------
        // 3. Por cada jubilado → obtener valorPension desde VPPC e INSERT en CG44
        // -------------------------------------------------------
        long contador = 0L;

        for (Entidad entidad : jubilados) {
            Long codigoEntidad = entidad.getCodigo();

            // valorPension y valorNetoRecibir → valorPagar de VPPC
            Double valorPension = null;
            List<ValorPagoPensionComplementaria> vppcList = vppcService.selectByEntidad(codigoEntidad);
            if (vppcList != null && !vppcList.isEmpty()) {
                valorPension = vppcList.get(0).getValorPagar();
            }

            // fechaJubilacion e imposicionesAcumuladas → buscar en HistoricoG44 por cédula
            java.time.LocalDate fechaJubilacion = null;
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
                        try {
                            fechaJubilacion = java.time.LocalDate.parse(rawFecha, fmt);
                            break;
                        } catch (Exception ex) {
                            // intentar siguiente formato
                        }
                    }
                    if (fechaJubilacion == null) {
                        System.out.println("G44 - fechaJubilacion no parseable para cedula: "
                            + entidad.getNumeroIdentificacion() + " valor en BD: [" + rawFecha + "]");
                    }
                }
            }

            ParticipeJubiladoG44 jubilado = new ParticipeJubiladoG44();
            jubilado.setIdentificacion(entidad.getNumeroIdentificacion());
            jubilado.setTipoIdentificacion("C");
            jubilado.setTipoJubilacion("V");
            jubilado.setFechaJubilacion(fechaJubilacion);
            jubilado.setImposicionesAcumuladas(imposicionesAcumuladas != null ? imposicionesAcumuladas : 0L);
            jubilado.setValorPension(valorPension);
            jubilado.setValorNetoRecibir(valorPension);
            jubilado.setSaldoCuenta(mapaSaldo.getOrDefault(codigoEntidad, 0.0));
            jubilado.setJubilacionIess("S");
            jubilado.setDetalleEjecucion(detalle);

            cg44DaoService.save(jubilado, null);
            System.out.println("G44 INSERT jubilado: " + entidad.getNumeroIdentificacion());
            contador++;
        }

        // -------------------------------------------------------
        // 4. Ex-jubilados: en HistoricoG44 con estado != 30 en ENTD
        // -------------------------------------------------------
        LocalDateTime fechaInicio = LocalDateTime.of((int) anio, (int) mes, 1, 0, 0, 0);

        List<HistoricoG44> exJubiladosHist = null;
        try {
            exJubiladosHist = historicoG44Service.selectExJubilados();
        } catch (Throwable e) {
            exJubiladosHist = new java.util.ArrayList<>();
            System.out.println("G44 - selectExJubilados error: " + e.getMessage());
        }
        System.out.println("G44 - Ex-jubilados desde HistoricoG44: " + exJubiladosHist.size());

        if (!exJubiladosHist.isEmpty()) {
            // Cargar sumas de aportes tipo 23 en el mes
            List<Object[]> sumaEnMesList = aporteService.selectSumaAportesTipo23EnRango(fechaInicio, fechaCorte);
            Map<Long, Double> mapaAportesMes = new HashMap<>();
            for (Object[] fila : sumaEnMesList) {
                Long cod = toLong(fila[0]);
                Double suma = toDouble(fila[1]);
                if (cod != null && suma != null) mapaAportesMes.put(cod, suma);
            }

            for (HistoricoG44 hist : exJubiladosHist) {
                String identificacion = hist.getIdentificacion();

                // Obtener codigo de entidad para buscar en el mapa
                Entidad entidad = null;
                try {
                    entidad = entidadService.selectByNumeroIdentificacion(identificacion);
                } catch (Throwable e) {
                    System.out.println("G44 - entidad no encontrada para ex-jubilado: " + identificacion);
                    continue;
                }
                if (entidad == null) continue;

                Long codigoEntidad = entidad.getCodigo();
                Double sumaEnMes  = mapaAportesMes.getOrDefault(codigoEntidad, 0.0);
                Double saldoTotal = mapaSaldo.getOrDefault(codigoEntidad, 0.0);

                // Exclusión: sin saldo y sin aportes en el mes → NO incluir
                if (saldoTotal == 0.0 && sumaEnMes == 0.0) {
                    System.out.println("G44 - SKIP ex-jubilado " + identificacion + " (sin saldo ni aportes en el mes)");
                    continue;
                }

                // Parsear fechaJubilacion
                java.time.LocalDate fechaJubilacion = null;
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
                        try { fechaJubilacion = java.time.LocalDate.parse(rawFecha, fmt); break; }
                        catch (Exception ex) { /* siguiente formato */ }
                    }
                }

                ParticipeJubiladoG44 exJubilado = new ParticipeJubiladoG44();
                exJubilado.setIdentificacion(identificacion);
                exJubilado.setTipoIdentificacion("C");
                exJubilado.setTipoJubilacion("V");
                exJubilado.setFechaJubilacion(fechaJubilacion);
                exJubilado.setImposicionesAcumuladas(hist.getImposicionesAcumuladas() != null ? hist.getImposicionesAcumuladas() : 0L);
                exJubilado.setValorPension(sumaEnMes);
                exJubilado.setValorNetoRecibir(sumaEnMes);
                exJubilado.setValoresCompensados(sumaEnMes);
                exJubilado.setSaldoCuenta(saldoTotal);
                exJubilado.setJubilacionIess("S");
                exJubilado.setDetalleEjecucion(detalle);

                cg44DaoService.save(exJubilado, null);
                System.out.println("G44 INSERT ex-jubilado: " + identificacion + " (sumaEnMes=" + sumaEnMes + ", saldoTotal=" + saldoTotal + ")");
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