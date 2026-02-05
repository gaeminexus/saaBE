package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.model.crd.Entidad;
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

@Path("entd")
public class EntidadRest {

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private EntidadService entidadService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public EntidadRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Obtiene todos los registros de Entidad.
     * 
     * @return Response con lista de Entidad
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Entidad> lista = entidadDaoService.selectAll(NombreEntidadesCredito.ENTIDAD);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener entidades: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene un registro de Entidad por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Entidad
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Entidad entidad = entidadDaoService.selectById(id, NombreEntidadesCredito.ENTIDAD);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Entidad con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(entidad)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener entidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Obtiene entidades por coincidencias de nombre.
     * 
     * @param nombre Nombre a buscar
     * @return Response con lista de Entidad
     */
    @GET
    @Path("/getCoincidencias/{nombre}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCoincidencias(@PathParam("nombre") String nombre) {
        try {
            List<Entidad> lista = entidadService.selectCoincidenciasByNombre(nombre);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al buscar coincidencias: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Recupera las entidades cuyo nombre completo coincide con el proporcionado por petro de 35 caracteres.
     * 
     * @param nombre Nombre a buscar
     * @return Response con lista de Entidad
     */
    @GET
    @Path("/getByNombrePetro35/{nombre}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByNombrePetro35(@PathParam("nombre") String nombre) {
        try {
            List<Entidad> lista = entidadDaoService.selectByNombrePetro35(nombre);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al buscar por nombre Petro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * PUT method for updating or creating an instance of EntidadRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response put(Entidad registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(entidadService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of EntidadRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Response post(Entidad registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(entidadService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of EntidadRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Entidad");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(entidadService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Entidad por ID.
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
            Entidad elimina = new Entidad();
            entidadDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar entidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

}
