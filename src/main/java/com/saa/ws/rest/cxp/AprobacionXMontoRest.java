package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.AprobacionXMontoDaoService;
import com.saa.ejb.cxp.service.AprobacionXMontoService;
import com.saa.model.cxp.AprobacionXMonto;
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
public class AprobacionXMontoRest {

    @EJB
    private AprobacionXMontoDaoService AprobacionXMontoDaoService;

    @EJB
    private AprobacionXMontoService AprobacionXMontoService;

    @Context
    private UriInfo context;

    public AprobacionXMontoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<AprobacionXMonto> getAll() throws Throwable {
        return AprobacionXMontoDaoService.selectAll(NombreEntidadesPago.APROBACION_X_MONTO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public AprobacionXMonto getId(@PathParam("id") Long id) throws Throwable {
        return AprobacionXMontoDaoService.selectById(id, NombreEntidadesPago.APROBACION_X_MONTO);
    }

    @PUT
    @Consumes("application/json")
    public AprobacionXMonto put(AprobacionXMonto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - AprobacionXMonto");
        return AprobacionXMontoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public AprobacionXMonto post(AprobacionXMonto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - AprobacionXMonto");
        return AprobacionXMontoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de AprobacionXMonto");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(AprobacionXMontoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - AprobacionXMonto");
        AprobacionXMonto elimina = new AprobacionXMonto();
        AprobacionXMontoDaoService.remove(elimina, id);
    }
}
