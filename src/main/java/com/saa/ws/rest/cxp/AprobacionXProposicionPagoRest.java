package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.AprobacionXProposicionPagoDaoService;
import com.saa.ejb.cxp.service.AprobacionXProposicionPagoService;
import com.saa.model.cxp.AprobacionXProposicionPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("apxm")
public class AprobacionXProposicionPagoRest {

    @EJB
    private AprobacionXProposicionPagoDaoService AprobacionXProposicionPagoDaoService;

    @EJB
    private AprobacionXProposicionPagoService AprobacionXProposicionPagoService;

    @Context
    private UriInfo context;

    public AprobacionXProposicionPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<AprobacionXProposicionPago> getAll() throws Throwable {
        return AprobacionXProposicionPagoDaoService.selectAll(NombreEntidadesPago.APROBACION_X_PROPOSICION_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public AprobacionXProposicionPago getId(@PathParam("id") Long id) throws Throwable {
        return AprobacionXProposicionPagoDaoService.selectById(id, NombreEntidadesPago.APROBACION_X_PROPOSICION_PAGO);
    }

    @PUT
    @Consumes("application/json")
    public AprobacionXProposicionPago put(AprobacionXProposicionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - AprobacionXProposicionPago");
        return AprobacionXProposicionPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public AprobacionXProposicionPago post(AprobacionXProposicionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - AprobacionXProposicionPago");
        return AprobacionXProposicionPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de AprobacionXProposicionPago");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(AprobacionXProposicionPagoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - AprobacionXProposicionPago");
        AprobacionXProposicionPago elimina = new AprobacionXProposicionPago();
        AprobacionXProposicionPagoDaoService.remove(elimina, id);
    }
}
