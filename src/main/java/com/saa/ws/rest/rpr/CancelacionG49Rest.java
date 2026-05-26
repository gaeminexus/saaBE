package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.CancelacionG49DaoService;
import com.saa.ejb.rpr.service.CancelacionG49Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.CancelacionG49;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cg49")
public class CancelacionG49Rest {

    @EJB private CancelacionG49DaoService cancelacionG49DaoService;
    @EJB private CancelacionG49Service cancelacionG49Service;
    @Context private UriInfo context;

    public CancelacionG49Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CancelacionG49> lista = cancelacionG49DaoService.selectAll(NombreEntidadesReporte.CANCELACION_G49);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener CancelacionG49: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CancelacionG49 entidad = cancelacionG49DaoService.selectById(id, NombreEntidadesReporte.CANCELACION_G49);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("CancelacionG49 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener CancelacionG49: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(CancelacionG49 registro) {
        System.out.println("LLEGA AL SERVICIO PUT CancelacionG49");
        try {
            return Response.status(Response.Status.OK).entity(cancelacionG49Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar CancelacionG49: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(CancelacionG49 registro) {
        System.out.println("LLEGA AL SERVICIO POST CancelacionG49");
        try {
            return Response.status(Response.Status.CREATED).entity(cancelacionG49Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear CancelacionG49: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CancelacionG49 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            cancelacionG49Service.remove(ids);
            return Response.status(Response.Status.OK).entity("CancelacionG49 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar CancelacionG49: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria CancelacionG49");
        try {
            return Response.status(Response.Status.OK).entity(cancelacionG49Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria CancelacionG49: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
