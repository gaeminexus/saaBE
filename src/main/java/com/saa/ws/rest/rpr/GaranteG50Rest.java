package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.GaranteG50DaoService;
import com.saa.ejb.rpr.service.GaranteG50Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.GaranteG50;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cg50")
public class GaranteG50Rest {

    @EJB private GaranteG50DaoService garanteG50DaoService;
    @EJB private GaranteG50Service garanteG50Service;
    @Context private UriInfo context;

    public GaranteG50Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<GaranteG50> lista = garanteG50DaoService.selectAll(NombreEntidadesReporte.GARANTE_G50);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener GaranteG50: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            GaranteG50 entidad = garanteG50DaoService.selectById(id, NombreEntidadesReporte.GARANTE_G50);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("GaranteG50 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener GaranteG50: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(GaranteG50 registro) {
        System.out.println("LLEGA AL SERVICIO PUT GaranteG50");
        try {
            return Response.status(Response.Status.OK).entity(garanteG50Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar GaranteG50: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(GaranteG50 registro) {
        System.out.println("LLEGA AL SERVICIO POST GaranteG50");
        try {
            return Response.status(Response.Status.CREATED).entity(garanteG50Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear GaranteG50: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE GaranteG50 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            garanteG50Service.remove(ids);
            return Response.status(Response.Status.OK).entity("GaranteG50 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar GaranteG50: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria GaranteG50");
        try {
            return Response.status(Response.Status.OK).entity(garanteG50Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria GaranteG50: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
