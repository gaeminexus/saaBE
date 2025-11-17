package com.saa.ws.rest.tesoreria;

import java.util.List;

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
     * Selecciona registros de DireccionPersona por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DireccionPersona> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DIRECCION PERSONA: " + test);
        return direccionPersonaDaoService.selectAll(NombreEntidadesTesoreria.DIRECCION_PERSONA);
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
