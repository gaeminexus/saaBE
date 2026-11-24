package com.saa.ws.rest.crd;

import java.util.HashMap;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.ReportePrestamoCanceladoService;
import com.saa.ejb.crd.service.dto.FiltroPrestamosCancelados;
import com.saa.ejb.crd.service.dto.ReportePrestamosCancelados;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Reporte de préstamos cancelados (pantalla y PDF). No es una tabla: {@code prcn} es una ruta de reporte.
 * Contrato para el frontend en docs/logica-negocio/crd/API-REPORTE-PRESTAMOS-CANCELADOS.md — cualquier
 * cambio de ruta, request o response se registra ahí en el mismo cambio.
 *
 * <p>Los errores salen SIEMPRE como JSON {@code {"mensaje": "..."}} (el FE lee {@code mensaje}):
 * 400 para un filtro inválido ({@link IncomeException}), 500 para cualquier otra falla.</p>
 */
@Path("prcn")
public class PrestamoCanceladoRest {

    @EJB
    private ReportePrestamoCanceladoService reportePrestamoCanceladoService;

    @POST
    @Path("/consultar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response consultar(FiltroPrestamosCancelados filtro) {
        System.out.println("PrestamoCanceladoRest.consultar");
        try {
            ReportePrestamosCancelados reporte = reportePrestamoCanceladoService.consultar(filtro);
            return Response.ok(reporte, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e);
        }
    }

    @POST
    @Path("/pdf")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/pdf")
    public Response pdf(FiltroPrestamosCancelados filtro) {
        System.out.println("PrestamoCanceladoRest.pdf");
        try {
            byte[] pdf = reportePrestamoCanceladoService.pdf(filtro);
            // La validación ya pasó dentro del servicio: las fechas son válidas.
            String nombre = "prestamos-cancelados-" + filtro.getFechaDesde().trim() + "-a-"
                + filtro.getFechaHasta().trim() + ".pdf";
            return Response.ok(pdf, "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + nombre + "\"")
                .build();
        } catch (Throwable e) {
            return error(e);
        }
    }

    /** Error como JSON con {@code mensaje}; el tipo se fija aquí porque {@code @Produces} del método es el PDF. */
    private Response error(Throwable e) {
        boolean validacion = e instanceof IncomeException;
        if (!validacion) {
            e.printStackTrace();
        }
        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("mensaje", validacion
            ? e.getMessage()
            : "Error al generar el reporte de préstamos cancelados: " + e.getMessage());
        return Response.status(validacion ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR)
            .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }
}
