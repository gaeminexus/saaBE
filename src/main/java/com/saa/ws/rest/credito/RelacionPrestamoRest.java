package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.RelacionPrestamoDaoService;
import com.saa.ejb.credito.service.RelacionPrestamoService;
import com.saa.model.credito.RelacionPrestamo;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("rlpr")
public class RelacionPrestamoRest {

    @EJB
    private RelacionPrestamoDaoService relacionPrestamoDaoService;

    @EJB
    private RelacionPrestamoService relacionPrestamoService;

    @Context
    private UriInfo context;

    public RelacionPrestamoRest() { }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<RelacionPrestamo> lista = relacionPrestamoDaoService.selectAll(NombreEntidadesCredito.RELACION_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener relaciones de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            RelacionPrestamo relacion = relacionPrestamoDaoService.selectById(id, NombreEntidadesCredito.RELACION_PRESTAMO);
            if (relacion == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("RelacionPrestamo con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(relacion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener relación de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(RelacionPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - RLPR");
        try {
            RelacionPrestamo resultado = relacionPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar relación de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(RelacionPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST - RLPR");
        try {
            RelacionPrestamo resultado = relacionPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear relación de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de RLPR");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(relacionPrestamoService.selectByCriteria(registros))
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
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - RLPR");
        try {
            RelacionPrestamo elimina = new RelacionPrestamo();
            relacionPrestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar relación de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
