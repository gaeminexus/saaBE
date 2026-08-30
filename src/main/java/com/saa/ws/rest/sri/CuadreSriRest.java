package com.saa.ws.rest.sri;

import java.util.Map;

import com.saa.ejb.sri.service.ReporteCuadreSriService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Reporte de apoyo al cuadre de los formularios 104 (IVA) y 103 (retenciones) — Fase 6, ver
 * docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §10. No genera los formularios; da los
 * totales que el sistema puede calcular, para contrastar contra lo que el SRI prellena.
 */
@Path("cuadresri")
public class CuadreSriRest {

    @EJB
    private ReporteCuadreSriService reporteCuadreSriService;

    /**
     * GET /rest/cuadresri/104/{idFacturador}?anio=2026&mes=8
     */
    @GET
    @Path("/104/{idFacturador}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cuadre104(@PathParam("idFacturador") Long idFacturador,
            @QueryParam("anio") Integer anio, @QueryParam("mes") Integer mes) {
        System.out.println("LLEGA AL SERVICIO GET /cuadresri/104/" + idFacturador);
        try {
            if (anio == null || mes == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar anio y mes.").type(MediaType.APPLICATION_JSON).build();
            }
            Map<String, Object> resultado = reporteCuadreSriService.calcularCuadre104(idFacturador, anio, mes);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al calcular el cuadre del 104: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET /rest/cuadresri/103/{idFacturador}?anio=2026&mes=8
     */
    @GET
    @Path("/103/{idFacturador}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cuadre103(@PathParam("idFacturador") Long idFacturador,
            @QueryParam("anio") Integer anio, @QueryParam("mes") Integer mes) {
        System.out.println("LLEGA AL SERVICIO GET /cuadresri/103/" + idFacturador);
        try {
            if (anio == null || mes == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar anio y mes.").type(MediaType.APPLICATION_JSON).build();
            }
            Map<String, Object> resultado = reporteCuadreSriService.calcularCuadre103(idFacturador, anio, mes);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al calcular el cuadre del 103: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
