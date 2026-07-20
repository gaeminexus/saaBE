package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.PathNegociacionDaoService;
import com.saa.ejb.cxp.service.PathNegociacionService;
import com.saa.model.cxp.PathNegociacion;
import com.saa.model.cxp.NombreEntidadesCompra;

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

@Path("ptng")
public class PathNegociacionRest {

    @EJB
    private PathNegociacionDaoService pathNegociacionDaoService;

    @EJB
    private PathNegociacionService pathNegociacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PathNegociacionRest() {
    }

    /**
     * Obtiene todos los documentos digitalizados de negociaciones.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PathNegociacion> lista = pathNegociacionDaoService.selectAll(NombreEntidadesCompra.PATH_NEGOCIACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener documentos de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un documento digitalizado de negociación por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PathNegociacion entidad = pathNegociacionDaoService.selectById(id, NombreEntidadesCompra.PATH_NEGOCIACION);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Documento de negociación con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener documento de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza un documento digitalizado de negociación existente.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PathNegociacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PATH_NEGOCIACION");
        try {
            PathNegociacion resultado = pathNegociacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar documento de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra un nuevo documento digitalizado asociado a una negociación.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PathNegociacion registro) {
        System.out.println("LLEGA AL SERVICIO POST - PATH_NEGOCIACION");
        try {
            PathNegociacion resultado = pathNegociacionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear documento de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca documentos digitalizados de negociación por criterios.
     * Uso típico: filtrar por negociacion.id para obtener todos los documentos de una negociación.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de PATH_NEGOCIACION");
        try {
            List<PathNegociacion> lista = pathNegociacionService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un documento digitalizado de negociación por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PATH_NEGOCIACION id: " + id);
        try {
            PathNegociacion elimina = new PathNegociacion();
            pathNegociacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar documento de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}