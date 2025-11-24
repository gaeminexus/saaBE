package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempMotivoPagoDaoService;
import com.saa.ejb.tesoreria.service.TempMotivoPagoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempMotivoPago;

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

@Path("tpmt")
public class TempMotivoPagoRest {

    @EJB
    private TempMotivoPagoDaoService tempMotivoPagoDaoService;

    @EJB
    private TempMotivoPagoService tempMotivoPagoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempMotivoPagoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempMotivoPago.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempMotivoPago> getAll() throws Throwable {
        return tempMotivoPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
    }

    /**
     * Recupera un registro de TempMotivoPago por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempMotivoPago getId(@PathParam("id") Long id) throws Throwable {
        return tempMotivoPagoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempMotivoPago put(TempMotivoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_MOTIVO_PAGO");
        return tempMotivoPagoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempMotivoPago post(TempMotivoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_MOTIVO_PAGO");
        return tempMotivoPagoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempMotivoPagoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_MOTIVO_PAGO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempMotivoPagoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de TempMotivoPago por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_MOTIVO_PAGO");
        TempMotivoPago elimina = new TempMotivoPago();
        tempMotivoPagoDaoService.remove(elimina, id);
    }
}
