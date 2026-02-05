package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.TipoPrestamoDaoService;
import com.saa.ejb.crd.service.TipoPrestamoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoPrestamo;

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

@Path("tppr")
public class TipoPrestamoRest {
    
    @EJB
    private TipoPrestamoDaoService tipoPrestamoDaoService;
    
    @EJB
    private TipoPrestamoService tipoPrestamoService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public TipoPrestamoRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de TipoPrestamo.
     * 
     * @return Response con lista de TipoPrestamo
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TipoPrestamo> tipos = tipoPrestamoDaoService.selectAll(NombreEntidadesCredito.TIPO_PRESTAMO);
            return Response.status(Response.Status.OK)
                    .entity(tipos)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener tipos de préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Obtiene un registro de TipoPrestamo por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto TipoPrestamo
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            TipoPrestamo tipo = tipoPrestamoDaoService.selectById(id, NombreEntidadesCredito.TIPO_PRESTAMO);
            if (tipo == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("TipoPrestamo con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(tipo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener tipo de préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de TipoPrestamo (PUT).
     * 
     * @param registro Objeto TipoPrestamo
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TipoPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE TipoPrestamo");
        try {
            TipoPrestamo resultado = tipoPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar tipo de préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de TipoPrestamo (POST).
     * 
     * @param registro Objeto TipoPrestamo
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TipoPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST DE TipoPrestamo");
        try {
            TipoPrestamo resultado = tipoPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear tipo de préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoPrestamo");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoPrestamoService.selectByCriteria(registros))
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
     * Elimina un registro de TipoPrestamo por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE TipoPrestamo");
        try {
            TipoPrestamo elimina = new TipoPrestamo();
            tipoPrestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar tipo de préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
