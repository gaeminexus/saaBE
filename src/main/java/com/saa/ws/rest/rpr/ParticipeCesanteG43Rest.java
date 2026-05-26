package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.ParticipeCesanteG43DaoService;
import com.saa.ejb.rpr.service.ParticipeCesanteG43Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.ParticipeCesanteG43;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cg43")
public class ParticipeCesanteG43Rest {

    @EJB private ParticipeCesanteG43DaoService participeCesanteG43DaoService;
    @EJB private ParticipeCesanteG43Service participeCesanteG43Service;
    @Context private UriInfo context;

    public ParticipeCesanteG43Rest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ParticipeCesanteG43> lista = participeCesanteG43DaoService.selectAll(NombreEntidadesReporte.PARTICIPE_CESANTE_G43);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener ParticipeCesanteG43: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ParticipeCesanteG43 entidad = participeCesanteG43DaoService.selectById(id, NombreEntidadesReporte.PARTICIPE_CESANTE_G43);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("ParticipeCesanteG43 con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener ParticipeCesanteG43: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(ParticipeCesanteG43 registro) {
        System.out.println("LLEGA AL SERVICIO PUT ParticipeCesanteG43");
        try {
            return Response.status(Response.Status.OK).entity(participeCesanteG43Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar ParticipeCesanteG43: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(ParticipeCesanteG43 registro) {
        System.out.println("LLEGA AL SERVICIO POST ParticipeCesanteG43");
        try {
            return Response.status(Response.Status.CREATED).entity(participeCesanteG43Service.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear ParticipeCesanteG43: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE ParticipeCesanteG43 con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            participeCesanteG43Service.remove(ids);
            return Response.status(Response.Status.OK).entity("ParticipeCesanteG43 eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar ParticipeCesanteG43: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria ParticipeCesanteG43");
        try {
            return Response.status(Response.Status.OK).entity(participeCesanteG43Service.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria ParticipeCesanteG43: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
