package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.ejb.cnt.dao.DesgloseMayorizacionCCDaoService;
import com.saa.ejb.cnt.service.DesgloseMayorizacionCCService;
import com.saa.model.cnt.DesgloseMayorizacionCC;
import com.saa.model.cnt.NombreEntidadesContabilidad;

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

@Path("dtmc")
public class DesgloseMayorizacionCCRest {

    @EJB
    private DesgloseMayorizacionCCDaoService desgloseMayorizacionCCDaoService;

    @EJB
    private DesgloseMayorizacionCCService desgloseMayorizacionCCService;

    @Context
    private UriInfo context;

    public DesgloseMayorizacionCCRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DesgloseMayorizacionCC> lista = desgloseMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener desgloses de mayorización CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DesgloseMayorizacionCC desglose = desgloseMayorizacionCCDaoService.selectById(id, NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC);
            if (desglose == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Desglose de mayorización CC con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(desglose).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener desglose de mayorización CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("criteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(Long test) {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        try {
            List<DesgloseMayorizacionCC> lista = desgloseMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en criteria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
