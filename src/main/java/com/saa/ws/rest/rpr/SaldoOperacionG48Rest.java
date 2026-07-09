package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.SaldoOperacionG48DaoService;
import com.saa.ejb.rpr.service.SaldoOperacionG48Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.SaldoOperacionG48;

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

@Path("cg48")
public class SaldoOperacionG48Rest {

    @EJB private SaldoOperacionG48DaoService saldoOperacionG48DaoService;
    @EJB private SaldoOperacionG48Service saldoOperacionG48Service;
    @Context private UriInfo context;

    public SaldoOperacionG48Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<SaldoOperacionG48> lista = saldoOperacionG48DaoService.selectAll(NombreEntidadesReporte.SALDO_OPERACION_G48);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener SaldoOperacionG48: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            SaldoOperacionG48 entidad = saldoOperacionG48DaoService.selectById(id, NombreEntidadesReporte.SALDO_OPERACION_G48);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("SaldoOperacionG48 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener SaldoOperacionG48: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(SaldoOperacionG48 registro) {
        System.out.println("LLEGA AL SERVICIO PUT SaldoOperacionG48");
        try {
            return Response.status(Response.Status.OK).entity(saldoOperacionG48Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar SaldoOperacionG48: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(SaldoOperacionG48 registro) {
        System.out.println("LLEGA AL SERVICIO POST SaldoOperacionG48");
        try {
            return Response.status(Response.Status.CREATED).entity(saldoOperacionG48Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear SaldoOperacionG48: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE SaldoOperacionG48 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            saldoOperacionG48Service.remove(ids);
            return Response.status(Response.Status.OK).entity("SaldoOperacionG48 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar SaldoOperacionG48: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria SaldoOperacionG48");
        try {
            return Response.status(Response.Status.OK).entity(saldoOperacionG48Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria SaldoOperacionG48: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
