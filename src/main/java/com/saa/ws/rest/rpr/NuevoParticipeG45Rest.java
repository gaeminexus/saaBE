package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.NuevoParticipeG45DaoService;
import com.saa.ejb.rpr.service.NuevoParticipeG45Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.NuevoParticipeG45;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cg45")
public class NuevoParticipeG45Rest {

    @EJB private NuevoParticipeG45DaoService nuevoParticipeG45DaoService;
    @EJB private NuevoParticipeG45Service nuevoParticipeG45Service;
    @Context private UriInfo context;

    public NuevoParticipeG45Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NuevoParticipeG45> lista = nuevoParticipeG45DaoService.selectAll(NombreEntidadesReporte.NUEVO_PARTICIPE_G45);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener NuevoParticipeG45: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NuevoParticipeG45 entidad = nuevoParticipeG45DaoService.selectById(id, NombreEntidadesReporte.NUEVO_PARTICIPE_G45);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("NuevoParticipeG45 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener NuevoParticipeG45: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(NuevoParticipeG45 registro) {
        System.out.println("LLEGA AL SERVICIO PUT NuevoParticipeG45");
        try {
            return Response.status(Response.Status.OK).entity(nuevoParticipeG45Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar NuevoParticipeG45: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(NuevoParticipeG45 registro) {
        System.out.println("LLEGA AL SERVICIO POST NuevoParticipeG45");
        try {
            return Response.status(Response.Status.CREATED).entity(nuevoParticipeG45Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear NuevoParticipeG45: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE NuevoParticipeG45 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            nuevoParticipeG45Service.remove(ids);
            return Response.status(Response.Status.OK).entity("NuevoParticipeG45 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar NuevoParticipeG45: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria NuevoParticipeG45");
        try {
            return Response.status(Response.Status.OK).entity(nuevoParticipeG45Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria NuevoParticipeG45: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
