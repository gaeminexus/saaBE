package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.ComposicionCuotaInicialPagoDaoService;
import com.saa.ejb.cxp.service.ComposicionCuotaInicialPagoService;
import com.saa.model.cxp.ComposicionCuotaInicialPago;
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

@Path("ccip")
public class ComposicionCuotaInicialPagoRest {

    @EJB
    private ComposicionCuotaInicialPagoDaoService ComposicionCuotaInicialPagoDaoService;

    @EJB
    private ComposicionCuotaInicialPagoService ComposicionCuotaInicialPagoService;

    @Context
    private UriInfo context;

    public ComposicionCuotaInicialPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ComposicionCuotaInicialPago> getAll() throws Throwable {
        return ComposicionCuotaInicialPagoDaoService.selectAll(NombreEntidadesPago.COMPOSICION_CUOTA_INICIAL_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ComposicionCuotaInicialPago getId(@PathParam("id") Long id) throws Throwable {
        return ComposicionCuotaInicialPagoDaoService.selectById(id, NombreEntidadesPago.COMPOSICION_CUOTA_INICIAL_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public ComposicionCuotaInicialPago put(ComposicionCuotaInicialPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - COMPOSICION_CUOTA_INICIAL_PAGO");
        return ComposicionCuotaInicialPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public ComposicionCuotaInicialPago post(ComposicionCuotaInicialPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - COMPOSICION_CUOTA_INICIAL_PAGO");
        return ComposicionCuotaInicialPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de COMPOSICION_CUOTA_INICIAL_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(ComposicionCuotaInicialPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - COMPOSICION_CUOTA_INICIAL_PAGO");
        ComposicionCuotaInicialPago elimina = new ComposicionCuotaInicialPago();
        ComposicionCuotaInicialPagoDaoService.remove(elimina, id);
    }
}
