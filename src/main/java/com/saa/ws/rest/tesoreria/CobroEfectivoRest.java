package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CobroEfectivoDaoService;
import com.saa.ejb.tesoreria.service.CobroEfectivoService;
import com.saa.model.tesoreria.CobroEfectivo;
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

@Path("cefc")
public class CobroEfectivoRest {

    @EJB
    private CobroEfectivoDaoService cobroEfectivoDaoService;

    @EJB
    private CobroEfectivoService cobroEfectivoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroEfectivoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroEfectivo.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CobroEfectivo> getAll() throws Throwable {
        return cobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.COBRO_EFECTIVO);
    }

    /**
     * Recupera un registro de CobroEfectivo por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CobroEfectivo getId(@PathParam("id") Long id) throws Throwable {
        return cobroEfectivoDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_EFECTIVO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CobroEfectivo put(CobroEfectivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO EFECTIVO");
        return cobroEfectivoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CobroEfectivo post(CobroEfectivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO EFECTIVO");
        return cobroEfectivoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CobroEfectivoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de COBRO_EFECTIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cobroEfectivoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de CobroEfectivo por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO EFECTIVO");
        CobroEfectivo elimina = new CobroEfectivo();
        cobroEfectivoDaoService.remove(elimina, id);
    }
}
