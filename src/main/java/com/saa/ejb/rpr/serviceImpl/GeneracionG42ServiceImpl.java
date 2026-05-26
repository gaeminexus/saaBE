package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.HistorialSueldoService;
import com.saa.ejb.rpr.dao.SaldoCuentaG42DaoService;
import com.saa.ejb.rpr.service.GeneracionG42Service;
import com.saa.model.crd.Entidad;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.SaldoCuentaG42;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG42ServiceImpl implements GeneracionG42Service {

    @EJB private AporteService            aporteService;
    @EJB private HistorialSueldoService   historialSueldoService;
    @EJB private EntidadService           entidadService;
    @EJB private SaldoCuentaG42DaoService cg42DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G42");

        // -------------------------------------------------------
        // 0. Calcular fechaCorte = último día del mes de ejecución a las 23:59:59
        // -------------------------------------------------------
        long mes  = detalle.getEjecucionReporte().getMes();
        long anio = detalle.getEjecucionReporte().getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();
        LocalDateTime fechaCorte = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);
        System.out.println("G42 - fechaCorte: " + fechaCorte);

        // -------------------------------------------------------
        // 1. Ejecutar los 4 SELECTs con SUM en la BD filtrados por fechaCorte
        // -------------------------------------------------------
        List<Object[]> rendimientos   = aporteService.selectSumaRendimientoPorEntidad(fechaCorte);
        List<Object[]> patronales     = aporteService.selectSumaPatronalPorEntidad(fechaCorte);
        List<Object[]> personales     = aporteService.selectSumaPersonalPorEntidad(fechaCorte);
        List<Object[]> aportePersonal = historialSueldoService.selectSumaAportePersonalPorEntidad(fechaCorte);

        System.out.println("G42 - Rendimientos: "   + rendimientos.size()   + " entidades");
        System.out.println("G42 - Patronales: "      + patronales.size()     + " entidades");
        System.out.println("G42 - Personales: "      + personales.size()     + " entidades");
        System.out.println("G42 - AportePersonal: "  + aportePersonal.size() + " entidades");

        // Si no hay ningún dato en ningún grupo → G42 vacío, OK
        if (rendimientos.isEmpty() && patronales.isEmpty() && personales.isEmpty() && aportePersonal.isEmpty()) {
            System.out.println("No hay datos en ningún grupo → G42 queda vacío y OK");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Consolidar en un Map<codigoEntidad, SaldoCuentaG42>
        //    para unificar los 4 grupos antes de persistir
        // -------------------------------------------------------
        Map<Long, SaldoCuentaG42> mapaRegistros = new HashMap<>();

        // Grupo 1 — Rendimiento
        for (Object[] fila : rendimientos) {
            Long codigoEntidad = toLong(fila[0]);
            Double suma        = toDouble(fila[1]);
            if (codigoEntidad == null || suma == null || suma == 0) continue;
            obtenerOCrear(mapaRegistros, codigoEntidad, detalle).setRendimiento(suma);
        }

        // Grupo 2 — Saldo Aporte Patronal
        for (Object[] fila : patronales) {
            Long codigoEntidad = toLong(fila[0]);
            Double suma        = toDouble(fila[1]);
            if (codigoEntidad == null || suma == null || suma == 0) continue;
            obtenerOCrear(mapaRegistros, codigoEntidad, detalle).setSaldoAportePatronal(suma);
        }

        // Grupo 3 — Saldo Aporte Personal
        for (Object[] fila : personales) {
            Long codigoEntidad = toLong(fila[0]);
            Double suma        = toDouble(fila[1]);
            if (codigoEntidad == null || suma == null || suma == 0) continue;
            obtenerOCrear(mapaRegistros, codigoEntidad, detalle).setSaldoAportePersonal(suma);
        }

        // Grupo 4 — Aporte Personal (desde HistorialSueldo estado=99)
        for (Object[] fila : aportePersonal) {
            Long codigoEntidad = toLong(fila[0]);
            Double suma        = toDouble(fila[1]);
            if (codigoEntidad == null || suma == null || suma == 0) continue;
            obtenerOCrear(mapaRegistros, codigoEntidad, detalle).setAportePersonal(suma);
        }

        // -------------------------------------------------------
        // 3. Por cada entidad consolidada → INSERT o UPDATE en CG42
        // -------------------------------------------------------
        long contador = 0L;

        for (Map.Entry<Long, SaldoCuentaG42> entry : mapaRegistros.entrySet()) {
            Long codigoEntidad   = entry.getKey();
            SaldoCuentaG42 nuevo = entry.getValue();

            // Buscar si ya existe un registro para esta entidad + detalle
            SaldoCuentaG42 existente = cg42DaoService.selectByEntidadYDetalle(codigoEntidad, detalle);

            if (existente != null) {
                // UPDATE — actualizar solo los campos que tienen valor
                if (nuevo.getRendimiento()        != null) existente.setRendimiento(nuevo.getRendimiento());
                if (nuevo.getSaldoAportePatronal() != null) existente.setSaldoAportePatronal(nuevo.getSaldoAportePatronal());
                if (nuevo.getSaldoAportePersonal() != null) existente.setSaldoAportePersonal(nuevo.getSaldoAportePersonal());
                if (nuevo.getAportePersonal()      != null) existente.setAportePersonal(nuevo.getAportePersonal());
                cg42DaoService.save(existente, existente.getCodigo());
                System.out.println("G42 UPDATE entidad: " + codigoEntidad);
            } else {
                // INSERT — completar datos de identificación desde Entidad
                Entidad entidad = entidadService.findById(codigoEntidad);
                if (entidad == null) {
                    System.out.println("G42 SKIP entidad: " + codigoEntidad + " — no existe en ENTD (aporte huérfano)");
                    continue;
                }
                nuevo.setEntidad(entidad);
                nuevo.setIdentificacion(entidad.getNumeroIdentificacion());
                nuevo.setTipoIdentificacion("C");
                cg42DaoService.save(nuevo, null);
                System.out.println("G42 INSERT entidad: " + codigoEntidad);
            }

            contador++;
        }

        System.out.println("G42 generado con " + contador + " registros");
        return contador;
    }

    // -------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------

    /**
     * Obtiene el SaldoCuentaG42 del mapa si ya existe, o crea uno nuevo con
     * detalleEjecucion ya asignado y lo agrega al mapa.
     */
    private SaldoCuentaG42 obtenerOCrear(Map<Long, SaldoCuentaG42> mapa,
                                          Long codigoEntidad,
                                          DetalleEjecucionReporte detalle) {
        return mapa.computeIfAbsent(codigoEntidad, id -> {
            SaldoCuentaG42 g42 = new SaldoCuentaG42();
            g42.setDetalleEjecucion(detalle);
            g42.setRendimiento(0.0);
            g42.setSaldoAportePatronal(0.0);
            g42.setSaldoAportePersonal(0.0);
            g42.setAportePersonal(0.0);
            return g42;
        });
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
