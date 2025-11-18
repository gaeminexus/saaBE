package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoPagoDaoService;
import com.saa.ejb.credito.service.TipoPagoService;
import com.saa.model.credito.TipoPago;
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

@Path("tppg")
public class TipoPagoRest {

    @EJB
    private TipoPagoDaoService tipoPagoDaoService;

    @EJB
    private TipoPagoService tipoPagoService;

    @Context
    private UriInfo context;

    public TipoPagoRest() {
        // Constructor vacío
    }

    /**
     * GET - Obtener todos los registros
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TipoPago> getAll() throws Throwable {
        return tipoPagoDaoService.selectAll(NombreEntidadesCredito.TIPO_PAGO);
    }

    /**
     * GET - Obtener por ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TipoPago getId(@PathParam("id") Long id) throws Throwable {
        return tipoPagoDaoService.selectById(id, NombreEntidadesCredito.TIPO_PAGO);
    }

    /**
     * PUT - Actualizar o crear registro
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoPago put(TipoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return tipoPagoService.saveSingle(registro);
    }

    /**
     * POST - Crear registro
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoPago post(TipoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return tipoPagoService.saveSingle(registro);
    }

    /**
     * POST - Select by criteria
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Tipo Pago");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoPagoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return respuesta;
    }

    /**
     * DELETE - Eliminar registro por ID
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        TipoPago elimina = new TipoPago();
        tipoPagoDaoService.remove(elimina, id);
    }
}
