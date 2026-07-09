package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.EjecucionReporteCarteraDaoService;
import com.saa.ejb.rpr.service.EjecucionReporteCarteraService;
import com.saa.ejb.rpr.service.GeneracionReportesCarteraService;
import com.saa.model.rpr.EjecucionReporteCartera;
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

@Path("ejcc")
public class EjecucionReporteCarteraRest {

    @EJB private EjecucionReporteCarteraDaoService ejecucionReporteCarteraDaoService;
    @EJB private EjecucionReporteCarteraService ejecucionReporteCarteraService;
    @EJB private GeneracionReportesCarteraService generacionReportesCarteraService;
    @Context private UriInfo context;

    public EjecucionReporteCarteraRest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<EjecucionReporteCartera> lista = ejecucionReporteCarteraDaoService.selectAll(NombreEntidadesReporte.EJECUCION_REPORTE_CARTERA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener EjecucionReporteCartera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            EjecucionReporteCartera entidad = ejecucionReporteCarteraDaoService.selectById(id, NombreEntidadesReporte.EJECUCION_REPORTE_CARTERA);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("EjecucionReporteCartera con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener EjecucionReporteCartera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getByMesAnio/{mes}/{anio}") @Produces(MediaType.APPLICATION_JSON)
    public Response getByMesAnio(@PathParam("mes") Long mes, @PathParam("anio") Long anio) {
        try {
            List<EjecucionReporteCartera> lista = ejecucionReporteCarteraService.selectByMesAnio(mes, anio);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en getByMesAnio EjecucionReporteCartera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(EjecucionReporteCartera registro) {
        System.out.println("LLEGA AL SERVICIO PUT EjecucionReporteCartera");
        try {
            return Response.status(Response.Status.OK).entity(ejecucionReporteCarteraService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar EjecucionReporteCartera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(EjecucionReporteCartera registro) {
        System.out.println("LLEGA AL SERVICIO POST EjecucionReporteCartera");
        try {
            return Response.status(Response.Status.CREATED).entity(ejecucionReporteCarteraService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear EjecucionReporteCartera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE EjecucionReporteCartera con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            ejecucionReporteCarteraService.remove(ids);
            return Response.status(Response.Status.OK).entity("EjecucionReporteCartera eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar EjecucionReporteCartera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria EjecucionReporteCartera");
        try {
            return Response.status(Response.Status.OK).entity(ejecucionReporteCarteraService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria EjecucionReporteCartera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Endpoint para ejecutar la generación de reportes de cartera CPRM, CJBM y CCPM.
     * Body esperado: { "mes": 6, "anio": 2026, "usuario": "admin" }
     * URL: POST /ejcc/ejecutar
     */
    @POST @Path("/ejecutar") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response ejecutar(SolicitudGeneracionCartera solicitud) {
        System.out.println("LLEGA AL SERVICIO POST ejcc/ejecutar - mes: "
                + solicitud.getMes() + ", anio: " + solicitud.getAnio()
                + ", usuario: " + solicitud.getUsuario());
        try {
            EjecucionReporteCartera resultado = generacionReportesCarteraService.ejecutarGeneracion(
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
                    .entity("Error al ejecutar reportes de cartera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Endpoint para regenerar: borra los datos existentes de una ejecución y vuelve a generar.
     * URL: POST /ejcc/regenerar
     * Body esperado: { "codigoEjecucion": 3, "usuario": "admin" }
     */
    @POST @Path("/regenerar") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response regenerar(SolicitudRegeneracionCartera solicitud) {
        System.out.println("LLEGA AL SERVICIO POST ejcc/regenerar - codigoEjecucion: "
                + solicitud.getCodigoEjecucion() + ", usuario: " + solicitud.getUsuario());
        try {
            EjecucionReporteCartera resultado = generacionReportesCarteraService.regenerarGeneracion(
                    solicitud.getCodigoEjecucion(),
                    solicitud.getUsuario()
            );
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al regenerar reportes de cartera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Clase interna para la solicitud de regeneración de reportes de cartera.
     */
    public static class SolicitudRegeneracionCartera {
        private Long codigoEjecucion;
        private String usuario;

        public Long getCodigoEjecucion() { return codigoEjecucion; }
        public void setCodigoEjecucion(Long codigoEjecucion) { this.codigoEjecucion = codigoEjecucion; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
    }

    /**
     * Clase interna para la solicitud de generación de reportes de cartera.
     */
    public static class SolicitudGeneracionCartera {
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
