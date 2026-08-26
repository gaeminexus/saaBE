package com.saa.ws.rest.crd;

import com.saa.ejb.crd.service.CierreCarteraService;
import com.saa.ejb.crd.service.dto.SolicitudCierreCartera;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Proceso mensual de apertura / cierre de cartera (§3.2 del levantamiento).
 *
 * Contrato para el frontend en docs/logica-negocio/crd/API-CIERRE-CARTERA.md — cualquier
 * cambio de ruta, request o response se registra ahí en el mismo cambio.
 *
 * <b>No hay CRUD genérico.</b> Una corrida de cierre no se crea ni se edita a mano: se
 * previsualiza, se ejecuta y, si hizo falta, se reversa. Los cuatro verbos de negocio son
 * los únicos que se exponen, más el histórico.
 */
@Path("cierrecartera")
public class CierreCarteraRest {

    @EJB
    private CierreCarteraService cierreCarteraService;

    @Context
    private UriInfo context;

    public CierreCarteraRest() {
    }

    /**
     * POST - Calcula la corrida SIN grabar nada. Es lo que contabilidad revisa antes de
     * autorizar: los seis sub-procesos con sus líneas, el snapshot y las desviaciones.
     *
     * @param solicitud { idEmpresa, anio, mes, usuario, ip, observacion }
     * @return 200 con el CierreCartera calculado; 500 si falta parametrización
     */
    @POST
    @Path("/previsualizar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response previsualizar(SolicitudCierreCartera solicitud) {
        System.out.println("LLEGA AL SERVICIO PREVISUALIZAR - CIERRE_CARTERA - empresa: "
                + (solicitud != null ? solicitud.getIdEmpresa() : null) + " periodo: "
                + (solicitud != null ? solicitud.getAnio() + "-" + solicitud.getMes() : null));
        try {
            return Response.status(Response.Status.OK)
                    .entity(cierreCarteraService.previsualizar(solicitud))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al previsualizar el cierre de cartera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Calcula, graba la corrida con su snapshot y genera los asientos. Transaccional:
     * si un sub-asiento falla no queda nada a medias. Ejecutar dos veces el mismo período
     * falla con mensaje claro.
     *
     * @param solicitud { idEmpresa, anio, mes, usuario, ip, observacion }
     * @return 200 con el CierreCartera grabado; 500 si el período ya se cerró o algo no cuadra
     */
    @POST
    @Path("/ejecutar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ejecutar(SolicitudCierreCartera solicitud) {
        System.out.println("LLEGA AL SERVICIO EJECUTAR - CIERRE_CARTERA - empresa: "
                + (solicitud != null ? solicitud.getIdEmpresa() : null) + " periodo: "
                + (solicitud != null ? solicitud.getAnio() + "-" + solicitud.getMes() : null));
        try {
            return Response.status(Response.Status.OK)
                    .entity(cierreCarteraService.ejecutar(solicitud))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al ejecutar el cierre de cartera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Lo que quedó GRABADO de un período: corrida, snapshot y asientos generados.
     * No recalcula nada.
     *
     * @param idEmpresa Código de la empresa (SCP.PJRQ)
     * @param anio      Año del mes cerrado
     * @param mes       Mes cerrado, 1 a 12
     */
    @GET
    @Path("/consultar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consultar(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("anio") Long anio, @QueryParam("mes") Long mes) {
        System.out.println("LLEGA AL SERVICIO CONSULTAR - CIERRE_CARTERA - empresa: " + idEmpresa
                + " periodo: " + anio + "-" + mes);
        try {
            return Response.status(Response.Status.OK)
                    .entity(cierreCarteraService.consultar(idEmpresa, anio, mes))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al consultar el cierre de cartera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Anula los asientos de una corrida ejecutada y la marca REVERSADA. No borra
     * filas. Después del reverso el período vuelve a estar libre.
     *
     * @param idCorrida Código de la corrida (CRD.CRCT)
     * @param usuario   Usuario que reversa
     * @param ip        IP desde la que se reversa
     * @param motivo    Motivo del reverso
     */
    @POST
    @Path("/reversar/{idCorrida}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reversar(@PathParam("idCorrida") Long idCorrida,
            @QueryParam("usuario") String usuario, @QueryParam("ip") String ip,
            @QueryParam("motivo") String motivo) {
        System.out.println("LLEGA AL SERVICIO REVERSAR - CIERRE_CARTERA - corrida: " + idCorrida
                + " usuario: " + usuario);
        try {
            return Response.status(Response.Status.OK)
                    .entity(cierreCarteraService.reversar(idCorrida, usuario, ip, motivo))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al reversar el cierre de cartera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Histórico de corridas de una empresa, de la más reciente a la más antigua.
     *
     * @param idEmpresa Código de la empresa (SCP.PJRQ)
     */
    @GET
    @Path("/corridas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response corridas(@QueryParam("idEmpresa") Long idEmpresa) {
        System.out.println("LLEGA AL SERVICIO CORRIDAS - CIERRE_CARTERA - empresa: " + idEmpresa);
        try {
            return Response.status(Response.Status.OK)
                    .entity(cierreCarteraService.listarCorridas(idEmpresa))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar las corridas de cierre de cartera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
