package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.PersonaDaoService;
import com.saa.ejb.tesoreria.service.PersonaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.Persona;

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
import jakarta.ws.rs.core.UriInfo;

@Path("prsn")
public class PersonaRest {

    @EJB
    private PersonaDaoService personaDaoService;

    @EJB
    private PersonaService personaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public PersonaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Persona.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Persona> getAll() throws Throwable {
        return personaDaoService.selectAll(NombreEntidadesTesoreria.PERSONA);
    }

    /**
     * Recupera un registro de Persona por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Persona getId(@PathParam("id") Long id) throws Throwable {
        return personaDaoService.selectById(id, NombreEntidadesTesoreria.PERSONA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Persona put(Persona registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT PERSONA");
        return personaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Persona post(Persona registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST PERSONA");
        return personaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de Persona por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Persona> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA PERSONA: " + test);
        return personaDaoService.selectAll(NombreEntidadesTesoreria.PERSONA);
    }

    /**
     * Elimina un registro de Persona por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE PERSONA");
        Persona elimina = new Persona();
        personaDaoService.remove(elimina, id);
    }
}
