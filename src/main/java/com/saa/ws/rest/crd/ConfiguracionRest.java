package com.saa.ws.rest.crd;

import java.util.LinkedHashMap;
import java.util.Map;

import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.ConfiguracionGeneracionAportesService;
import com.saa.ejb.crd.service.dto.EstadoContabilidadCrd;
import com.saa.ejb.crd.service.dto.EstadoGeneracionPorFaltante;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Configuracion general de CRD. Por ahora, el flag global de contabilidad (§4.3 del plan
 * de devengo de aportes).
 */
@Path("cnfg")
public class ConfiguracionRest {

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private ConfiguracionGeneracionAportesService configuracionGeneracionAportesService;

    /**
     * Cuerpo del PUT de {@link #actualizarContabilidadCrd(SolicitudCambioContabilidad)}.
     */
    public static class SolicitudCambioContabilidad {
        private boolean activa;
        private String usuario;
        private String motivo;

        public boolean isActiva() {
            return activa;
        }

        public void setActiva(boolean activa) {
            this.activa = activa;
        }

        public String getUsuario() {
            return usuario;
        }

        public void setUsuario(String usuario) {
            this.usuario = usuario;
        }

        public String getMotivo() {
            return motivo;
        }

        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }
    }

    @GET
    @Path("/contabilidadCrd")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerContabilidadCrd() {
        System.out.println("LLEGA AL SERVICIO GET - CONFIGURACION CONTABILIDAD CRD");
        try {
            EstadoContabilidadCrd estado = configuracionContabilidadService.obtenerEstado();
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("activa", estado.isActiva());
            cuerpo.put("usuarioUltimoCambio", estado.getUsuarioUltimoCambio());
            cuerpo.put("fechaUltimoCambio", estado.getFechaUltimoCambio());
            cuerpo.put("motivoUltimoCambio", estado.getMotivoUltimoCambio());
            return Response.status(Response.Status.OK).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener la configuracion de contabilidad de CRD: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Path("/contabilidadCrd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarContabilidadCrd(SolicitudCambioContabilidad solicitud) {
        System.out.println("LLEGA AL SERVICIO PUT - CONFIGURACION CONTABILIDAD CRD - activa: "
            + (solicitud != null ? solicitud.isActiva() : null)
            + " - usuario: " + (solicitud != null ? solicitud.getUsuario() : null)
            + " - motivo: " + (solicitud != null ? solicitud.getMotivo() : null));
        try {
            if (solicitud == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Debe enviar el cuerpo de la solicitud").type(MediaType.APPLICATION_JSON).build();
            }
            boolean activa = configuracionContabilidadService.actualizar(
                solicitud.isActiva(), solicitud.getUsuario(), solicitud.getMotivo());
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("activa", activa);
            return Response.status(Response.Status.OK).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar la configuracion de contabilidad de CRD: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cuerpo del PUT de {@link #actualizarGeneracionPorFaltante(SolicitudCambioGeneracionPorFaltante)}.
     * Misma forma que {@link SolicitudCambioContabilidad}; se declara aparte para que el
     * esquema JSON de cada endpoint quede identificado con su propio flag.
     */
    public static class SolicitudCambioGeneracionPorFaltante {
        private boolean activa;
        private String usuario;
        private String motivo;

        public boolean isActiva() {
            return activa;
        }

        public void setActiva(boolean activa) {
            this.activa = activa;
        }

        public String getUsuario() {
            return usuario;
        }

        public void setUsuario(String usuario) {
            this.usuario = usuario;
        }

        public String getMotivo() {
            return motivo;
        }

        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }
    }

    /**
     * Fase 4 (plan de devengo de aportes): bandera del camino nuevo de generación de
     * aportes por faltante en {@code GeneracionArchivoPetroServiceImpl.recopilarAportes}.
     * Se entrega APAGADA.
     */
    @GET
    @Path("/generacionPorFaltanteAh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerGeneracionPorFaltante() {
        System.out.println("LLEGA AL SERVICIO GET - CONFIGURACION GENERACION POR FALTANTE (AH)");
        try {
            EstadoGeneracionPorFaltante estado = configuracionGeneracionAportesService.obtenerEstado();
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("activa", estado.isActiva());
            cuerpo.put("usuarioUltimoCambio", estado.getUsuarioUltimoCambio());
            cuerpo.put("fechaUltimoCambio", estado.getFechaUltimoCambio());
            cuerpo.put("motivoUltimoCambio", estado.getMotivoUltimoCambio());
            return Response.status(Response.Status.OK).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener la configuracion de generacion por faltante: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Path("/generacionPorFaltanteAh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarGeneracionPorFaltante(SolicitudCambioGeneracionPorFaltante solicitud) {
        System.out.println("LLEGA AL SERVICIO PUT - CONFIGURACION GENERACION POR FALTANTE (AH) - activa: "
            + (solicitud != null ? solicitud.isActiva() : null)
            + " - usuario: " + (solicitud != null ? solicitud.getUsuario() : null)
            + " - motivo: " + (solicitud != null ? solicitud.getMotivo() : null));
        try {
            if (solicitud == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Debe enviar el cuerpo de la solicitud").type(MediaType.APPLICATION_JSON).build();
            }
            boolean activa = configuracionGeneracionAportesService.actualizar(
                solicitud.isActiva(), solicitud.getUsuario(), solicitud.getMotivo());
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("activa", activa);
            return Response.status(Response.Status.OK).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar la configuracion de generacion por faltante: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
