package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.BaseInicialParticipesDaoService;
import com.saa.ejb.crd.service.BaseInicialParticipesService;
import com.saa.model.crd.BaseInicialParticipes;
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

@Path("bipr")
public class BaseInicialParticipesRest {
    
    @EJB
    private BaseInicialParticipesDaoService baseInicialParticipesDaoService;
    
    @EJB
    private BaseInicialParticipesService baseInicialParticipesService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public BaseInicialParticipesRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de BaseInicialParticipes.
     * 
     * @return Response con lista de BaseInicialParticipes
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<BaseInicialParticipes> registros = baseInicialParticipesDaoService.selectAll(NombreEntidadesCredito.BASE_INICIAL_PARTICIPES);
            return Response.status(Response.Status.OK)
                    .entity(registros)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener base inicial partícipes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Obtiene un registro de BaseInicialParticipes por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto BaseInicialParticipes
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            BaseInicialParticipes registro = baseInicialParticipesDaoService.selectById(id, NombreEntidadesCredito.BASE_INICIAL_PARTICIPES);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("BaseInicialParticipes con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(registro)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener base inicial partícipes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de BaseInicialParticipes (PUT).
     * 
     * @param registro Objeto BaseInicialParticipes
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(BaseInicialParticipes registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE BaseInicialParticipes");
        try {
            BaseInicialParticipes resultado = baseInicialParticipesService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar base inicial partícipes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de BaseInicialParticipes (POST).
     * 
     * @param registro Objeto BaseInicialParticipes
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(BaseInicialParticipes registro) {
        System.out.println("LLEGA AL SERVICIO POST DE BaseInicialParticipes");
        try {
            BaseInicialParticipes resultado = baseInicialParticipesService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear base inicial partícipes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Busca registros de BaseInicialParticipes por criterios específicos.
     * 
     * @param registros Lista de criterios de búsqueda
     * @return Response con lista de BaseInicialParticipes que coinciden con los criterios
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de BaseInicialParticipes");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(baseInicialParticipesService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return respuesta;
    }

    /**
     * Elimina un registro de BaseInicialParticipes por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE BaseInicialParticipes");
        try {
            BaseInicialParticipes elimina = new BaseInicialParticipes();
            baseInicialParticipesDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar base inicial partícipes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
