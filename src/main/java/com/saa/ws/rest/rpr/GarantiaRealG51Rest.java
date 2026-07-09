package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.GarantiaRealG51DaoService;
import com.saa.ejb.rpr.service.GarantiaRealG51Service;
import com.saa.model.rpr.GarantiaRealG51;
import com.saa.model.rpr.NombreEntidadesReporte;

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

@Path("cg51")
public class GarantiaRealG51Rest {

    @EJB private GarantiaRealG51DaoService garantiaRealG51DaoService;
    @EJB private GarantiaRealG51Service garantiaRealG51Service;
    @Context private UriInfo context;

    public GarantiaRealG51Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<GarantiaRealG51> lista = garantiaRealG51DaoService.selectAll(NombreEntidadesReporte.GARANTIA_REAL_G51);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener GarantiaRealG51: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            GarantiaRealG51 entidad = garantiaRealG51DaoService.selectById(id, NombreEntidadesReporte.GARANTIA_REAL_G51);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("GarantiaRealG51 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener GarantiaRealG51: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(GarantiaRealG51 registro) {
        System.out.println("LLEGA AL SERVICIO PUT GarantiaRealG51");
        try {
            return Response.status(Response.Status.OK).entity(garantiaRealG51Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar GarantiaRealG51: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(GarantiaRealG51 registro) {
        System.out.println("LLEGA AL SERVICIO POST GarantiaRealG51");
        try {
            return Response.status(Response.Status.CREATED).entity(garantiaRealG51Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear GarantiaRealG51: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE GarantiaRealG51 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            garantiaRealG51Service.remove(ids);
            return Response.status(Response.Status.OK).entity("GarantiaRealG51 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar GarantiaRealG51: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria GarantiaRealG51");
        try {
            return Response.status(Response.Status.OK).entity(garantiaRealG51Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria GarantiaRealG51: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
