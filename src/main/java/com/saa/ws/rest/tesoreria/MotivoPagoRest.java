package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.MotivoPagoDaoService;
import com.saa.ejb.tesoreria.service.MotivoPagoService;
import com.saa.model.tesoreria.MotivoPago;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

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

@Path("pmtv")
public class MotivoPagoRest {

    @EJB
    private MotivoPagoDaoService motivoPagoDaoService;

    @EJB
    private MotivoPagoService motivoPagoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public MotivoPagoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de MotivoPago.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MotivoPago> getAll() throws Throwable {
        return motivoPagoDaoService.selectAll(NombreEntidadesTesoreria.MOTIVO_PAGO);
    }

    /**
     * Recupera un registro de MotivoPago por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public MotivoPago getId(@PathParam("id") Long id) throws Throwable {
        return motivoPagoDaoService.selectById(id, NombreEntidadesTesoreria.MOTIVO_PAGO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public MotivoPago put(MotivoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT MOTIVO PAGO");
        return motivoPagoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public MotivoPago post(MotivoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST MOTIVO PAGO");
        return motivoPagoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MotivoPagoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MOTIVO_PAGO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(motivoPagoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de MotivoPago por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE MOTIVO PAGO");
        MotivoPago elimina = new MotivoPago();
        motivoPagoDaoService.remove(elimina, id);
    }
}
