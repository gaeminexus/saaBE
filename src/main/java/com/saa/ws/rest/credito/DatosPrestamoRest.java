package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.DatosPrestamoDaoService;
import com.saa.ejb.credito.service.DatosPrestamoService;
import com.saa.model.credito.DatosPrestamo;
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

@Path("dtsp")
public class DatosPrestamoRest {

    @EJB
    private DatosPrestamoDaoService datosPrestamoDaoService;

    @EJB
    private DatosPrestamoService datosPrestamoService;

    @Context
    private UriInfo context;

    public DatosPrestamoRest() { }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DatosPrestamo> lista = datosPrestamoDaoService.selectAll(NombreEntidadesCredito.DATOS_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener datos de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DatosPrestamo datos = datosPrestamoDaoService.selectById(id, NombreEntidadesCredito.DATOS_PRESTAMO);
            if (datos == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("DatosPrestamo con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(datos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener datos de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DatosPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DTSP");
        try {
            DatosPrestamo resultado = datosPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar datos de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DatosPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST - DTSP");
        try {
            DatosPrestamo resultado = datosPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear datos de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DTSP");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(datosPrestamoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DTSP");
        try {
            DatosPrestamo elimina = new DatosPrestamo();
            datosPrestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar datos de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
