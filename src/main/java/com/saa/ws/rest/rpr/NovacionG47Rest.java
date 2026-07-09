package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.NovacionG47DaoService;
import com.saa.ejb.rpr.service.NovacionG47Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.NovacionG47;

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

@Path("cg47")
public class NovacionG47Rest {

    @EJB private NovacionG47DaoService novacionG47DaoService;
    @EJB private NovacionG47Service novacionG47Service;
    @Context private UriInfo context;

    public NovacionG47Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NovacionG47> lista = novacionG47DaoService.selectAll(NombreEntidadesReporte.NOVACION_G47);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener NovacionG47: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NovacionG47 entidad = novacionG47DaoService.selectById(id, NombreEntidadesReporte.NOVACION_G47);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("NovacionG47 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener NovacionG47: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(NovacionG47 registro) {
        System.out.println("LLEGA AL SERVICIO PUT NovacionG47");
        try {
            return Response.status(Response.Status.OK).entity(novacionG47Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar NovacionG47: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(NovacionG47 registro) {
        System.out.println("LLEGA AL SERVICIO POST NovacionG47");
        try {
            return Response.status(Response.Status.CREATED).entity(novacionG47Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear NovacionG47: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE NovacionG47 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            novacionG47Service.remove(ids);
            return Response.status(Response.Status.OK).entity("NovacionG47 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar NovacionG47: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria NovacionG47");
        try {
            return Response.status(Response.Status.OK).entity(novacionG47Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria NovacionG47: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
