package com.saa.ws.rest.crd;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.RecepcionValorSeguroDaoService;
import com.saa.ejb.crd.service.RecepcionValorSeguroService;
import com.saa.ejb.crd.service.dto.SolicitudDecisionRecepcionSeguro;
import com.saa.ejb.crd.service.dto.SolicitudRegistroRecepcionSeguro;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.RecepcionValorSeguro;
import com.saa.rubros.CrdEstadoRecepcionSeguro;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST de la recepción de valores de seguro (sepelio), CRD.RVSG. Ver
 * {@code docs/logica-negocio/crd/API-RECEPCION-VALORES-SEGURO.md}.
 *
 * Las respuestas del flujo (registrar, aprobar, rechazar, anular) llevan
 * {@code {exito, etapa, mensaje, resultado}}; los fallos de negocio salen como 400 (dato faltante
 * o inválido), 404 (no existe) o 409 (estado o configuración que no lo permiten).
 *
 * No se expone POST/PUT/DELETE genérico: la recepción es una máquina de estados que mueve
 * dinero, y una escritura directa se saltaría el asiento y el aporte.
 */
@Path("rvsg")
public class RecepcionValorSeguroRest {

    private static final String ETAPA_VALIDACION = "VALIDACION";
    private static final String ETAPA_APLICACION = "APLICACION";

    @EJB
    private RecepcionValorSeguroDaoService recepcionValorSeguroDaoService;

    @EJB
    private RecepcionValorSeguroService recepcionValorSeguroService;

    public RecepcionValorSeguroRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - RVSG");
        try {
            List<RecepcionValorSeguro> lista = recepcionValorSeguroDaoService
                    .selectAll(NombreEntidadesCredito.RECEPCION_VALOR_SEGURO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las recepciones de valores de seguro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - RVSG - id: " + id);
        try {
            RecepcionValorSeguro recepcion = recepcionValorSeguroDaoService
                    .selectById(id, NombreEntidadesCredito.RECEPCION_VALOR_SEGURO);
            return Response.status(Response.Status.OK).entity(recepcion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la recepción " + id + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** GET /rest/rvsg/porEntidad/{idEntidad} — todas las recepciones de un partícipe. */
    @GET
    @Path("/porEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO GET porEntidad - RVSG - idEntidad: " + idEntidad);
        try {
            List<RecepcionValorSeguro> lista = recepcionValorSeguroDaoService.selectByEntidad(idEntidad);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las recepciones de la entidad " + idEntidad + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** GET /rest/rvsg/pendientes — las de estado REGISTRADO, para la bandeja de contabilidad. */
    @GET
    @Path("/pendientes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pendientes() {
        System.out.println("LLEGA AL SERVICIO GET pendientes - RVSG");
        try {
            List<RecepcionValorSeguro> lista = recepcionValorSeguroDaoService
                    .selectByEstado(Long.valueOf(CrdEstadoRecepcionSeguro.REGISTRADO));
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las recepciones pendientes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        System.out.println("LLEGA AL SERVICIO selectByCriteria - RVSG");
        try {
            List<RecepcionValorSeguro> lista = recepcionValorSeguroService.selectByCriteria(datos);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en la busqueda de recepciones de valores de seguro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** POST /rest/rvsg/registrar — queda REGISTRADO; no genera asiento ni toca el saldo. */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(SolicitudRegistroRecepcionSeguro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST registrar - RVSG - idEntidad: "
                + (solicitud != null ? solicitud.getIdEntidad() : null));
        try {
            RecepcionValorSeguro recepcion = recepcionValorSeguroService.registrar(solicitud);
            return respuestaExito(Response.Status.CREATED,
                    "Recepción registrada. El valor entra a la cuenta del partícipe cuando contabilidad la apruebe.",
                    recepcion);
        } catch (Throwable e) {
            return respuestaError(e, "registrar la recepción");
        }
    }

    /** POST /rest/rvsg/{id}/aprobar — genera el asiento, registra el aporte y pasa a APROBADO. */
    @POST
    @Path("/{id}/aprobar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aprobar(@PathParam("id") Long id, SolicitudDecisionRecepcionSeguro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST aprobar - RVSG - id: " + id);
        try {
            RecepcionValorSeguro recepcion = recepcionValorSeguroService.aprobar(id,
                    solicitud != null ? solicitud.getUsuario() : null);
            return respuestaExito(Response.Status.OK,
                    "Recepción aprobada: asiento generado y valor registrado en la cuenta del partícipe.",
                    recepcion);
        } catch (Throwable e) {
            return respuestaError(e, "aprobar la recepción " + id);
        }
    }

    /** POST /rest/rvsg/{id}/rechazar — sólo desde REGISTRADO. Motivo obligatorio. */
    @POST
    @Path("/{id}/rechazar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rechazar(@PathParam("id") Long id, SolicitudDecisionRecepcionSeguro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST rechazar - RVSG - id: " + id);
        try {
            RecepcionValorSeguro recepcion = recepcionValorSeguroService.rechazar(id,
                    solicitud != null ? solicitud.getUsuario() : null,
                    solicitud != null ? solicitud.getMotivo() : null);
            return respuestaExito(Response.Status.OK, "Recepción rechazada.", recepcion);
        } catch (Throwable e) {
            return respuestaError(e, "rechazar la recepción " + id);
        }
    }

    /** POST /rest/rvsg/{id}/anular — sólo desde APROBADO; reversa asiento y aporte. Motivo obligatorio. */
    @POST
    @Path("/{id}/anular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, SolicitudDecisionRecepcionSeguro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST anular - RVSG - id: " + id);
        try {
            RecepcionValorSeguro recepcion = recepcionValorSeguroService.anular(id,
                    solicitud != null ? solicitud.getUsuario() : null,
                    solicitud != null ? solicitud.getMotivo() : null);
            return respuestaExito(Response.Status.OK, "Recepción anulada: asiento y aporte reversados.", recepcion);
        } catch (Throwable e) {
            return respuestaError(e, "anular la recepción " + id);
        }
    }

    private Response respuestaExito(Response.Status status, String mensaje, Object resultado) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.TRUE);
        cuerpo.put("etapa", ETAPA_APLICACION);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("resultado", resultado);
        return Response.status(status).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }

    /** 400 / 404 según el prefijo del mensaje; cualquier otro fallo de negocio es 409; el resto, 500. */
    private Response respuestaError(Throwable e, String accion) {
        System.err.println("ERROR al " + accion + ": " + e.getMessage());
        String mensaje = e.getMessage();
        int status;
        String etapa = ETAPA_VALIDACION;
        if (e instanceof IncomeException) {
            if (mensaje != null && mensaje.startsWith(RecepcionValorSeguroService.ERR_VALIDACION)) {
                status = Response.Status.BAD_REQUEST.getStatusCode();
            } else if (mensaje != null && mensaje.startsWith(RecepcionValorSeguroService.ERR_NO_ENCONTRADA)) {
                status = Response.Status.NOT_FOUND.getStatusCode();
            } else {
                status = Response.Status.CONFLICT.getStatusCode();
            }
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            etapa = ETAPA_APLICACION;
            mensaje = "Error al " + accion + ": " + mensaje;
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("etapa", etapa);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("error", mensaje);
        return Response.status(status).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }
}
