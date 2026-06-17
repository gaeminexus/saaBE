package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;

import com.saa.ejb.rpr.service.EjecucionReporteCarteraService;
import com.saa.ejb.rpr.service.GeneracionCCPMService;
import com.saa.ejb.rpr.service.GeneracionCJBMService;
import com.saa.ejb.rpr.service.GeneracionCPRMService;
import com.saa.ejb.rpr.service.GeneracionReportesCarteraService;
import com.saa.ejb.rpr.service.LimpiezaReportesService;
import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Servicio orquestador para la generación de reportes de cartera.
 * Ejecuta secuencialmente: CPRM, CJBM y CCPM.
 */
@Stateless
public class GeneracionReportesCarteraServiceImpl implements GeneracionReportesCarteraService {

    @EJB private EjecucionReporteCarteraService         ejccService;
    @EJB private GeneracionCPRMService                  cprmService;
    @EJB private GeneracionCJBMService                  cjbmService;
    @EJB private GeneracionCCPMService                  ccpmService;
    @EJB private LimpiezaReportesService                limpiezaReportesService;

    @Override
    public EjecucionReporteCartera ejecutarGeneracion(Long mes, Long anio, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo ejecutarGeneracion de reportes de cartera - mes: " + mes + ", anio: " + anio + ", usuario: " + usuario);

        // -------------------------------------------------------
        // 1. Validar que no exista una ejecución previa para este mes/año
        // -------------------------------------------------------
        java.util.List<EjecucionReporteCartera> ejecucionesExistentes = null;
        try {
            ejecucionesExistentes = ejccService.selectByMesAnio(mes, anio);
        } catch (Throwable e) {
            ejecucionesExistentes = new java.util.ArrayList<>();
        }

        if (!ejecucionesExistentes.isEmpty()) {
            EjecucionReporteCartera existente = ejecucionesExistentes.get(0);
            System.out.println("Ya existe una ejecución de reportes de cartera para " + mes + "/" + anio 
                + " con código: " + existente.getCodigo());
            throw new Exception("Los reportes de cartera CPRM, CJBM y CCPM ya fueron generados para " + mes + "/" + anio 
                + ". Si desea regenerarlos, primero elimine la ejecución existente (código: " + existente.getCodigo() + ")");
        }

        // -------------------------------------------------------
        // 2. Crear la ejecución
        // -------------------------------------------------------
        EjecucionReporteCartera ejcc = new EjecucionReporteCartera();
        ejcc.setMes(mes);
        ejcc.setAnio(anio);
        ejcc.setUsuario(usuario);
        ejcc.setFechaGeneracion(LocalDate.now());
        ejcc.setObservaciones("Generación automática de reportes CPRM, CJBM y CCPM");
        
        ejcc = ejccService.saveSingle(ejcc);
        System.out.println("Ejecución de reportes de cartera creada con código: " + ejcc.getCodigo());

        // -------------------------------------------------------
        // 3. Ejecutar los 3 reportes secuencialmente
        // -------------------------------------------------------
        StringBuilder observaciones = new StringBuilder();
        long totalRegistros = 0L;

        try {
            // CPRM - Crédito Partícipes Mensual (similar a G42)
            System.out.println("========================================");
            System.out.println("Generando CPRM (Crédito Partícipes Mensual)...");
            System.out.println("========================================");
            long registrosCPRM = cprmService.generar(ejcc);
            observaciones.append("CPRM: ").append(registrosCPRM).append(" registros. ");
            totalRegistros += registrosCPRM;
            System.out.println("CPRM completado: " + registrosCPRM + " registros");

        } catch (Throwable e) {
            System.out.println("ERROR al generar CPRM: " + e.getMessage());
            e.printStackTrace();
            observaciones.append("CPRM: ERROR - ").append(e.getMessage()).append(". ");
        }

        try {
            // CJBM - Crédito Jubilados Mensual (similar a G44)
            System.out.println("========================================");
            System.out.println("Generando CJBM (Crédito Jubilados Mensual)...");
            System.out.println("========================================");
            long registrosCJBM = cjbmService.generar(ejcc);
            observaciones.append("CJBM: ").append(registrosCJBM).append(" registros. ");
            totalRegistros += registrosCJBM;
            System.out.println("CJBM completado: " + registrosCJBM + " registros");

        } catch (Throwable e) {
            System.out.println("ERROR al generar CJBM: " + e.getMessage());
            e.printStackTrace();
            observaciones.append("CJBM: ERROR - ").append(e.getMessage()).append(". ");
        }

        try {
            // CCPM - Crédito Cuotas Préstamos Mensual (similar a G48 con campos adicionales)
            System.out.println("========================================");
            System.out.println("Generando CCPM (Crédito Cuotas Préstamos Mensual)...");
            System.out.println("========================================");
            long registrosCCPM = ccpmService.generar(ejcc);
            observaciones.append("CCPM: ").append(registrosCCPM).append(" registros.");
            totalRegistros += registrosCCPM;
            System.out.println("CCPM completado: " + registrosCCPM + " registros");

        } catch (Throwable e) {
            System.out.println("ERROR al generar CCPM: " + e.getMessage());
            e.printStackTrace();
            observaciones.append("CCPM: ERROR - ").append(e.getMessage()).append(".");
        }

        // -------------------------------------------------------
        // 4. Actualizar la ejecución con el resumen
        // -------------------------------------------------------
        ejcc.setObservaciones(observaciones.toString());
        ejcc = ejccService.saveSingle(ejcc);

        System.out.println("========================================");
        System.out.println("GENERACIÓN DE REPORTES DE CARTERA COMPLETADA");
        System.out.println("Total de registros generados: " + totalRegistros);
        System.out.println("Observaciones: " + observaciones.toString());
        System.out.println("========================================");

        return ejcc;
    }

    @Override
    public EjecucionReporteCartera regenerarGeneracion(Long codigoEjecucion, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo regenerarGeneracion - codigoEjecucion: " + codigoEjecucion + ", usuario: " + usuario);

        // 1. Obtener la ejecución existente para saber mes/año
        EjecucionReporteCartera ejccExistente = ejccService.selectById(codigoEjecucion);
        if (ejccExistente == null) {
            throw new Exception("No existe una ejecución de reportes de cartera con código: " + codigoEjecucion);
        }
        Long mes  = ejccExistente.getMes();
        Long anio = ejccExistente.getAnio();
        System.out.println("Regenerando reportes de cartera para " + mes + "/" + anio);

        // 2. Borrar hijos en una transacción separada (REQUIRES_NEW) para asegurar el commit antes de borrar el padre
        limpiezaReportesService.limpiarDatosReportes(codigoEjecucion);
        System.out.println("Limpieza de reportes hijos completada y confirmada.");

        // 3. Borrar el EJCC padre
        java.util.List<Long> ids = new java.util.ArrayList<>();
        ids.add(codigoEjecucion);
        ejccService.remove(ids);
        System.out.println("Ejecución EJCC " + codigoEjecucion + " eliminada");

        // 4. Volver a generar
        return ejecutarGeneracion(mes, anio, usuario);
    }
}
