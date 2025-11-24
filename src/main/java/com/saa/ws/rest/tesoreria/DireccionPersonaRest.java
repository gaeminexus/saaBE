package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DireccionPersonaDaoService;
import com.saa.ejb.tesoreria.service.DireccionPersonaService;
import com.saa.model.tesoreria.DireccionPersona;
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

@Path("pdrc")
public class DireccionPersonaRest {

    @EJB
    private DireccionPersonaDaoService direccionPersonaDaoService;

    @EJB
    private DireccionPersonaService direccionPersonaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DireccionPersonaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DireccionPersona.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DireccionPersona> getAll() throws Throwable {
        return direccionPersonaDaoService.selectAll(NombreEntidadesTesoreria.DIRECCION_PERSONA);
    }

    /**
     * Recupera un registro de DireccionPersona por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DireccionPersona getId(@PathParam("id") Long id) throws Throwable {
        return direccionPersonaDaoService.selectById(id, NombreEntidadesTesoreria.DIRECCION_PERSONA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DireccionPersona put(DireccionPersona registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DIRECCION PERSONA");
        return direccionPersonaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DireccionPersona post(DireccionPersona registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DIRECCION PERSONA");
        return direccionPersonaService.saveSingle(registro);
    }
/**
 * POST method for updating or creating an instance of DireccionPersonaRest
 *
 * @param content representation for the resource
 * @return an HTTP response with content of the updated or created resource.
 */
@POST
@Path("selectByCriteria")
@Consumes("application/json")
public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
    System.out.println("selectByCriteria de DIRECCION_PERSONA");
    Response respuesta = null;
    try {
        respuesta = Response.status(Response.Status.OK).entity(direccionPersonaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
    } catch (Throwable e) {
        respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
    }
    return respuesta;
}

    /**
     * Elimina un registro de DireccionPersona por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DIRECCION PERSONA");
        DireccionPersona elimina = new DireccionPersona();
        direccionPersonaDaoService.remove(elimina, id);
    }
}
