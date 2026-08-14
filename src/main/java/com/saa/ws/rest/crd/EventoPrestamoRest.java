package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.ejb.crd.service.EventoPrestamoService;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST de solo lectura para EventoPrestamo (EVPR).
 *
 * NO expone POST/PUT/DELETE de creación: los eventos SOLO se crean y anulan desde los
 * procesos de pago de préstamos (§7 de ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md).
 */
@Path("evpr")
public class EventoPrestamoRest {

    @EJB
    private EventoPrestamoDaoService eventoPrestamoDaoService;

    @EJB
    private EventoPrestamoService eventoPrestamoService;

    @Context
    private UriInfo context;

    public EventoPrestamoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - EVENTO_PRESTAMO");
        try {
            List<EventoPrestamo> lista = eventoPrestamoDaoService.selectAll(NombreEntidadesCredito.EVENTO_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener eventos de préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - EVENTO_PRESTAMO: " + id);
        try {
            EventoPrestamo evento = eventoPrestamoDaoService.selectById(id, NombreEntidadesCredito.EVENTO_PRESTAMO);
            if (evento == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Evento de préstamo con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(evento).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener evento de préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/porPrestamo/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porPrestamo(@PathParam("idPrestamo") Long idPrestamo) {
        System.out.println("LLEGA AL SERVICIO GET POR PRESTAMO - EVENTO_PRESTAMO: " + idPrestamo);
        try {
            List<EventoPrestamo> lista = eventoPrestamoService.listarPorPrestamo(idPrestamo);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener eventos del préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de EVENTO_PRESTAMO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(eventoPrestamoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
}
