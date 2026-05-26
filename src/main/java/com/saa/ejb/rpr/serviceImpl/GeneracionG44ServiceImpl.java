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
        // 2. Ejecutar SELECTs con COUNT/SUM en BD filtrados por fechaCorte
        // -------------------------------------------------------

        // COUNT de aportes con tipoAporte.codigo IN (9, 11) → imposicionesAcumuladas
        List<Object[]> listaImposiciones = aporteService.selectCountImposicionesJubilacionPorEntidad(fechaCorte);
        Map<Long, Long> mapaImposiciones = new HashMap<>();
        for (Object[] fila : listaImposiciones) {
            Long codigoEntidad = toLong(fila[0]);
            Long count         = toLong(fila[1]);
            if (codigoEntidad != null && count != null) {
                mapaImposiciones.put(codigoEntidad, count);
            }
        }

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

        System.out.println("G44 - Imposiciones en BD: " + mapaImposiciones.size() + " entidades");
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

            // fechaJubilacion → buscar en HistoricoG44 por cédula; null si no existe
            java.time.LocalDate fechaJubilacion = null;
            List<HistoricoG44> historicoList = historicoG44Service.selectByIdentificacion(entidad.getNumeroIdentificacion());
            if (historicoList != null && !historicoList.isEmpty() && historicoList.get(0).getFechaJubilacion() != null) {
                try {
                    fechaJubilacion = java.time.LocalDate.parse(historicoList.get(0).getFechaJubilacion());
                } catch (Exception ex) {
                    System.out.println("G44 - fechaJubilacion no parseable para cedula: " + entidad.getNumeroIdentificacion());
                }
            }

            ParticipeJubiladoG44 jubilado = new ParticipeJubiladoG44();
            jubilado.setIdentificacion(entidad.getNumeroIdentificacion());
            jubilado.setTipoIdentificacion("C");
            jubilado.setTipoJubilacion("V");
            jubilado.setFechaJubilacion(fechaJubilacion);
            jubilado.setImposicionesAcumuladas(mapaImposiciones.getOrDefault(codigoEntidad, 0L));
            jubilado.setValorPension(valorPension);
            jubilado.setValorNetoRecibir(valorPension);
            jubilado.setSaldoCuenta(mapaSaldo.getOrDefault(codigoEntidad, 0.0));
            jubilado.setJubilacionIess("S");
            jubilado.setDetalleEjecucion(detalle);

            cg44DaoService.save(jubilado, null);
            System.out.println("G44 INSERT jubilado: " + entidad.getNumeroIdentificacion());
            contador++;
        }

        System.out.println("G44 generado con " + contador + " registros");
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