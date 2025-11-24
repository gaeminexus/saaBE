package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.PersonaRolDaoService;
import com.saa.ejb.tesoreria.service.PersonaRolService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.PersonaRol;

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

@Path("prrl")
public class PersonaRolRest {

    @EJB
    private PersonaRolDaoService personaRolDaoService;

    @EJB
    private PersonaRolService personaRolService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public PersonaRolRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de PersonaRol.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PersonaRol> getAll() throws Throwable {
        return personaRolDaoService.selectAll(NombreEntidadesTesoreria.PERSONA_ROL);
    }

    /**
     * Recupera un registro de PersonaRol por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public PersonaRol getId(@PathParam("id") Long id) throws Throwable {
        return personaRolDaoService.selectById(id, NombreEntidadesTesoreria.PERSONA_ROL);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public PersonaRol put(PersonaRol registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT PERSONA_ROL");
        return personaRolService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public PersonaRol post(PersonaRol registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST PERSONA_ROL");
        return personaRolService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PersonaRolRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PERSONA_ROL");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(personaRolService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de PersonaRol por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE PERSONA_ROL");
        PersonaRol elimina = new PersonaRol();
        personaRolDaoService.remove(elimina, id);
    }
}
