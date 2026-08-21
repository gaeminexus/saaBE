package com.saa.ws.rest.rhh;

import com.saa.ejb.rhh.service.PlanillaControlIessService;
import com.saa.model.rhh.PlanillaControlIess;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Planilla de control del IESS: lo que deberia salir en el portal, calculado desde
 * nuestras nominas, para enfrentarlo con la planilla real antes de pagar.
 */
@Path("plie")
public class PlanillaControlIessRest {

    @EJB
    private PlanillaControlIessService planillaControlIessService;

    public PlanillaControlIessRest() {
    }

    /**
     * Genera la planilla de control del periodo.
     *
     * @param idPeriodo	: Id del periodo de nomina
     * @return			: La planilla con lineas, totales del comprobante y avisos
     */
    @GET
    @Path("/getPeriodo/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPeriodo(@PathParam("idPeriodo") Long idPeriodo) {
        System.out.println("LLEGA AL SERVICIO GET PERIODO - PLANILLA CONTROL IESS");
        try {
            PlanillaControlIess planilla = planillaControlIessService.generar(idPeriodo);
            return Response.status(Response.Status.OK).entity(planilla).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error al generar la planilla de control: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
