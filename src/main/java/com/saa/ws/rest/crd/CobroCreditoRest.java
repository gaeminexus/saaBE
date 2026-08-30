package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.ejb.crd.dao.CobroCreditoDaoService;
import com.saa.ejb.crd.dao.DetalleCobroCreditoDaoService;
import com.saa.ejb.crd.service.CobroCreditoService;
import com.saa.ejb.crd.service.dto.FilaBandejaAprobacion;
import com.saa.ejb.crd.service.dto.ResultadoProcesoCobro;
import com.saa.ejb.crd.service.dto.ResultadoRegistroCobro;
import com.saa.ejb.crd.service.dto.SolicitudAprobacionCobro;
import com.saa.ejb.crd.service.dto.SolicitudEdicionCobro;
import com.saa.ejb.crd.service.dto.SolicitudRegistroCobro;
import com.saa.model.crd.CobroCredito;
import com.saa.model.crd.DetalleCobroCredito;
import com.saa.model.crd.NombreEntidadesCredito;

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
 * REST de la autorización de contabilidad para cobros individuales (CRD.CBCR/DCBC).
 * Ver {@code docs/logica-negocio/crd/sql/DDL-COBROS-APROBACION-CONTABILIDAD.sql} y
 * {@link CobroCreditoService}.
 *
 * Solo expone REGISTRO (fase 2) y lectura por ahora. Aprobación/rechazo (fase 3) y
 * proceso (fase 4) se agregan a esta misma clase cuando estén listos.
 *
 * @author Sistema SAA
 * @since 2026-08-29
 */
@Path("cbcr")
public class CobroCreditoRest {

    @EJB
    private CobroCreditoDaoService cobroCreditoDaoService;

    @EJB
    private DetalleCobroCreditoDaoService detalleCobroCreditoDaoService;

    @EJB
    private CobroCreditoService cobroCreditoService;

    public CobroCreditoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CobroCredito> lista = cobroCreditoDaoService.selectAll(NombreEntidadesCredito.COBRO_CREDITO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cobros: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CobroCredito cobro = cobroCreditoDaoService.selectById(id, NombreEntidadesCredito.COBRO_CREDITO);
            List<DetalleCobroCredito> detalles = detalleCobroCreditoDaoService.selectByCobro(id);
            return Response.status(Response.Status.OK)
                    .entity(new Object() {
                        public final CobroCredito cabecera = cobro;
                        public final List<DetalleCobroCredito> detalle = detalles;
                    })
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el cobro " + id + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET /rest/cbcr/bandeja/{estado} — bandeja de crédito o de contabilidad según el estado
     * pedido. Ver {@link com.saa.rubros.CrdEstadoCobro}.
     */
    @GET
    @Path("/bandeja/{estado}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bandeja(@PathParam("estado") Long estado) {
        System.out.println("LLEGA AL SERVICIO GET bandeja - CBCR - estado: " + estado);
        try {
            List<CobroCredito> lista = cobroCreditoDaoService.selectByEstado(estado);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la bandeja: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET /rest/cbcr/porEntidad/{idEntidad} — cobros de un partícipe, para su ficha.
     */
    @GET
    @Path("/porEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO GET porEntidad - CBCR - idEntidad: " + idEntidad);
        try {
            List<CobroCredito> lista = cobroCreditoDaoService.selectByEntidad(idEntidad);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los cobros de la entidad " + idEntidad + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/cbcr/registrar — paso 1 (REGISTRO). Ver {@link CobroCreditoService#registrarCobro}.
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(SolicitudRegistroCobro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST registrar - CBCR - idEntidad: "
                + (solicitud != null ? solicitud.getIdEntidad() : null));
        try {
            ResultadoRegistroCobro resultado = cobroCreditoService.registrarCobro(solicitud);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar el cobro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/cbcr/{id}/aprobar — paso 2 (APROBACIÓN), lado contabilidad.
     */
    @POST
    @Path("/{id}/aprobar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aprobar(@PathParam("id") Long id, SolicitudAprobacionCobro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST aprobar - CBCR - id: " + id);
        try {
            CobroCredito cobro = cobroCreditoService.aprobarCobro(id,
                    solicitud != null ? solicitud.getUsuario() : null);
            return Response.status(Response.Status.OK).entity(cobro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al aprobar el cobro " + id + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/cbcr/{id}/rechazar — paso 2 (RECHAZO), lado contabilidad. Motivo obligatorio.
     */
    @POST
    @Path("/{id}/rechazar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rechazar(@PathParam("id") Long id, SolicitudAprobacionCobro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST rechazar - CBCR - id: " + id);
        try {
            CobroCredito cobro = cobroCreditoService.rechazarCobro(id,
                    solicitud != null ? solicitud.getUsuario() : null,
                    solicitud != null ? solicitud.getMotivo() : null);
            return Response.status(Response.Status.OK).entity(cobro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al rechazar el cobro " + id + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/cbcr/{id}/reenviar — crédito corrige y reenvía un cobro RECHAZADO. El body
     * trae los datos corregidos completos ({@link SolicitudEdicionCobro}), no solo el usuario:
     * editar y reenviar son el mismo acto (ver el javadoc de
     * {@link CobroCreditoService#editarYReenviarCobro}).
     */
    @POST
    @Path("/{id}/reenviar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reenviar(@PathParam("id") Long id, SolicitudEdicionCobro correccion) {
        System.out.println("LLEGA AL SERVICIO POST reenviar - CBCR - id: " + id);
        try {
            CobroCredito cobro = cobroCreditoService.editarYReenviarCobro(id,
                    correccion != null ? correccion.getUsuario() : null, correccion);
            return Response.status(Response.Status.OK).entity(cobro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al reenviar el cobro " + id + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/cbcr/{id}/anular — crédito anula porque el depósito nunca llegó al banco.
     * Motivo obligatorio.
     */
    @POST
    @Path("/{id}/anular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, SolicitudAprobacionCobro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST anular - CBCR - id: " + id);
        try {
            CobroCredito cobro = cobroCreditoService.anularCobro(id,
                    solicitud != null ? solicitud.getUsuario() : null,
                    solicitud != null ? solicitud.getMotivo() : null);
            return Response.status(Response.Status.OK).entity(cobro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el cobro " + id + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/cbcr/{id}/procesar — paso 3 (PROCESO), lado crédito. Solo desde APROBADO.
     */
    @POST
    @Path("/{id}/procesar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesar(@PathParam("id") Long id, SolicitudAprobacionCobro solicitud) {
        System.out.println("LLEGA AL SERVICIO POST procesar - CBCR - id: " + id);
        try {
            ResultadoProcesoCobro resultado = cobroCreditoService.procesarCobro(id,
                    solicitud != null ? solicitud.getUsuario() : null);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al procesar el cobro " + id + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET /rest/cbcr/bandejaAprobacion — bandeja combinada (cobros individuales + cargas
     * Petro pendientes), sin modelo común. Ver {@link CobroCreditoService#bandejaAprobacion}.
     */
    @GET
    @Path("/bandejaAprobacion")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bandejaAprobacion() {
        try {
            List<FilaBandejaAprobacion> lista = cobroCreditoService.bandejaAprobacion();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la bandeja de aprobación: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
