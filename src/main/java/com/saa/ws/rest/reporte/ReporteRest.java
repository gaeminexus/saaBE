package com.saa.ws.rest.reporte;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.saa.ejb.reporte.service.ReporteService;
import com.saa.model.reporte.ReporteLoteItem;
import com.saa.model.reporte.ReporteLoteRequest;
import com.saa.model.reporte.ReporteRequest;
import com.saa.model.reporte.ReporteResponse;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API para generación de reportes JasperReports
 */
@Path("rprt")
public class ReporteRest { 
    
    private static final Logger LOGGER = Logger.getLogger(ReporteRest.class.getName());
    
    @EJB
    private ReporteService reporteService;
    
    /**
     * Genera un reporte basado en la solicitud proporcionada
     * 
     * POST /rest/rprt/generar
     * Body: {
     *   "modulo": "cnt",
     *   "nombreReporte": "balance_general",
     *   "formato": "PDF",
     *   "parametros": {
     *     "empresaId": 1,
     *     "fecha": "2026-01-29"
     *   }
     * }
     */
    @POST
    @Path("/generar")
    @Produces({"application/pdf", "application/vnd.ms-excel", "text/html"})
    public Response generarReporte(ReporteRequest request) {
        
        LOGGER.log(Level.INFO, "Solicitud de reporte recibida: {0}", request);
        
        try {
            // Validaciones
            if (request.getModulo() == null || request.getModulo().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ReporteResponse(false, "El módulo es requerido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
            }
            
            if (request.getNombreReporte() == null || request.getNombreReporte().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ReporteResponse(false, "El nombre del reporte es requerido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
            }
            
            if (!reporteService.esModuloValido(request.getModulo())) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ReporteResponse(false, "Módulo no válido. Debe ser: cnt, tsr, crd, cxc, cxp o rhh"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
            }
            
            // Formato por defecto PDF
            String formato = request.getFormato() != null ? request.getFormato() : "PDF";
            
            // Generar el reporte
            byte[] reporteBytes = reporteService.generarReporte(
                request.getModulo(),
                request.getNombreReporte(),
                request.getParametros(),
                formato
            );
            
            // Determinar el content type y extensión según el formato
            String contentType;
            String extension;
            
            switch (formato.toUpperCase()) {
                case "EXCEL":
                case "XLS":
                case "XLSX":
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    extension = ".xlsx";
                    break;
                case "HTML":
                    contentType = "text/html";
                    extension = ".html";
                    break;
                default:
                    contentType = "application/pdf";
                    extension = ".pdf";
            }
            
            // Construir nombre del archivo
            String nombreArchivo = String.format("%s_%s%s", 
                request.getModulo(), 
                request.getNombreReporte(), 
                extension);
            
            LOGGER.log(Level.INFO, "Reporte generado exitosamente: {0}", nombreArchivo);
            
            // Retornar el reporte como bytes
            return Response.ok(reporteBytes)
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                .header("Content-Type", contentType)
                .build();
                
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Reporte no encontrado", e);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ReporteResponse(false, "Reporte no encontrado: " + e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar reporte", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ReporteResponse(false, "Error al generar reporte: " + e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }
    
    /**
     * Un solo PDF con varios reportes (DISENO-IMPRESOS-OTORGAMIENTO.md §4).
     *
     * POST /rest/rprt/generarLote
     * Body: { "modulo": "crd", "formato": "PDF",
     *         "reportes": [ { "nombreReporte": "RPRT_PRST_SLCT", "parametros": { "P_PRSTCDGO": 5555 } }, ... ] }
     *
     * 400 si el formato no es PDF, la lista viene vacía o falta el módulo o un nombre; 500 si falla
     * alguno (el mensaje dice cuál) — no se entrega un PDF incompleto.
     */
    @POST
    @Path("/generarLote")
    @Produces({"application/pdf", "application/json"})
    public Response generarReporteLote(ReporteLoteRequest request) {
        System.out.println("ReporteRest.generarReporteLote: " + request);
        try {
            if (request == null || request.getModulo() == null || request.getModulo().trim().isEmpty()) {
                return respuestaLoteInvalida("El módulo es requerido");
            }
            if (!reporteService.esModuloValido(request.getModulo())) {
                return respuestaLoteInvalida("Módulo no válido. Debe ser: cnt, tsr, crd, cxc, cxp o rhh");
            }
            if (request.getFormato() == null || !"PDF".equalsIgnoreCase(request.getFormato().trim())) {
                return respuestaLoteInvalida("El lote solo se entrega en formato PDF");
            }
            if (request.getReportes() == null || request.getReportes().isEmpty()) {
                return respuestaLoteInvalida("La lista de reportes está vacía");
            }
            for (ReporteLoteItem item : request.getReportes()) {
                if (item == null || item.getNombreReporte() == null || item.getNombreReporte().trim().isEmpty()) {
                    return respuestaLoteInvalida("Cada reporte del lote necesita su nombreReporte");
                }
            }

            byte[] pdf = reporteService.generarReporteLote(request.getModulo(), request.getReportes());
            return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"" + request.getModulo() + "_lote.pdf\"")
                .header("Content-Type", "application/pdf")
                .build();
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Error al generar el lote de reportes", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ReporteResponse(false, "Error al generar el lote: " + e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }

    private Response respuestaLoteInvalida(String mensaje) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ReporteResponse(false, mensaje))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    /**
     * Lista los módulos disponibles
     * GET /rest/rprt/modulos
     */
    @GET
    @Path("/modulos")
    public Response listarModulos() {
        String[] modulos = {"cnt", "tsr", "crd", "cxc", "cxp", "rhh"};
        return Response.ok(modulos).build();
    }
    
    /**
     * Endpoint de prueba para verificar que el servicio está funcionando
     * GET /rest/rprt/ping
     */
    @GET
    @Path("/ping")
    public Response ping() {
        return Response.ok(new ReporteResponse(true, "Servicio de reportes activo")).build();
    }
}