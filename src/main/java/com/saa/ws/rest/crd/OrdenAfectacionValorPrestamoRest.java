package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.OrdenAfectacionValorPrestamoDaoService;
import com.saa.ejb.crd.service.OrdenAfectacionValorPrestamoService;
import com.saa.model.crd.OrdenAfectacionValorPrestamo;
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
 * Servicio REST para OrdenAfectacionValorPrestamo.
 * @author GaemiSoft
 */
@Path("oavp")
public class OrdenAfectacionValorPrestamoRest {
    
    @EJB
    private OrdenAfectacionValorPrestamoDaoService ordenAfectacionValorPrestamoDaoService;
    
    @EJB
    private OrdenAfectacionValorPrestamoService ordenAfectacionValorPrestamoService;
    
    @Context
    private UriInfo context;
    
    public OrdenAfectacionValorPrestamoRest() {
    }
    
    /**
     * GET - Obtener todos los órdenes de afectación
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<OrdenAfectacionValorPrestamo> lista = ordenAfectacionValorPrestamoDaoService.selectAll(
                NombreEntidadesCredito.ORDEN_AFECTACION_VALOR_PRESTAMO
            );
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener órdenes de afectación: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * GET - Obtener todos los órdenes de afectación ordenados
     */
    @GET
    @Path("/getAllOrdenado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllOrdenado() {
        try {
            List<OrdenAfectacionValorPrestamo> lista = ordenAfectacionValorPrestamoService.selectAllOrdenado();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener órdenes de afectación ordenados: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * GET - Obtener un orden de afectación por ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            OrdenAfectacionValorPrestamo orden = ordenAfectacionValorPrestamoDaoService.selectById(
                id, NombreEntidadesCredito.ORDEN_AFECTACION_VALOR_PRESTAMO
            );
            if (orden == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Orden de afectación con ID " + id + " no encontrado")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(orden).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener orden de afectación: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * PUT - Actualizar un orden de afectación
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(OrdenAfectacionValorPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ORDEN_AFECTACION_VALOR_PRESTAMO");
        try {
            OrdenAfectacionValorPrestamo resultado = ordenAfectacionValorPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar orden de afectación: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * POST - Crear un nuevo orden de afectación
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(OrdenAfectacionValorPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST - ORDEN_AFECTACION_VALOR_PRESTAMO");
        try {
            OrdenAfectacionValorPrestamo resultado = ordenAfectacionValorPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al crear orden de afectación: " + e.getMessage())
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
        System.out.println("selectByCriteria de ORDEN_AFECTACION_VALOR_PRESTAMO");
        try {
            return Response.status(Response.Status.OK)
                .entity(ordenAfectacionValorPrestamoService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * DELETE - Eliminar un orden de afectación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ORDEN_AFECTACION_VALOR_PRESTAMO con id: " + id);
        try {
            OrdenAfectacionValorPrestamo elimina = new OrdenAfectacionValorPrestamo();
            ordenAfectacionValorPrestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.OK)
                .entity("Orden de afectación eliminado exitosamente")
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar orden de afectación: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
