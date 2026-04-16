package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.DetalleGeneracionArchivoDaoService;
import com.saa.ejb.crd.service.DetalleGeneracionArchivoService;
import com.saa.model.crd.DetalleGeneracionArchivo;
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

@Path("dtga")
public class DetalleGeneracionArchivoRest {

    @EJB
    private DetalleGeneracionArchivoDaoService detalleGeneracionArchivoDaoService;

    @EJB
    private DetalleGeneracionArchivoService detalleGeneracionArchivoService;

    @Context
    private UriInfo context;

    public DetalleGeneracionArchivoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetalleGeneracionArchivo> lista = detalleGeneracionArchivoDaoService.selectAll(NombreEntidadesCredito.DETALLE_GENERACION_ARCHIVO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalles de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetalleGeneracionArchivo detalle = detalleGeneracionArchivoDaoService.selectById(id, NombreEntidadesCredito.DETALLE_GENERACION_ARCHIVO);
            if (detalle == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Detalle de generación con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(detalle).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalle de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleGeneracionArchivo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE_GENERACION_ARCHIVO");
        try {
            DetalleGeneracionArchivo resultado = detalleGeneracionArchivoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar detalle de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleGeneracionArchivo registro) {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE_GENERACION_ARCHIVO");
        try {
            DetalleGeneracionArchivo resultado = detalleGeneracionArchivoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear detalle de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_GENERACION_ARCHIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(detalleGeneracionArchivoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE_GENERACION_ARCHIVO");
        try {
            DetalleGeneracionArchivo elimina = new DetalleGeneracionArchivo();
            detalleGeneracionArchivoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar detalle de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
