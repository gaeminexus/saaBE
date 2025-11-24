package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroEfectivoDaoService;
import com.saa.ejb.tesoreria.service.TempCobroEfectivoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroEfectivo;

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

@Path("tcef")
public class TempCobroEfectivoRest {

    @EJB
    private TempCobroEfectivoDaoService tempCobroEfectivoDaoService;

    @EJB
    private TempCobroEfectivoService tempCobroEfectivoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroEfectivoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroEfectivo.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobroEfectivo> getAll() throws Throwable {
        return tempCobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
    }

    /**
     * Recupera un registro de TempCobroEfectivo por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobroEfectivo getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroEfectivoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobroEfectivo put(TempCobroEfectivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO_EFECTIVO");
        return tempCobroEfectivoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobroEfectivo post(TempCobroEfectivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO_EFECTIVO");
        return tempCobroEfectivoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempCobroEfectivoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_COBRO_EFECTIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempCobroEfectivoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de TempCobroEfectivo por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO_EFECTIVO");
        TempCobroEfectivo elimina = new TempCobroEfectivo();
        tempCobroEfectivoDaoService.remove(elimina, id);
    }
}
