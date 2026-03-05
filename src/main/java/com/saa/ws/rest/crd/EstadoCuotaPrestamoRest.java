package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.EstadoCuotaPrestamoDaoService;
import com.saa.ejb.crd.service.EstadoCuotaPrestamoService;
import com.saa.model.crd.EstadoCuotaPrestamo;
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

/**
 * Servicio REST para EstadoCuotaPrestamo.
 * @author GaemiSoft
 */
@Path("escp")
public class EstadoCuotaPrestamoRest {
    
    @EJB
    private EstadoCuotaPrestamoDaoService estadoCuotaPrestamoDaoService;
    
    @EJB
    private EstadoCuotaPrestamoService estadoCuotaPrestamoService;
    
    @Context
    private UriInfo context;
    
    public EstadoCuotaPrestamoRest() {
    }
    
    /**
     * GET - Obtener todos los estados de cuota de préstamo
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<EstadoCuotaPrestamo> lista = estadoCuotaPrestamoDaoService.selectAll(NombreEntidadesCredito.ESTADO_CUOTA_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener estados de cuota de préstamo: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * GET - Obtener un estado de cuota de préstamo por ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            EstadoCuotaPrestamo estado = estadoCuotaPrestamoDaoService.selectById(id, NombreEntidadesCredito.ESTADO_CUOTA_PRESTAMO);
            if (estado == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Estado de cuota de préstamo con ID " + id + " no encontrado")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(estado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener estado de cuota de préstamo: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * PUT - Actualizar un estado de cuota de préstamo
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(EstadoCuotaPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ESTADO_CUOTA_PRESTAMO");
        try {
            EstadoCuotaPrestamo resultado = estadoCuotaPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar estado de cuota de préstamo: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * POST - Crear un nuevo estado de cuota de préstamo
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(EstadoCuotaPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST - ESTADO_CUOTA_PRESTAMO");
        try {
            EstadoCuotaPrestamo resultado = estadoCuotaPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al crear estado de cuota de préstamo: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * POST - Búsqueda por criterios
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de ESTADO_CUOTA_PRESTAMO");
        try {
            return Response.status(Response.Status.OK)
                .entity(estadoCuotaPrestamoService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * DELETE - Eliminar un estado de cuota de préstamo
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ESTADO_CUOTA_PRESTAMO con id: " + id);
        try {
            EstadoCuotaPrestamo elimina = new EstadoCuotaPrestamo();
            estadoCuotaPrestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.OK)
                .entity("Estado de cuota de préstamo eliminado exitosamente")
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar estado de cuota de préstamo: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
