package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.CreditoParticipesMensualDaoService;
import com.saa.ejb.rpr.service.CreditoParticipesMensualService;
import com.saa.model.rpr.CreditoParticipesMensual;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cprm")
public class CreditoParticipesMensualRest {

    @EJB private CreditoParticipesMensualDaoService creditoParticipesMensualDaoService;
    @EJB private CreditoParticipesMensualService creditoParticipesMensualService;
    @Context private UriInfo context;

    public CreditoParticipesMensualRest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CreditoParticipesMensual> lista = creditoParticipesMensualDaoService.selectAll(NombreEntidadesReporte.CREDITO_PARTICIPES_MENSUAL);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener CreditoParticipesMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CreditoParticipesMensual entidad = creditoParticipesMensualDaoService.selectById(id, NombreEntidadesReporte.CREDITO_PARTICIPES_MENSUAL);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("CreditoParticipesMensual con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener CreditoParticipesMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(CreditoParticipesMensual registro) {
        System.out.println("LLEGA AL SERVICIO PUT CreditoParticipesMensual");
        try {
            return Response.status(Response.Status.OK).entity(creditoParticipesMensualService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar CreditoParticipesMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(CreditoParticipesMensual registro) {
        System.out.println("LLEGA AL SERVICIO POST CreditoParticipesMensual");
        try {
            return Response.status(Response.Status.CREATED).entity(creditoParticipesMensualService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear CreditoParticipesMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CreditoParticipesMensual con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            creditoParticipesMensualService.remove(ids);
            return Response.status(Response.Status.OK).entity("CreditoParticipesMensual eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar CreditoParticipesMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria CreditoParticipesMensual");
        try {
            return Response.status(Response.Status.OK).entity(creditoParticipesMensualService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria CreditoParticipesMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
