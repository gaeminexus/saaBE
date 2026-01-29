package com.saa.ws.rest.reporte;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.saa.model.reporte.ReporteRequest;
import com.saa.model.reporte.ReporteResponse;
import com.saa.ejb.reporte.service.ReporteService;

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