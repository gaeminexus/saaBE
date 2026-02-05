package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.ExterDaoService;
import com.saa.ejb.crd.service.ExterService;
import com.saa.model.crd.Exter;
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

@Path("extr")
public class ExterRest {

    @EJB
    private ExterDaoService exterDaoService;

    @EJB
    private ExterService exterService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ExterRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Obtiene todos los registros de Exter.
     * 
     * @return Response con lista de Exter
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Exter> lista = exterDaoService.selectAll(NombreEntidadesCredito.EXTER);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener registros: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene un registro de Exter por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Exter
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Exter exter = exterDaoService.selectById(id, NombreEntidadesCredito.EXTER);
            if (exter == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Exter con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(exter)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener registro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea o actualiza un registro de Exter (PUT).
     * 
     * @param registro Objeto Exter
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Exter registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            Exter resultado = exterService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar registro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea o actualiza un registro de Exter (POST).
     * 
     * @param registro Objeto Exter
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Exter registro) {
        System.out.println("LLEGA AL SERVICIO");
        try {
            Exter resultado = exterService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear registro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST method for updating or creating an instance of ExterRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Exter");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(exterService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Exter por ID.
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
            Exter elimina = new Exter();
            exterDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar registro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

}
