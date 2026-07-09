package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.DetalleEjecucionReporteDaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.model.rpr.DetalleEjecucionReporte;
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

@Path("ejrd")
public class DetalleEjecucionReporteRest {

    @EJB private DetalleEjecucionReporteDaoService detalleEjecucionReporteDaoService;
    @EJB private DetalleEjecucionReporteService detalleEjecucionReporteService;
    @Context private UriInfo context;

    public DetalleEjecucionReporteRest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetalleEjecucionReporte> lista = detalleEjecucionReporteDaoService.selectAll(NombreEntidadesReporte.DETALLE_EJECUCION_REPORTE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener DetalleEjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetalleEjecucionReporte entidad = detalleEjecucionReporteDaoService.selectById(id, NombreEntidadesReporte.DETALLE_EJECUCION_REPORTE);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("DetalleEjecucionReporte con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener DetalleEjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getByEjecucion/{idEjecucion}") @Produces(MediaType.APPLICATION_JSON)
    public Response getByEjecucion(@PathParam("idEjecucion") Long idEjecucion) {
        try {
            List<DetalleEjecucionReporte> lista = detalleEjecucionReporteService.selectByEjecucion(idEjecucion);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en getByEjecucion DetalleEjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getConNovedades/{idEjecucion}") @Produces(MediaType.APPLICATION_JSON)
    public Response getConNovedades(@PathParam("idEjecucion") Long idEjecucion) {
        try {
            List<DetalleEjecucionReporte> lista = detalleEjecucionReporteService.selectConNovedadesByEjecucion(idEjecucion);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en getConNovedades DetalleEjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleEjecucionReporte registro) {
        System.out.println("LLEGA AL SERVICIO PUT DetalleEjecucionReporte");
        try {
            return Response.status(Response.Status.OK).entity(detalleEjecucionReporteService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar DetalleEjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleEjecucionReporte registro) {
        System.out.println("LLEGA AL SERVICIO POST DetalleEjecucionReporte");
        try {
            return Response.status(Response.Status.CREATED).entity(detalleEjecucionReporteService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear DetalleEjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DetalleEjecucionReporte con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            detalleEjecucionReporteService.remove(ids);
            return Response.status(Response.Status.OK).entity("DetalleEjecucionReporte eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar DetalleEjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria DetalleEjecucionReporte");
        try {
            return Response.status(Response.Status.OK).entity(detalleEjecucionReporteService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria DetalleEjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
