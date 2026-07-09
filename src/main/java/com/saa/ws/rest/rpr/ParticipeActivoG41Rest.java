package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.ParticipeActivoG41DaoService;
import com.saa.ejb.rpr.service.ParticipeActivoG41Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.ParticipeActivoG41;

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

@Path("cg41")
public class ParticipeActivoG41Rest {

    @EJB private ParticipeActivoG41DaoService participeActivoG41DaoService;
    @EJB private ParticipeActivoG41Service participeActivoG41Service;
    @Context private UriInfo context;

    public ParticipeActivoG41Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ParticipeActivoG41> lista = participeActivoG41DaoService.selectAll(NombreEntidadesReporte.PARTICIPE_ACTIVO_G41);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener ParticipeActivoG41: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ParticipeActivoG41 entidad = participeActivoG41DaoService.selectById(id, NombreEntidadesReporte.PARTICIPE_ACTIVO_G41);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("ParticipeActivoG41 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener ParticipeActivoG41: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(ParticipeActivoG41 registro) {
        System.out.println("LLEGA AL SERVICIO PUT ParticipeActivoG41");
        try {
            return Response.status(Response.Status.OK).entity(participeActivoG41Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar ParticipeActivoG41: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(ParticipeActivoG41 registro) {
        System.out.println("LLEGA AL SERVICIO POST ParticipeActivoG41");
        try {
            return Response.status(Response.Status.CREATED).entity(participeActivoG41Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear ParticipeActivoG41: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE ParticipeActivoG41 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            participeActivoG41Service.remove(ids);
            return Response.status(Response.Status.OK).entity("ParticipeActivoG41 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar ParticipeActivoG41: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria ParticipeActivoG41");
        try {
            return Response.status(Response.Status.OK).entity(participeActivoG41Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria ParticipeActivoG41: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
