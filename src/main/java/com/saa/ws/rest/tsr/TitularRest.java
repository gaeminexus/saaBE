package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.TitularDaoService;

import com.saa.ejb.tsr.service.TitularService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Titular;

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

@Path("ttlr")
public class TitularRest {

    @EJB
    private TitularDaoService titularDaoService;
    
    @EJB
    private TitularService titularService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TitularRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Persona.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Titular> lista = titularDaoService.selectAll(NombreEntidadesTesoreria.TITULAR);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener personas: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de Persona por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Titular persona = titularDaoService.selectById(id, NombreEntidadesTesoreria.TITULAR);
            if (persona == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Persona con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(persona).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Titular registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TITULAR");
        try {
            Titular resultado = titularService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Titular registro) {
        System.out.println("LLEGA AL SERVICIO POST - PERSONA");
        try {
        	Titular resultado = titularService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of PersonaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de PERSONA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(titularService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de Persona por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PERSONA");
        try {
            Titular elimina = new Titular();
            titularDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar titular: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
