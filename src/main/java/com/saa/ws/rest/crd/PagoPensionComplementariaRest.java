package com.saa.ws.rest.crd;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.PagoPensionComplementariaService;
import com.saa.ejb.crd.service.dto.ResultadoGeneracionPagosPension;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
import com.saa.model.crd.PagoPensionComplementaria;

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
 * Pago mensual de pensión complementaria (ítem 4 de jubilados, 2026-08-31).
 *
 * @see PagoPensionComplementariaService
 */
@Path("pgpc")
public class PagoPensionComplementariaRest {

    @EJB
    private PagoPensionComplementariaService pagoPensionService;

    @Context
    private UriInfo context;

    public PagoPensionComplementariaRest() {
    }

    /**
     * Genera los pagos de pensión complementaria del período para TODOS los jubilados
     * elegibles. Idempotente: correrlo dos veces sobre el mismo mes no duplica ningún pago.
     *
     * @param idEmpresa Empresa contable. Obligatorio
     * @param anio      Año del período
     * @param mes       Mes del período (1-12)
     * @param usuario   Usuario/proceso que dispara la generación
     * @return 200 con el resumen de la corrida
     */
    @POST
    @Path("/generarPagosDelMes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarPagosDelMes(
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("anio") Integer anio,
            @QueryParam("mes") Integer mes,
            @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO GENERAR PAGOS DEL MES - Periodo: " + mes + "/" + anio);

        if (idEmpresa == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar idEmpresa", null);
        }
        if (anio == null || mes == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar anio y mes", null);
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el usuario que dispara la generación", null);
        }

        try {
            ResultadoGeneracionPagosPension resultado =
                pagoPensionService.generarPagosDelMes(idEmpresa, anio, mes, usuario);

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("mensaje", "Generación " + mes + "/" + anio + " - " + resultado.getGenerados()
                + " pagos generados, " + resultado.getYaGenerados() + " ya existían, "
                + resultado.getConError() + " con error, de " + resultado.getEvaluados() + " evaluados.");
            cuerpo.put("resultado", resultado);

            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * Reconciliador manual: sincroniza los pagos pendientes contra el estado real en Cuentas
     * por Pagar. Normalmente lo dispara un timer, pero se expone para forzarlo desde la
     * pantalla si hace falta.
     */
    @POST
    @Path("/sincronizarPagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sincronizarPagos() {
        System.out.println("LLEGA AL SERVICIO SINCRONIZAR PAGOS DE PENSION COMPLEMENTARIA");
        try {
            ResultadoSincronizacion resultado = pagoPensionService.sincronizarPagos();
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("resultado", resultado);
            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** Historial de pagos de un jubilado, del más reciente al más antiguo. */
    @GET
    @Path("/porEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO PAGOS POR ENTIDAD - Entidad: " + idEntidad);
        try {
            List<PagoPensionComplementaria> pagos = pagoPensionService.listarPorEntidad(idEntidad);
            return Response.status(Response.Status.OK)
                    .entity(pagos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * Todos los pagos de un período — el informe mensual completo. Existe porque
     * {@code generarPagosDelMes} no puede reconstruirlo en una segunda corrida (idempotencia sin
     * informe, ver contrato REST §4). Un período sin pagos devuelve {@code []}, no 404.
     */
    @GET
    @Path("/porPeriodo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porPeriodo(@QueryParam("anio") Integer anio, @QueryParam("mes") Integer mes) {
        System.out.println("LLEGA AL SERVICIO PAGOS POR PERIODO - Periodo: " + mes + "/" + anio);
        if (anio == null || mes == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar anio y mes", null);
        }
        try {
            List<PagoPensionComplementaria> pagos = pagoPensionService.listarPorPeriodo(anio, mes);
            return Response.status(Response.Status.OK)
                    .entity(pagos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ------------------------------------------------------------------------
    // Sobre de respuesta y mapeo de errores (mismo convenio que AporteRest/PrestamoRest)
    // ------------------------------------------------------------------------

    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    private static final List<String> CODIGOS_400 = Arrays.asList("PARAMETRO_INVALIDO");

    private static final List<String> CODIGOS_404 = Arrays.asList("ENTIDAD_NO_ENCONTRADA", "PAGO_NO_ENCONTRADO");

    private Response respuestaError(Throwable e) {
        System.err.println("ERROR en PagoPensionComplementariaRest: " + e.getMessage());
        e.printStackTrace();

        String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
        String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

        int status;
        if (CODIGOS_400.contains(codigo)) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
        } else if (CODIGOS_404.contains(codigo)) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (e instanceof com.saa.basico.util.IncomeException) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        return respuestaFallo(status, mensaje, codigo);
    }

    private Response respuestaFallo(int status, String mensaje, String codigo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("error", codigo != null && !codigo.isEmpty() ? codigo : mensaje);
        return Response.status(status)
                .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }
}
