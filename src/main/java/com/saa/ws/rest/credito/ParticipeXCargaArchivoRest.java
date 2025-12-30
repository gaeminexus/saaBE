package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ParticipeXCargaArchivoDaoService;
import com.saa.ejb.credito.service.ParticipeXCargaArchivoService;
import com.saa.model.credito.ParticipeXCargaArchivo;
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

@Path("pxca")
public class ParticipeXCargaArchivoRest {

    @EJB
    private ParticipeXCargaArchivoDaoService participeXCargaArchivoDaoService;

    @EJB
    private ParticipeXCargaArchivoService participeXCargaArchivoService;

    @Context
    private UriInfo context;

    public ParticipeXCargaArchivoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ParticipeXCargaArchivo> lista = participeXCargaArchivoDaoService.selectAll(NombreEntidadesCredito.PARTICIPE_X_CARGA_ARCHIVO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener partícipes por carga: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ParticipeXCargaArchivo participe = participeXCargaArchivoDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE_X_CARGA_ARCHIVO);
            if (participe == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("ParticipeXCargaArchivo con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(participe).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener partícipe por carga: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ParticipeXCargaArchivo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PARTICIPEXCARGAARCHIVO");
        try {
            ParticipeXCargaArchivo resultado = participeXCargaArchivoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar partícipe por carga: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ParticipeXCargaArchivo registro) {
        System.out.println("LLEGA AL SERVICIO POST - PARTICIPEXCARGAARCHIVO");
        try {
            ParticipeXCargaArchivo resultado = participeXCargaArchivoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear partícipe por carga: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PARTICIPEXCARGAARCHIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(participeXCargaArchivoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PARTICIPEXCARGAARCHIVO");
        try {
            ParticipeXCargaArchivo elimina = new ParticipeXCargaArchivo();
            participeXCargaArchivoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar partícipe por carga: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
