package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.NuevoPrestamoG46DaoService;
import com.saa.ejb.rpr.service.NuevoPrestamoG46Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.NuevoPrestamoG46;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cg46")
public class NuevoPrestamoG46Rest {

    @EJB private NuevoPrestamoG46DaoService nuevoPrestamoG46DaoService;
    @EJB private NuevoPrestamoG46Service nuevoPrestamoG46Service;
    @Context private UriInfo context;

    public NuevoPrestamoG46Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NuevoPrestamoG46> lista = nuevoPrestamoG46DaoService.selectAll(NombreEntidadesReporte.NUEVO_PRESTAMO_G46);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener NuevoPrestamoG46: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NuevoPrestamoG46 entidad = nuevoPrestamoG46DaoService.selectById(id, NombreEntidadesReporte.NUEVO_PRESTAMO_G46);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("NuevoPrestamoG46 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener NuevoPrestamoG46: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(NuevoPrestamoG46 registro) {
        System.out.println("LLEGA AL SERVICIO PUT NuevoPrestamoG46");
        try {
            return Response.status(Response.Status.OK).entity(nuevoPrestamoG46Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar NuevoPrestamoG46: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(NuevoPrestamoG46 registro) {
        System.out.println("LLEGA AL SERVICIO POST NuevoPrestamoG46");
        try {
            return Response.status(Response.Status.CREATED).entity(nuevoPrestamoG46Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear NuevoPrestamoG46: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE NuevoPrestamoG46 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            nuevoPrestamoG46Service.remove(ids);
            return Response.status(Response.Status.OK).entity("NuevoPrestamoG46 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar NuevoPrestamoG46: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria NuevoPrestamoG46");
        try {
            return Response.status(Response.Status.OK).entity(nuevoPrestamoG46Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria NuevoPrestamoG46: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
