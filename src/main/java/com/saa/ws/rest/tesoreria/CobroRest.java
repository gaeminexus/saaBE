package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CobroDaoService;
import com.saa.ejb.tesoreria.service.CobroService;
import com.saa.model.tesoreria.Cobro;
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

@Path("cbro")
public class CobroRest {

    @EJB
    private CobroDaoService cobroDaoService;

    @EJB
    private CobroService cobroService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Cobro.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Cobro> getAll() throws Throwable {
        return cobroDaoService.selectAll(NombreEntidadesTesoreria.COBRO);
    }

    /**
     * Recupera un registro de Cobro por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Cobro getId(@PathParam("id") Long id) throws Throwable {
        return cobroDaoService.selectById(id, NombreEntidadesTesoreria.COBRO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Cobro put(Cobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO");
        return cobroService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Cobro post(Cobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO");
        return cobroService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CobroRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de COBRO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Cobro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO");
        Cobro elimina = new Cobro();
        cobroDaoService.remove(elimina, id);
    }
}
