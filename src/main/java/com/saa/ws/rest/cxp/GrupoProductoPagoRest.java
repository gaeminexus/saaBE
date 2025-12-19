package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.GrupoProductoPagoDaoService;
import com.saa.ejb.cxp.service.GrupoProductoPagoService;
import com.saa.model.cxp.GrupoProductoPago;
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

@Path("grpp")
public class GrupoProductoPagoRest {

    @EJB
    private GrupoProductoPagoDaoService GrupoProductoPagoDaoService;

    @EJB
    private GrupoProductoPagoService GrupoProductoPagoService;

    @Context
    private UriInfo context;

    public GrupoProductoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<GrupoProductoPago> getAll() throws Throwable {
        return GrupoProductoPagoDaoService.selectAll(NombreEntidadesPago.GRUPO_PRODUCTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public GrupoProductoPago getId(@PathParam("id") Long id) throws Throwable {
        return GrupoProductoPagoDaoService.selectById(id, NombreEntidadesPago.GRUPO_PRODUCTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public GrupoProductoPago put(GrupoProductoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - GRUPO_PRODUCTO_PAGO");
        return GrupoProductoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public GrupoProductoPago post(GrupoProductoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - GRUPO_PRODUCTO_PAGO");
        return GrupoProductoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de GRUPO_PRODUCTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(GrupoProductoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - GRUPO_PRODUCTO_PAGO");
        GrupoProductoPago elimina = new GrupoProductoPago();
        GrupoProductoPagoDaoService.remove(elimina, id);
    }
}
