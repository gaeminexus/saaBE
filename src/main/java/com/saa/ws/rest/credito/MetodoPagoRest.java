package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.MetodoPagoDaoService;
import com.saa.ejb.credito.service.MetodoPagoService;
import com.saa.model.credito.MetodoPago;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("mtdp")
public class MetodoPagoRest {

    @EJB
    private MetodoPagoDaoService metodoPagoDaoService;

    @EJB
    private MetodoPagoService metodoPagoService;

    @Context
    private UriInfo context;

    public MetodoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MetodoPago> getAll() throws Throwable {
        return metodoPagoDaoService.selectAll(NombreEntidadesCredito.METODO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public MetodoPago getId(@PathParam("id") Long id) throws Throwable {
        return metodoPagoDaoService.selectById(id, NombreEntidadesCredito.METODO_PAGO);
    }

    @PUT
    @Consumes("application/json")
    public MetodoPago put(MetodoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return metodoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public MetodoPago post(MetodoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return metodoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MetodoPago");
        Response respuesta;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(metodoPagoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        MetodoPago elimina = new MetodoPago();
        metodoPagoDaoService.remove(elimina, id);
    }
}
