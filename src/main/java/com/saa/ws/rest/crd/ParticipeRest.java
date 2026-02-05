package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.ejb.crd.service.ParticipeService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Participe;

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

@Path("prtc")
public class ParticipeRest {

    @EJB
    private ParticipeDaoService participeDaoService;

    @EJB
    private ParticipeService participeService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ParticipeRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Obtiene todos los registros de Participe.
     * 
     * @return Response con lista de Participe
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Participe> participes = participeDaoService.selectAll(NombreEntidadesCredito.PARTICIPE);
            return Response.status(Response.Status.OK)
                    .entity(participes)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener partícipes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene un registro de Participe por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Participe
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Participe participe = participeDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE);
            if (participe == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Participe con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(participe)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener partícipe: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea o actualiza un registro de Participe (PUT).
     * 
     * @param registro Objeto Participe
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Participe registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PARTICIPE");
        try {
            Participe resultado = participeService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar partícipe: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea o actualiza un registro de Participe (POST).
     * 
     * @param registro Objeto Participe
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Participe registro) {
        System.out.println("LLEGA AL SERVICIO POST - PARTICIPE");
        try {
            Participe resultado = participeService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear partícipe: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST method for updating or creating an instance of ParticipeRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Participe");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(participeService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Participe por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PARTICIPE");
        try {
            Participe elimina = new Participe();
            participeDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar partícipe: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

}
