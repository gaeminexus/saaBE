package com.saa.ws.rest.crd;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.saa.ejb.crd.service.DistribucionBandaService;
import com.saa.ejb.crd.service.dto.FiltroDetalleDistribucionBanda;
import com.saa.ejb.crd.service.dto.OrigenDistribucionBandaResumen;
import com.saa.ejb.crd.service.dto.ResultadoCuadreDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoDetalleDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoDiferenciaDistribucionBanda;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Auditoría de distribución en bandas — PLAN-AUDITORIA-BANDAS.md / API-AUDITORIA-BANDAS.md.
 * Casi todo este REST es de solo lectura (la pantalla audita lo que ya ocurrió); la única
 * excepción es {@link #recalcularDistribucion}, que reescribe CRD.DSBN sin reprocesar nada —
 * ver su javadoc.
 *
 * @see DistribucionBandaService
 */
@Path("dsbn")
public class DistribucionBandaRest {

    @EJB
    private DistribucionBandaService distribucionBandaService;

    public DistribucionBandaRest() {
    }

    /** El encabezado de cuadre — API-AUDITORIA-BANDAS.md §1. Es lo primero que pinta la pantalla. */
    @GET
    @Path("/cuadre")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cuadre(@QueryParam("origen") String origen, @QueryParam("idOrigen") Long idOrigen) {
        System.out.println("LLEGA AL SERVICIO CUADRE DISTRIBUCION BANDAS - " + origen + "/" + idOrigen);
        if (origen == null || idOrigen == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar origen e idOrigen", null);
        }
        try {
            ResultadoCuadreDistribucionBanda resultado = distribucionBandaService.obtenerCuadre(origen, idOrigen);
            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * «¿De quién es la diferencia?» — API-AUDITORIA-BANDAS.md §4. El cuadre dice QUE hay
     * diferencia; esto dice DE QUIÉN. Botón en pantalla, visible solo cuando {@code cuadra === false}.
     */
    @GET
    @Path("/diferencia")
    @Produces(MediaType.APPLICATION_JSON)
    public Response diferencia(@QueryParam("origen") String origen, @QueryParam("idOrigen") Long idOrigen) {
        System.out.println("LLEGA AL SERVICIO DIFERENCIA DISTRIBUCION BANDAS - " + origen + "/" + idOrigen);
        if (origen == null || idOrigen == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar origen e idOrigen", null);
        }
        try {
            ResultadoDiferenciaDistribucionBanda resultado = distribucionBandaService.obtenerDiferencia(origen, idOrigen);
            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** El detalle filtrable — API-AUDITORIA-BANDAS.md §2. Va por POST porque los filtros son largos. */
    @POST
    @Path("/detalle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response detalle(FiltroDetalleDistribucionBanda filtro) {
        System.out.println("LLEGA AL SERVICIO DETALLE DISTRIBUCION BANDAS - "
            + (filtro != null ? filtro.getOrigen() + "/" + filtro.getIdOrigen() : null));
        if (filtro == null || filtro.getOrigen() == null || filtro.getIdOrigen() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar origen e idOrigen", null);
        }
        try {
            ResultadoDetalleDistribucionBanda resultado = distribucionBandaService.obtenerDetalle(filtro);
            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** Alimenta el selector de orígenes — API-AUDITORIA-BANDAS.md §3. */
    @GET
    @Path("/origenes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response origenes(@QueryParam("origen") String origen,
            @QueryParam("fechaDesde") String fechaDesde,
            @QueryParam("fechaHasta") String fechaHasta,
            @QueryParam("limite") Integer limite) {
        System.out.println("LLEGA AL SERVICIO ORIGENES DISTRIBUCION BANDAS");
        try {
            LocalDate desde = fechaDesde != null && !fechaDesde.trim().isEmpty() ? LocalDate.parse(fechaDesde) : null;
            LocalDate hasta = fechaHasta != null && !fechaHasta.trim().isEmpty() ? LocalDate.parse(fechaHasta) : null;
            List<OrigenDistribucionBandaResumen> resultado =
                distribucionBandaService.listarOrigenes(origen, desde, hasta, limite);
            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * Reescribe la distribución en bandas de una carga Petro YA PROCESADA, sin reprocesar nada
     * — hallazgo 2026-09-03: el WAR desplegado cuando se procesó la carga 449 era anterior a
     * {@code 500079b} (el commit que agregó el registro de aportes en CRD.DSBN), así que quedó
     * sin esas filas. Ver el javadoc de
     * {@link DistribucionBandaService#recalcularDistribucionCargaPetro} para los tres límites
     * (solo reescribe DSBN, idAsiento del ANCP vigente, idempotente).
     *
     * Solo {@code CARGA_PETRO} por ahora. {@code usuario} es opcional — "sistema" si no se
     * indica, mismo valor que usa el procesamiento normal cuando no hay un usuario puntual.
     */
    @POST
    @Path("/recalcularDistribucion")
    @Produces(MediaType.APPLICATION_JSON)
    public Response recalcularDistribucion(@QueryParam("origen") String origen,
            @QueryParam("idOrigen") Long idOrigen, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO RECALCULAR DISTRIBUCION BANDAS - " + origen + "/" + idOrigen);
        if (origen == null || idOrigen == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar origen e idOrigen", null);
        }
        if (!com.saa.rubros.DsbnOrigen.CARGA_PETRO.equals(origen)) {
            return respuestaFallo(HTTP_REGLA_DE_NEGOCIO,
                "El recálculo de distribución solo está disponible hoy para CARGA_PETRO", null);
        }
        try {
            java.util.Map<Long, com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda> clasificacionPorPago =
                distribucionBandaService.recalcularDistribucionCargaPetro(idOrigen,
                    usuario != null && !usuario.trim().isEmpty() ? usuario : "sistema");

            java.util.Map<String, Object> cuerpo = new java.util.LinkedHashMap<>();
            cuerpo.put("idCarga", idOrigen);
            cuerpo.put("pagosClasificados", clasificacionPorPago.size());
            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ------------------------------------------------------------------------
    // Sobre de respuesta y mapeo de errores (mismo convenio que PagoPensionComplementariaRest)
    // ------------------------------------------------------------------------

    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    private static final List<String> CODIGOS_404 = Arrays.asList("ORIGEN_NO_ENCONTRADO");

    private static final List<String> CODIGOS_422 = Arrays.asList("ORIGEN_INVALIDO");

    private Response respuestaError(Throwable e) {
        System.err.println("ERROR en DistribucionBandaRest: " + e.getMessage());
        e.printStackTrace();

        String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
        String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

        int status;
        if (CODIGOS_404.contains(codigo)) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (CODIGOS_422.contains(codigo) || e instanceof com.saa.basico.util.IncomeException) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        return respuestaFallo(status, mensaje, codigo);
    }

    private Response respuestaFallo(int status, String mensaje, String codigo) {
        java.util.Map<String, Object> cuerpo = new java.util.LinkedHashMap<>();
        cuerpo.put("mensaje", mensaje);
        if (codigo != null && !codigo.isEmpty()) {
            cuerpo.put("error", codigo);
        }
        return Response.status(status)
                .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }
}
