package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.EstadoParticipeService;
import com.saa.ejb.rpr.dao.CreditoParticipesMensualDaoService;
import com.saa.ejb.rpr.service.GeneracionCPRMService;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.EstadoParticipe;
import com.saa.model.crd.TipoAporte;
import com.saa.model.rpr.CreditoParticipesMensual;
import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Servicio de generación del reporte CPRM (Crédito Partícipes Mensual).
 *
 * A diferencia del G42 (un registro por entidad con columnas por tipo de aporte),
 * este reporte genera UN REGISTRO POR CADA COMBINACIÓN entidad + tipo de aporte,
 * con el total acumulado de aportes de esa entidad para ese tipo hasta la fecha de corte.
 */
@Stateless
public class GeneracionCPRMServiceImpl implements GeneracionCPRMService {

    @EJB private AporteService                      aporteService;
    @EJB private EntidadService                     entidadService;
    @EJB private EstadoParticipeService             estadoParticipeService;
    @EJB private CreditoParticipesMensualDaoService cprmDaoService;

    @Override
    public long generar(EjecucionReporteCartera ejecucion) throws Throwable {
        System.out.println("Ingresa al metodo generar CPRM");

        // -------------------------------------------------------
        // 0. Calcular fechaCorte = último día del mes a las 23:59:59
        // -------------------------------------------------------
        long mes  = ejecucion.getMes();
        long anio = ejecucion.getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();
        LocalDateTime fechaCorte = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);
        System.out.println("CPRM - fechaCorte: " + fechaCorte);

        // -------------------------------------------------------
        // 1. Ejecutar la query que retorna la suma agrupada por entidad Y tipo de aporte.
        //    Retorna: Object[]{Long codigoEntidad, Long codigoTipoAporte,
        //                      String nombreTipoAporte, Double suma}
        // -------------------------------------------------------
        List<Object[]> filas = aporteService.selectSumaPorEntidadYTipoAporte(fechaCorte);
        System.out.println("CPRM - Filas entidad+tipoAporte con suma != 0: " + filas.size());

        if (filas.isEmpty()) {
            System.out.println("CPRM - Sin datos. CPRM queda vacío, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Cargar todas las entidades necesarias en una sola consulta (optimización N+1)
        // -------------------------------------------------------
        Set<Long> codigosEntidadesNecesarias = new HashSet<>();
        for (Object[] fila : filas) {
            Long codigoEntidad = toLong(fila[0]);
            if (codigoEntidad != null) {
                codigosEntidadesNecesarias.add(codigoEntidad);
            }
        }
        
        List<Entidad> entidadesCargadas = entidadService.findByCodigosIn(
            new ArrayList<>(codigosEntidadesNecesarias)
        );
        Map<Long, Entidad> mapaEntidades = new HashMap<>();
        for (Entidad e : entidadesCargadas) {
            mapaEntidades.put(e.getCodigo(), e);
        }
        System.out.println("CPRM - Entidades cargadas en batch: " + mapaEntidades.size());

        // Cargar todos los estados de partícipe desde CRD.ESPR en una sola consulta
        Map<Long, String> mapaEstados = new HashMap<>();
        try {
            List<EstadoParticipe> estados = estadoParticipeService.selectAll();
            for (EstadoParticipe ep : estados) {
                if (ep.getCodigo() != null && ep.getNombre() != null) {
                    mapaEstados.put(ep.getCodigo(), ep.getNombre());
                }
            }
        } catch (Throwable e) {
            System.out.println("CPRM - No se pudieron cargar estados de partícipe: " + e.getMessage());
        }
        System.out.println("CPRM - Estados de partícipe cargados: " + mapaEstados.size());

        // -------------------------------------------------------
        // 3. Por cada fila → INSERT en RPR.CPRM (en primera generación no hay UPDATEs)
        // -------------------------------------------------------
        long contador = 0L;

        for (Object[] fila : filas) {
            Long   codigoEntidad    = toLong(fila[0]);
            Long   codigoTipoAporte = toLong(fila[1]);
            String nombreTipoAporte = (String) fila[2];
            Double suma             = toDouble(fila[3]);

            if (codigoEntidad == null || codigoTipoAporte == null || suma == null) continue;

            // NO se normaliza a 0: los negativos deben conservarse para que cuando CCPM
            // sume todos los totales por entidad, el resultado nete igual que G42
            // (G42 netea dentro de cada grupo; CPRM netea al sumar todas las filas).

            // Si el total es exactamente 0, no incluir el registro
            if (suma == 0.0) {
                System.out.println("CPRM SKIP entidad: " + codigoEntidad
                        + " tipoAporte: " + codigoTipoAporte + " — total es 0");
                continue;
            }

            try {
                // Obtener entidad del mapa pre-cargado
                Entidad entidad = mapaEntidades.get(codigoEntidad);
                if (entidad == null) {
                    System.out.println("CPRM SKIP entidad: " + codigoEntidad + " — no existe en ENTD");
                    continue;
                }

                // Construir el objeto TipoAporte referencial (solo necesitamos el código)
                TipoAporte tipoAporte = new TipoAporte();
                tipoAporte.setCodigo(codigoTipoAporte);

                CreditoParticipesMensual nuevo = new CreditoParticipesMensual();
                nuevo.setEjecucionReporte(ejecucion);
                nuevo.setEntidad(entidad);
                nuevo.setIdentificacion(entidad.getNumeroIdentificacion());
                nuevo.setTipoIdentificacion("C");
                nuevo.setTipoAporte(tipoAporte);
                nuevo.setTotal(suma);
                nuevo.setNombreEstado(mapaEstados.getOrDefault(entidad.getIdEstado(), "Estado " + entidad.getIdEstado()));

                cprmDaoService.save(nuevo, null);
                System.out.println("CPRM INSERT entidad: " + codigoEntidad
                        + " tipoAporte: " + codigoTipoAporte + " [" + nombreTipoAporte + "]"
                        + " total: " + suma);
                contador++;

            } catch (Throwable e) {
                System.out.println("CPRM ERROR entidad: " + codigoEntidad
                        + " tipoAporte: " + codigoTipoAporte + " — " + e.getMessage());
            }
        }

        System.out.println("CPRM generado con " + contador + " registros");
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