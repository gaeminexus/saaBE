package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempPagoDaoService;
import com.saa.ejb.tesoreria.service.TempPagoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempPago;

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

@Path("tpgs")
public class TempPagoRest {

    @EJB
    private TempPagoDaoService tempPagoDaoService;

    @EJB
    private TempPagoService tempPagoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempPagoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempPago.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempPago> getAll() throws Throwable {
        return tempPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_PAGO);
    }

    /**
     * Recupera un registro de TempPago por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempPago getId(@PathParam("id") Long id) throws Throwable {
        return tempPagoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_PAGO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempPago put(TempPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_PAGO");
        return tempPagoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempPago post(TempPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_PAGO");
        return tempPagoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempPagoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_PAGO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempPagoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de TempPago por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_PAGO");
        TempPago elimina = new TempPago();
        tempPagoDaoService.remove(elimina, id);
    }
}
