package com.saa.ws.rest.crd;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DeclaracionPlazoVencidoDaoService;
import com.saa.ejb.crd.service.DeclaracionPlazoVencidoService;
import com.saa.ejb.crd.service.dto.CandidatoPlazoVencido;
import com.saa.ejb.crd.service.dto.DeclaracionPlazoVencidoDTO;
import com.saa.ejb.crd.service.dto.EncabezadoPlazoVencido;
import com.saa.ejb.crd.service.dto.ResultadoDeclararPlazoVencido;
import com.saa.ejb.crd.service.dto.ResultadoRevertirPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudDeclararPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudLiquidarPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudRevertirPlazoVencido;
import com.saa.ejb.crd.util.NumeroALetrasUtil;
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.model.crd.DeclaracionPlazoVencido;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Pase de préstamos EN MORA a DE PLAZO VENCIDO: memorando (Crédito), liquidación contable
 * (Contabilidad) y reverso. Ver {@code docs/logica-negocio/crd/API-PASE-A-PLAZO-VENCIDO.md}.
 */
@Path("plvn")
public class DeclaracionPlazoVencidoRest {

    /** 422 UNPROCESSABLE ENTITY - no existe en el enum Response.Status de Jakarta REST */
    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    private static final String[] MESES = {
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    @EJB
    private DeclaracionPlazoVencidoDaoService declaracionPlazoVencidoDaoService;

    @EJB
    private DeclaracionPlazoVencidoService declaracionPlazoVencidoService;

    @EJB
    private ReporteService reporteService;

    public DeclaracionPlazoVencidoRest() {}

    // ========================================================================
    // Consultas (§3, §4, §9)
    // ========================================================================

    @GET
    @Path("/candidatos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response candidatos(@QueryParam("fechaCorte") String fechaCorteParam) {
        System.out.println("LLEGA AL SERVICIO candidatos - PLVN - fechaCorte: " + fechaCorteParam);
        try {
            LocalDate fechaCorte = parseFecha(fechaCorteParam);
            List<CandidatoPlazoVencido> candidatos = declaracionPlazoVencidoService.obtenerCandidatos(fechaCorte);
            return Response.status(Response.Status.OK).entity(candidatos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/ultimoEncabezado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ultimoEncabezado() {
        System.out.println("LLEGA AL SERVICIO ultimoEncabezado - PLVN");
        try {
            EncabezadoPlazoVencido encabezado = declaracionPlazoVencidoService.obtenerUltimoEncabezado();
            return Response.status(Response.Status.OK).entity(encabezado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("estado") Long estado,
            @QueryParam("desde") String desdeParam, @QueryParam("hasta") String hastaParam) {
        System.out.println("LLEGA AL SERVICIO listar - PLVN - estado: " + estado);
        try {
            LocalDate desde = desdeParam != null && !desdeParam.trim().isEmpty() ? LocalDate.parse(desdeParam.trim()) : null;
            LocalDate hasta = hastaParam != null && !hastaParam.trim().isEmpty() ? LocalDate.parse(hastaParam.trim()) : null;
            List<DeclaracionPlazoVencidoDTO> declaraciones = declaracionPlazoVencidoService.listar(estado, desde, hasta);
            return Response.status(Response.Status.OK).entity(declaraciones).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO getId - PLVN id: " + id);
        try {
            DeclaracionPlazoVencidoDTO declaracion = declaracionPlazoVencidoService.obtenerPorId(id);
            return Response.status(Response.Status.OK).entity(declaracion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // POST /declarar, POST /{id}/liquidar, POST /{id}/revertir (§5, §6, §7)
    // ========================================================================

    @POST
    @Path("/declarar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response declarar(SolicitudDeclararPlazoVencido solicitud) {
        System.out.println("LLEGA AL SERVICIO declarar - PLVN");
        try {
            List<ResultadoDeclararPlazoVencido> resultado = declaracionPlazoVencidoService.declarar(solicitud);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @POST
    @Path("/{id}/liquidar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response liquidar(@PathParam("id") Long id, SolicitudLiquidarPlazoVencido solicitud) {
        System.out.println("LLEGA AL SERVICIO liquidar - PLVN id: " + id);
        try {
            DeclaracionPlazoVencidoDTO resultado = declaracionPlazoVencidoService.liquidar(id, solicitud);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @POST
    @Path("/{id}/revertir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertir(@PathParam("id") Long id, SolicitudRevertirPlazoVencido solicitud) {
        System.out.println("LLEGA AL SERVICIO revertir - PLVN id: " + id);
        try {
            ResultadoRevertirPlazoVencido resultado = declaracionPlazoVencidoService.revertir(id, solicitud);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // Documentos (§8)
    // ========================================================================

    @GET
    @Path("/{id}/memorando")
    public Response memorando(@PathParam("id") Long id, @QueryParam("formato") @DefaultValue("PDF") String formato) {
        System.out.println("LLEGA AL SERVICIO memorando - PLVN id: " + id + " - formato: " + formato);
        try {
            if (!"PDF".equalsIgnoreCase(formato) && !"DOCX".equalsIgnoreCase(formato)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("PARAMETRO_INVALIDO: formato debe ser PDF o DOCX")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            DeclaracionPlazoVencido d = buscarEntidad(id);

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("P_NUMERO_MEMORANDO", d.getNumeroMemorando());
            parametros.put("P_PARA_NOMBRE", d.getParaNombre());
            parametros.put("P_PARA_CARGO", d.getParaCargo());
            parametros.put("P_CC_NOMBRE", d.getCcNombre());
            parametros.put("P_CC_CARGO", d.getCcCargo());
            parametros.put("P_TIPO_CREDITO", d.getTipoCredito());
            parametros.put("P_NUMERO_PRESTAMO", d.getNumeroPrestamoImpreso());
            parametros.put("P_FECHA_MEMO_TEXTO", fechaLarga(d.getFechaCorte()));
            parametros.put("P_NOMBRE_PARTICIPE", d.getNombreParticipe());
            parametros.put("P_CEDULA", d.getCedula());
            parametros.put("P_FECHA_INICIAL_TEXTO", fechaCorta(d.getFechaInicial()));
            parametros.put("P_FECHA_FINAL_TEXTO", fechaCorta(d.getFechaFinal()));
            parametros.put("P_FECHA_ULTIMO_COBRO_TEXTO", fechaCorta(d.getFechaUltimoCobro()));
            parametros.put("P_MONTO", d.getMonto());
            parametros.put("P_CAPITAL_COBRADO", d.getCapitalCobrado());
            parametros.put("P_SALDO_CAPITAL", d.getSaldoCapital());
            parametros.put("P_INTERES_DEVENGADO", d.getInteresDevengado());
            parametros.put("P_INTERES_COBRADO", d.getInteresCobrado());
            parametros.put("P_INTERES_SALDO", d.getSaldoInteres());
            parametros.put("P_DESGRAVAMEN_DEVENGADO", d.getDesgravamenDevengado());
            parametros.put("P_DESGRAVAMEN_COBRADO", d.getDesgravamenCobrado());
            parametros.put("P_DESGRAVAMEN_SALDO", d.getSaldoDesgravamen());
            parametros.put("P_SEGURO_DEVENGADO", d.getSeguroDevengado());
            parametros.put("P_SEGURO_COBRADO", d.getSeguroCobrado());
            parametros.put("P_SEGURO_SALDO", d.getSaldoSeguro());
            parametros.put("P_MORA_DEVENGADA", d.getMoraDevengada());
            parametros.put("P_MORA_COBRADA", d.getMoraCobrada());
            parametros.put("P_MORA_SALDO", d.getSaldoMora());
            parametros.put("P_TOTAL_COBRADO", d.getTotalCobrado());
            parametros.put("P_TOTAL_POR_COBRAR", d.getTotalPorCobrar());
            parametros.put("P_FECHA_CORTE_TEXTO", fechaCorta(d.getFechaCorte()));
            parametros.put("P_PLAZO", d.getCuotasPlazo());
            parametros.put("P_CUOTAS_COBRADAS", d.getCuotasCobradas());
            parametros.put("P_CUOTAS_PENDIENTES", d.getCuotasPendientes());
            parametros.put("P_CUOTAS_POR_VENCER", d.getCuotasPorVencer());
            parametros.put("P_DIVIDENDO_MENSUAL", d.getDividendoMensual());
            parametros.put("P_REVERTIDA", DeclaracionPlazoVencido.ESTADO_REVERTIDA == (d.getEstado() != null ? d.getEstado() : 0L));
            parametros.put("P_USUARIO", d.getUsuarioDeclaracion());

            byte[] bytes = reporteService.generarReporte("crd", "RPRT_PLVN_MMRN", parametros, formato);
            return respuestaArchivo(bytes, formato, "ORDEN_DE_COBRO_" + nombreParaArchivo(d.getNombreParticipe()));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/{id}/liquidacion")
    public Response liquidacion(@PathParam("id") Long id, @QueryParam("formato") @DefaultValue("PDF") String formato) {
        System.out.println("LLEGA AL SERVICIO liquidacion - PLVN id: " + id + " - formato: " + formato);
        try {
            if (!"PDF".equalsIgnoreCase(formato) && !"DOCX".equalsIgnoreCase(formato)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("PARAMETRO_INVALIDO: formato debe ser PDF o DOCX")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            DeclaracionPlazoVencido d = buscarEntidad(id);
            if (d.getEstado() != null && d.getEstado() == DeclaracionPlazoVencido.ESTADO_DECLARADA) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(DeclaracionPlazoVencidoService.ERR_NO_LIQUIDADA + ": la declaración " + id + " todavía no fue liquidada")
                    .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("P_CIUDAD", "Quito");
            parametros.put("P_FECHA_EMISION_TEXTO", fechaLarga(d.getFechaCorteLiquidacion()));
            parametros.put("P_NUMERO_PRESTAMO", d.getNumeroPrestamoImpreso());
            parametros.put("P_NUMERO_MEMORANDO", d.getNumeroMemorando());
            parametros.put("P_FECHA_MEMO_TEXTO", fechaLarga(d.getFechaCorte()));
            parametros.put("P_TIPO_CREDITO", d.getTipoCredito());
            parametros.put("P_FECHA_INICIAL_TEXTO", fechaLarga(d.getFechaInicial()));
            parametros.put("P_MONTO", d.getMonto());
            parametros.put("P_FECHA_FINAL_TEXTO", fechaLarga(d.getFechaFinal()));
            parametros.put("P_NOMBRE_PARTICIPE", d.getNombreParticipe());
            parametros.put("P_CEDULA", d.getCedula());
            parametros.put("P_CUOTAS_IMPAGAS", d.getLiquidacionCuotasImpagas());
            parametros.put("P_CUOTAS_IMPAGAS_LETRAS", NumeroALetrasUtil.convertir(d.getLiquidacionCuotasImpagas()));
            parametros.put("P_MES_INICIO_MORA", d.getFechaInicioMora() != null ? MESES[d.getFechaInicioMora().getMonthValue() - 1] : "");
            parametros.put("P_ANIO_INICIO_MORA", d.getFechaInicioMora() != null ? String.valueOf(d.getFechaInicioMora().getYear()) : "");
            parametros.put("P_FECHA_CORTE_TEXTO", fechaLarga(d.getFechaCorteLiquidacion()));
            parametros.put("P_LQ_SALDO_CAPITAL", d.getLiquidacionSaldoCapital());
            parametros.put("P_LQ_INTERES_MORA", d.getLiquidacionMora());
            parametros.put("P_LQ_INTERES_VENCIDO", d.getLiquidacionInteres());
            parametros.put("P_LQ_DESGRAVAMEN", d.getLiquidacionDesgravamen());
            parametros.put("P_LQ_SEGURO_INCENDIO", d.getLiquidacionSeguro());
            parametros.put("P_LQ_TOTAL", d.getLiquidacionTotal());
            parametros.put("P_REVERTIDA", DeclaracionPlazoVencido.ESTADO_REVERTIDA == (d.getEstado() != null ? d.getEstado() : 0L));
            parametros.put("P_USUARIO", d.getUsuarioLiquidacion());

            byte[] bytes = reporteService.generarReporte("crd", "RPRT_PLVN_LQDC", parametros, formato);
            return respuestaArchivo(bytes, formato, "LIQUIDACION_" + nombreParaArchivo(d.getNombreParticipe()));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private DeclaracionPlazoVencido buscarEntidad(Long id) throws Throwable {
        try {
            return declaracionPlazoVencidoDaoService.selectById(id, NombreEntidadesCredito.DECLARACION_PLAZO_VENCIDO);
        } catch (NoResultException e) {
            throw new IncomeException(DeclaracionPlazoVencidoService.ERR_DECLARACION_NO_ENCONTRADA
                + ": no existe la declaración " + id);
        }
    }

    private Response respuestaArchivo(byte[] bytes, String formato, String nombreBase) {
        boolean esDocx = "DOCX".equalsIgnoreCase(formato);
        String contentType = esDocx
            ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            : "application/pdf";
        String extension = esDocx ? ".docx" : ".pdf";
        return Response.ok(new ByteArrayInputStream(bytes))
            .header("Content-Disposition", "attachment; filename=\"" + nombreBase + extension + "\"")
            .header("Content-Type", contentType)
            .build();
    }

    /** Apellidos/nombres para el nombre de archivo: espacios a guión bajo, sin acentos raros. */
    private String nombreParaArchivo(String nombreParticipe) {
        if (nombreParticipe == null || nombreParticipe.trim().isEmpty()) {
            return "PARTICIPE";
        }
        return nombreParticipe.trim().toUpperCase().replaceAll("[^A-ZÁÉÍÓÚÑ0-9]+", "_");
    }

    private LocalDate parseFecha(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IncomeException(DeclaracionPlazoVencidoService.ERR_PARAMETRO_INVALIDO + ": fechaCorte es obligatoria");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (Exception e) {
            throw new IncomeException(DeclaracionPlazoVencidoService.ERR_PARAMETRO_INVALIDO
                + ": fechaCorte '" + valor + "' no es una fecha válida (yyyy-MM-dd)");
        }
    }

    private String fechaLarga(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return String.format("%02d de %s de %d", fecha.getDayOfMonth(), MESES[fecha.getMonthValue() - 1], fecha.getYear());
    }

    private String fechaCorta(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return String.format("%02d/%02d/%d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
    }

    /**
     * Mapea el CODIGO con el que {@code DeclaracionPlazoVencidoService} prefija sus
     * {@code IncomeException} ({@code CODIGO: descripción}) al status HTTP exacto del contrato
     * (§2.6): 400 / 404 / 409 / 422, mismo patrón que {@code CuentaBancariaBeneficiarioRest}.
     */
    private Response respuestaError(Throwable e) {
        String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
        String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

        int status;
        if (DeclaracionPlazoVencidoService.ERR_MEMORANDO_DUPLICADO.equals(codigo)
                || DeclaracionPlazoVencidoService.ERR_PRESTAMO_NO_EN_MORA.equals(codigo)
                || DeclaracionPlazoVencidoService.ERR_YA_LIQUIDADA.equals(codigo)
                || DeclaracionPlazoVencidoService.ERR_DECLARACION_REVERTIDA.equals(codigo)
                || DeclaracionPlazoVencidoService.ERR_PRESTAMO_NO_EN_PLAZO_VENCIDO.equals(codigo)
                || DeclaracionPlazoVencidoService.ERR_NO_LIQUIDADA.equals(codigo)) {
            status = Response.Status.CONFLICT.getStatusCode();
        } else if (DeclaracionPlazoVencidoService.ERR_DECLARACION_NO_ENCONTRADA.equals(codigo)) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (DeclaracionPlazoVencidoService.ERR_CALCULO_NO_CUADRA.equals(codigo)) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else if (DeclaracionPlazoVencidoService.ERR_PARAMETRO_INVALIDO.equals(codigo)) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
        } else if (e instanceof NoResultException) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (e instanceof IncomeException) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        return Response.status(status).entity(mensaje).type(MediaType.APPLICATION_JSON).build();
    }
}
