package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.ParticipeDetalleGeneracionArchivoDaoService;
import com.saa.ejb.crd.service.ParticipeDetalleGeneracionArchivoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.ParticipeDetalleGeneracionArchivo;

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

@Path("pdga")
public class ParticipeDetalleGeneracionArchivoRest {

    @EJB
    private ParticipeDetalleGeneracionArchivoDaoService participeDetalleGeneracionArchivoDaoService;

    @EJB
    private ParticipeDetalleGeneracionArchivoService participeDetalleGeneracionArchivoService;

    @Context
    private UriInfo context;

    public ParticipeDetalleGeneracionArchivoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ParticipeDetalleGeneracionArchivo> lista = participeDetalleGeneracionArchivoDaoService.selectAll(NombreEntidadesCredito.PARTICIPE_DETALLE_GENERACION_ARCHIVO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener partícipes de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ParticipeDetalleGeneracionArchivo participe = participeDetalleGeneracionArchivoDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE_DETALLE_GENERACION_ARCHIVO);
            if (participe == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Partícipe de generación con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(participe).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener partícipe de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ParticipeDetalleGeneracionArchivo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PARTICIPE_DETALLE_GENERACION_ARCHIVO");
        try {
            ParticipeDetalleGeneracionArchivo resultado = participeDetalleGeneracionArchivoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar partícipe de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ParticipeDetalleGeneracionArchivo registro) {
        System.out.println("LLEGA AL SERVICIO POST - PARTICIPE_DETALLE_GENERACION_ARCHIVO");
        try {
            ParticipeDetalleGeneracionArchivo resultado = participeDetalleGeneracionArchivoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear partícipe de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PARTICIPE_DETALLE_GENERACION_ARCHIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(participeDetalleGeneracionArchivoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - PARTICIPE_DETALLE_GENERACION_ARCHIVO");
        try {
            ParticipeDetalleGeneracionArchivo elimina = new ParticipeDetalleGeneracionArchivo();
            participeDetalleGeneracionArchivoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar partícipe de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
