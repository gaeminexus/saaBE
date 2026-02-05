package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;

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

@Path("pgpr")
public class PagoPrestamoRest {

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private PagoPrestamoService pagoPrestamoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public PagoPrestamoRest() {
        // Constructor vacío
    }

    /**
     * Obtiene todos los registros de PagoPrestamo.
     * 
     * @return Lista de PagoPrestamo
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PagoPrestamo> lista = pagoPrestamoDaoService.selectAll(NombreEntidadesCredito.PAGO_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener pagos de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            PagoPrestamo pago = pagoPrestamoDaoService.selectById(id, NombreEntidadesCredito.PAGO_PRESTAMO);
            if (pago == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("PagoPrestamo con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(pago).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener pago de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PagoPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE PagoPrestamo");
        try {
            PagoPrestamo resultado = pagoPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar pago de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PagoPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST DE PagoPrestamo");
        try {
            PagoPrestamo resultado = pagoPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear pago de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PagoPrestamo");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(pagoPrestamoService.selectByCriteria(registros))
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

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE PagoPrestamo");
        try {
            PagoPrestamo elimina = new PagoPrestamo();
            pagoPrestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar pago de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
