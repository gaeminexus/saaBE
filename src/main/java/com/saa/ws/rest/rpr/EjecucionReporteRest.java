package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.EjecucionReporteDaoService;
import com.saa.ejb.rpr.service.EjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionReportesService;
import com.saa.model.rpr.EjecucionReporte;
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

@Path("ejrc")
public class EjecucionReporteRest {

    @EJB private EjecucionReporteDaoService ejecucionReporteDaoService;
    @EJB private EjecucionReporteService ejecucionReporteService;
    @EJB private GeneracionReportesService generacionReportesService;
    @Context private UriInfo context;

    public EjecucionReporteRest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<EjecucionReporte> lista = ejecucionReporteDaoService.selectAll(NombreEntidadesReporte.EJECUCION_REPORTE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener EjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            EjecucionReporte entidad = ejecucionReporteDaoService.selectById(id, NombreEntidadesReporte.EJECUCION_REPORTE);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("EjecucionReporte con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener EjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getByMesAnio/{mes}/{anio}") @Produces(MediaType.APPLICATION_JSON)
    public Response getByMesAnio(@PathParam("mes") Long mes, @PathParam("anio") Long anio) {
        try {
            List<EjecucionReporte> lista = ejecucionReporteService.selectByMesAnio(mes, anio);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en getByMesAnio EjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(EjecucionReporte registro) {
        System.out.println("LLEGA AL SERVICIO PUT EjecucionReporte");
        try {
            return Response.status(Response.Status.OK).entity(ejecucionReporteService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar EjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(EjecucionReporte registro) {
        System.out.println("LLEGA AL SERVICIO POST EjecucionReporte");
        try {
            return Response.status(Response.Status.CREATED).entity(ejecucionReporteService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear EjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE EjecucionReporte con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            ejecucionReporteService.remove(ids);
            return Response.status(Response.Status.OK).entity("EjecucionReporte eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar EjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria EjecucionReporte");
        try {
            return Response.status(Response.Status.OK).entity(ejecucionReporteService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria EjecucionReporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Endpoint único para lanzar la generación de reportes G40-G51.
     * Body esperado: { "mes": 4, "anio": 2026, "usuario": "jperez" }
     * URL: POST /ejrc/ejecutar
     */
    @POST @Path("/ejecutar") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response ejecutar(SolicitudGeneracion solicitud) {
        System.out.println("LLEGA AL SERVICIO POST ejrc/ejecutar - mes: "
                + solicitud.getMes() + ", anio: " + solicitud.getAnio()
                + ", usuario: " + solicitud.getUsuario());
        try {
            EjecucionReporte resultado = generacionReportesService.ejecutarGeneracion(
                    solicitud.getMes(),
                    solicitud.getAnio(),
                    solicitud.getUsuario()
            );
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            if (e.getMessage() != null && e.getMessage().contains("ya fueron generados")) {
                return Response.status(Response.Status.OK)
                        .entity("{\"mensaje\": \"" + e.getMessage() + "\"}")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en generacion de reportes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // DTO para recibir el body del endpoint ejecutar
    public static class SolicitudGeneracion {
        private Long mes;
        private Long anio;
        private String usuario;

        public Long getMes() { return mes; }
        public void setMes(Long mes) { this.mes = mes; }
        public Long getAnio() { return anio; }
        public void setAnio(Long anio) { this.anio = anio; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
    }
}
