package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.EjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG40Service;
import com.saa.ejb.rpr.service.GeneracionG41Service;
import com.saa.ejb.rpr.service.GeneracionG42Service;
import com.saa.ejb.rpr.service.GeneracionG43Service;
import com.saa.ejb.rpr.service.GeneracionG44Service;
import com.saa.ejb.rpr.service.GeneracionG45Service;
import com.saa.ejb.rpr.service.GeneracionReportesService;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.EjecucionReporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionReportesServiceImpl implements GeneracionReportesService {

    // Estados EJRC
    private static final Long EJRC_EN_PROCESO    = 1L;
    private static final Long EJRC_CON_NOVEDADES = 2L;
    private static final Long EJRC_COMPLETO      = 3L;

    // Estados EJRD
    private static final Long EJRD_OK            = 1L;
    private static final Long EJRD_CON_NOVEDADES = 2L;
    private static final Long EJRD_PENDIENTE     = 3L;

    // Tipo ejecución EJRC
    private static final Long TIPO_INICIAL       = 1L;
    private static final Long TIPO_CORRECCION    = 2L;

    // Tipos de reporte
    private static final String[] TIPOS_REPORTE = {
        "G40", "G41", "G42", "G43", "G44", "G45",
        "G46", "G47", "G48", "G49", "G50", "G51"
    };

    @EJB private EjecucionReporteService        ejrcService;
    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private GeneracionG40Service           g40Service;
    @EJB private GeneracionG41Service           g41Service;
    @EJB private GeneracionG42Service           g42Service;
    @EJB private GeneracionG43Service           g43Service;
    @EJB private GeneracionG44Service           g44Service;
    @EJB private GeneracionG45Service           g45Service;

    // -------------------------------------------------------
    // TODO: agregar @EJB de cada GeneracionGxxService
    // cuando el usuario vaya proporcionando la lógica de cada G.
    // @EJB private GeneracionG41Service g41Service;
    // ...
    // -------------------------------------------------------

    @Override
    public EjecucionReporte ejecutarGeneracion(Long mes, Long anio, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo ejecutarGeneracion con mes: " + mes + ", anio: " + anio + ", usuario: " + usuario);

        EjecucionReporte ejrc     = null;
        List<DetalleEjecucionReporte> ejrdsAProcesar = new ArrayList<>();
        boolean esCorreccion = false;

        // -------------------------------------------------------
        // 1. Buscar si ya existe una ejecución para ese mes/año
        // -------------------------------------------------------
        List<EjecucionReporte> ejecucionesExistentes = null;
        try {
            ejecucionesExistentes = ejrcService.selectByMesAnio(mes, anio);
        } catch (Throwable e) {
            // No existe ejecución previa → se crea nueva
            ejecucionesExistentes = new ArrayList<>();
        }

        if (!ejecucionesExistentes.isEmpty()) {
            ejrc = ejecucionesExistentes.get(0);
            System.out.println("Ejecucion existente encontrada con id: " + ejrc.getCodigo() + ", estado: " + ejrc.getEstado());

            // ---------------------------------------------------
            // 2. Si ya está completa → informar al frontend
            // ---------------------------------------------------
            if (EJRC_COMPLETO.equals(ejrc.getEstado())) {
                System.out.println("Todos los reportes ya fueron generados para mes: " + mes + " anio: " + anio);
                throw new Exception("Los reportes G40-G51 ya fueron generados correctamente para " + mes + "/" + anio
                        + ". Ejecucion id: " + ejrc.getCodigo());
            }

            // ---------------------------------------------------
            // 3. Obtener solo los EJRD pendientes o con novedades
            // ---------------------------------------------------
            try {
                ejrdsAProcesar = ejrdService.selectPendientesYNovedadesByEjecucion(ejrc.getCodigo());
            } catch (Throwable e) {
                ejrdsAProcesar = new ArrayList<>();
            }
            esCorreccion = true;

            // Marcar EJRC como en proceso nuevamente
            ejrc.setEstado(EJRC_EN_PROCESO);
            ejrcService.saveSingle(ejrc);

        } else {
            // ---------------------------------------------------
            // 4. Crear nueva cabecera EJRC
            // ---------------------------------------------------
            ejrc = new EjecucionReporte();
            ejrc.setMes(mes);
            ejrc.setAnio(anio);
            ejrc.setUsuario(usuario);
            ejrc.setFechaGeneracion(LocalDate.now());
            ejrc.setTipoEjecucion(TIPO_INICIAL);
            ejrc.setEstado(EJRC_EN_PROCESO);
            ejrc.setObservaciones("Ejecucion inicial generada automaticamente");
            ejrc = ejrcService.saveSingle(ejrc);
            System.out.println("EJRC creado con id: " + ejrc.getCodigo());

            // ---------------------------------------------------
            // 5. Crear los 12 EJRD en estado Pendiente
            // ---------------------------------------------------
            for (String tipoReporte : TIPOS_REPORTE) {
                DetalleEjecucionReporte ejrd = new DetalleEjecucionReporte();
                ejrd.setEjecucionReporte(ejrc);
                ejrd.setTipoReporte(tipoReporte);
                ejrd.setEstado(EJRD_PENDIENTE);
                ejrd = ejrdService.saveSingle(ejrd);
                ejrdsAProcesar.add(ejrd);
                System.out.println("EJRD creado para " + tipoReporte + " con id: " + ejrd.getCodigo());
            }
        }

        // -------------------------------------------------------
        // 6. Ejecutar la lógica de generación por cada EJRD
        // -------------------------------------------------------
        for (DetalleEjecucionReporte ejrd : ejrdsAProcesar) {
            System.out.println("Procesando reporte: " + ejrd.getTipoReporte());

            // Si es corrección, actualizar tipo ejecución del EJRC
            if (esCorreccion) {
                ejrc.setTipoEjecucion(TIPO_CORRECCION);
            }

            try {
                long cantidadRegistros = ejecutarG(ejrd);

                // Actualizar EJRD como OK
                ejrd.setEstado(EJRD_OK);
                ejrd.setFechaGeneracion(LocalDate.now());
                ejrd.setCantidadRegistros(cantidadRegistros);
                ejrd.setNovedades(null);
                ejrdService.saveSingle(ejrd);
                System.out.println("Reporte " + ejrd.getTipoReporte() + " generado OK con " + cantidadRegistros + " registros");

            } catch (Throwable e) {
                // Registrar el fallo en el EJRD sin detener los demás
                String mensajeError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                ejrd.setEstado(EJRD_CON_NOVEDADES);
                ejrd.setFechaGeneracion(LocalDate.now());
                ejrd.setNovedades("ERROR: " + mensajeError);
                ejrdService.saveSingle(ejrd);
                System.out.println("Reporte " + ejrd.getTipoReporte() + " fallo: " + mensajeError);
            }
        }

        // -------------------------------------------------------
        // 7. Evaluar estado final del EJRC
        // -------------------------------------------------------
        List<DetalleEjecucionReporte> conProblemas = new ArrayList<>();
        try {
            conProblemas = ejrdService.selectPendientesYNovedadesByEjecucion(ejrc.getCodigo());
        } catch (Throwable e) {
            // No hay problemas
        }

        if (conProblemas.isEmpty()) {
            ejrc.setEstado(EJRC_COMPLETO);
            ejrc.setObservaciones("Todos los reportes G40-G51 generados correctamente");
        } else {
            ejrc.setEstado(EJRC_CON_NOVEDADES);
            ejrc.setObservaciones(conProblemas.size() + " reporte(s) con novedades: ver detalle en EJRD");
        }

        ejrc = ejrcService.saveSingle(ejrc);
        System.out.println("Ejecucion finalizada con estado: " + ejrc.getEstado());

        return ejrc;
    }

    // -------------------------------------------------------
    // Despachador — llama al service del G correspondiente
    // Agregar cada caso conforme se vaya implementando la lógica
    // -------------------------------------------------------
    private long ejecutarG(DetalleEjecucionReporte ejrd) throws Throwable {
        switch (ejrd.getTipoReporte()) {
            case "G40": return g40Service.generar(ejrd);
            case "G41": return g41Service.generar(ejrd);
            case "G42": return g42Service.generar(ejrd);
            case "G43": return g43Service.generar(ejrd);
            case "G44": return g44Service.generar(ejrd);
            case "G45": return g45Service.generar(ejrd);
            // case "G44": return g44Service.generar(ejrd);
            // case "G45": return g45Service.generar(ejrd);
            // case "G46": return g46Service.generar(ejrd);
            // case "G47": return g47Service.generar(ejrd);
            // case "G48": return g48Service.generar(ejrd);
            // case "G49": return g49Service.generar(ejrd);
            // case "G50": return g50Service.generar(ejrd);
            // case "G51": return g51Service.generar(ejrd);
            default:
                throw new Exception("Logica de generacion no implementada para: " + ejrd.getTipoReporte());
        }
    }
}
