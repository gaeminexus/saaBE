package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.HistDetalleMayorizacionDaoService;
import com.saa.ejb.cnt.service.HistDetalleMayorizacionService;
import com.saa.model.cnt.HistDetalleMayorizacion;
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

@Path("dtmh")
public class HistDetalleMayorizacionRest {

    @EJB
    private HistDetalleMayorizacionDaoService histDetalleMayorizacionDaoService;

    @EJB
    private HistDetalleMayorizacionService histDetalleMayorizacionService;

    @Context
    private UriInfo context;

    public HistDetalleMayorizacionRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistDetalleMayorizacion> lista = histDetalleMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener historial de detalles de mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            HistDetalleMayorizacion hist = histDetalleMayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION);
            if (hist == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Historial de detalle de mayorización con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(hist).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener historial de detalle de mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(HistDetalleMayorizacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - HIST_DETALLE_MAYORIZACION");
        try {
            HistDetalleMayorizacion resultado = histDetalleMayorizacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar historial de detalle de mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(HistDetalleMayorizacion registro) {
        System.out.println("LLEGA AL SERVICIO POST - HIST_DETALLE_MAYORIZACION");
        try {
            HistDetalleMayorizacion resultado = histDetalleMayorizacionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear historial de detalle de mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de HIST_DETALLE_MAYORIZACION");
        try {
            return Response.status(Response.Status.OK)
                    .entity(histDetalleMayorizacionService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - HIST_DETALLE_MAYORIZACION");
        try {
            HistDetalleMayorizacion elimina = new HistDetalleMayorizacion();
            histDetalleMayorizacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar historial de detalle de mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
