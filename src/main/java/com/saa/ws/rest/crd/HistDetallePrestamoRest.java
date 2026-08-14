package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.service.HistDetallePrestamoService;
import com.saa.model.crd.HistDetallePrestamo;
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
 * REST de solo lectura para HistDetallePrestamo (HDTP).
 *
 * NO expone POST/PUT/DELETE de creación: las cuotas historizadas SOLO las escribe el
 * abono a capital / la precancelación dentro de su transacción.
 */
@Path("hdtp")
public class HistDetallePrestamoRest {

    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    @EJB
    private HistDetallePrestamoService histDetallePrestamoService;

    @Context
    private UriInfo context;

    public HistDetallePrestamoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - HIST_DETALLE_PRESTAMO");
        try {
            List<HistDetallePrestamo> lista = histDetallePrestamoDaoService.selectAll(NombreEntidadesCredito.HIST_DETALLE_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cuotas historizadas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - HIST_DETALLE_PRESTAMO: " + id);
        try {
            HistDetallePrestamo historico = histDetallePrestamoDaoService.selectById(id, NombreEntidadesCredito.HIST_DETALLE_PRESTAMO);
            if (historico == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Cuota historizada con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(historico).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cuota historizada: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/porEvento/{idEvento}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEvento(@PathParam("idEvento") Long idEvento) {
        System.out.println("LLEGA AL SERVICIO GET POR EVENTO - HIST_DETALLE_PRESTAMO: " + idEvento);
        try {
            List<HistDetallePrestamo> lista = histDetallePrestamoService.listarPorEvento(idEvento);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cuotas historizadas del evento: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/porPrestamo/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porPrestamo(@PathParam("idPrestamo") Long idPrestamo) {
        System.out.println("LLEGA AL SERVICIO GET POR PRESTAMO - HIST_DETALLE_PRESTAMO: " + idPrestamo);
        try {
            List<HistDetallePrestamo> lista = histDetallePrestamoService.listarPorPrestamo(idPrestamo);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cuotas historizadas del préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de HIST_DETALLE_PRESTAMO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(histDetallePrestamoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
}
