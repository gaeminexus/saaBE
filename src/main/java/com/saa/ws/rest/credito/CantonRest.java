package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.CantonDaoService;
import com.saa.ejb.crd.service.CantonService;
import com.saa.model.crd.Canton;
import com.saa.model.crd.NombreEntidadesCredito;

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

@Path("cntn")
public class CantonRest {

    @EJB
    private CantonDaoService cantonDaoService;

    @EJB
    private CantonService cantonService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public CantonRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Obtiene todos los registros de Canton.
     * 
     * @return Response con lista de Canton
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Canton> lista = cantonDaoService.selectAll(NombreEntidadesCredito.CANTON);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cantones: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene un registro de Canton por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Canton
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Canton canton = cantonDaoService.selectById(id, NombreEntidadesCredito.CANTON);
            if (canton == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Canton con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(canton)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea o actualiza un registro de Canton (PUT).
     * 
     * @param registro Objeto Canton
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Canton registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            Canton resultado = cantonService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea o actualiza un registro de Canton (POST).
     * 
     * @param registro Objeto Canton
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Canton registro) {
        System.out.println("LLEGA AL SERVICIO");
        try {
            Canton resultado = cantonService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST method for updating or creating an instance of CantonRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Canton");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cantonService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Canton por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE");
        try {
            Canton elimina = new Canton();
            cantonDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

}
