package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.SaldoCuentaG42DaoService;
import com.saa.ejb.rpr.service.SaldoCuentaG42Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.SaldoCuentaG42;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cg42")
public class SaldoCuentaG42Rest {

    @EJB private SaldoCuentaG42DaoService saldoCuentaG42DaoService;
    @EJB private SaldoCuentaG42Service saldoCuentaG42Service;
    @Context private UriInfo context;

    public SaldoCuentaG42Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<SaldoCuentaG42> lista = saldoCuentaG42DaoService.selectAll(NombreEntidadesReporte.SALDO_CUENTA_G42);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener SaldoCuentaG42: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            SaldoCuentaG42 entidad = saldoCuentaG42DaoService.selectById(id, NombreEntidadesReporte.SALDO_CUENTA_G42);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("SaldoCuentaG42 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener SaldoCuentaG42: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(SaldoCuentaG42 registro) {
        System.out.println("LLEGA AL SERVICIO PUT SaldoCuentaG42");
        try {
            return Response.status(Response.Status.OK).entity(saldoCuentaG42Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar SaldoCuentaG42: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(SaldoCuentaG42 registro) {
        System.out.println("LLEGA AL SERVICIO POST SaldoCuentaG42");
        try {
            return Response.status(Response.Status.CREATED).entity(saldoCuentaG42Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear SaldoCuentaG42: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE SaldoCuentaG42 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            saldoCuentaG42Service.remove(ids);
            return Response.status(Response.Status.OK).entity("SaldoCuentaG42 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar SaldoCuentaG42: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria SaldoCuentaG42");
        try {
            return Response.status(Response.Status.OK).entity(saldoCuentaG42Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria SaldoCuentaG42: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
