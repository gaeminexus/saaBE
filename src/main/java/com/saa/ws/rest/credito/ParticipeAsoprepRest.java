package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ParticipeAsoprepDaoService;
import com.saa.ejb.credito.service.ParticipeAsoprepService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.ParticipeAsoprep;

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

@Path("pras")
public class ParticipeAsoprepRest {

    @EJB
    private ParticipeAsoprepDaoService ParticipeAsoprepDaoService;

    @EJB
    private ParticipeAsoprepService ParticipeAsoprepService;

    @Context
    private UriInfo context;

    public ParticipeAsoprepRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ParticipeAsoprep> lista = ParticipeAsoprepDaoService.selectAll(NombreEntidadesCredito.PARTICIPE_ASOPREP);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener ParticipeAsopreps: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ParticipeAsoprep ParticipeAsoprep = ParticipeAsoprepDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE_ASOPREP);
            if (ParticipeAsoprep == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("ParticipeAsoprep con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(ParticipeAsoprep).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener ParticipeAsoprep: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ParticipeAsoprep registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PRAS");
        try {
            ParticipeAsoprep resultado = ParticipeAsoprepService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar ParticipeAsoprep: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ParticipeAsoprep registro) {
        System.out.println("LLEGA AL SERVICIO POST - PRAS");
        try {
            ParticipeAsoprep resultado = ParticipeAsoprepService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear ParticipeAsoprep: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PRAS");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                .entity(ParticipeAsoprepService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - PRAS");
        try {
            ParticipeAsoprep elimina = new ParticipeAsoprep();
            ParticipeAsoprepDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar ParticipeAsoprep: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
