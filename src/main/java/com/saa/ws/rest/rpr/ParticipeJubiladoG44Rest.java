package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.ParticipeJubiladoG44DaoService;
import com.saa.ejb.rpr.service.ParticipeJubiladoG44Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.ParticipeJubiladoG44;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cg44")
public class ParticipeJubiladoG44Rest {

    @EJB private ParticipeJubiladoG44DaoService participeJubiladoG44DaoService;
    @EJB private ParticipeJubiladoG44Service participeJubiladoG44Service;
    @Context private UriInfo context;

    public ParticipeJubiladoG44Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ParticipeJubiladoG44> lista = participeJubiladoG44DaoService.selectAll(NombreEntidadesReporte.PARTICIPE_JUBILADO_G44);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener ParticipeJubiladoG44: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ParticipeJubiladoG44 entidad = participeJubiladoG44DaoService.selectById(id, NombreEntidadesReporte.PARTICIPE_JUBILADO_G44);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("ParticipeJubiladoG44 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener ParticipeJubiladoG44: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(ParticipeJubiladoG44 registro) {
        System.out.println("LLEGA AL SERVICIO PUT ParticipeJubiladoG44");
        try {
            return Response.status(Response.Status.OK).entity(participeJubiladoG44Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar ParticipeJubiladoG44: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(ParticipeJubiladoG44 registro) {
        System.out.println("LLEGA AL SERVICIO POST ParticipeJubiladoG44");
        try {
            return Response.status(Response.Status.CREATED).entity(participeJubiladoG44Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear ParticipeJubiladoG44: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE ParticipeJubiladoG44 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            participeJubiladoG44Service.remove(ids);
            return Response.status(Response.Status.OK).entity("ParticipeJubiladoG44 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar ParticipeJubiladoG44: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria ParticipeJubiladoG44");
        try {
            return Response.status(Response.Status.OK).entity(participeJubiladoG44Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria ParticipeJubiladoG44: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
