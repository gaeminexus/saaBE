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

            // Todos los valores llegan YA FORMATEADOS como String (moneda ecuatoriana
            // "$90.018,75", fechas "d/M/yyyy" o "dd-MM-yyyy" según la fila) — el diseño nuevo
            // (2026-09-30, sobre el Word real del usuario) no delega el formato a Jasper.
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
            parametros.put("P_FECHA_INICIAL_TEXTO", fechaSinCero(d.getFechaInicial()));
            parametros.put("P_FECHA_FINAL_TEXTO", fechaSinCero(d.getFechaFinal()));
            parametros.put("P_FECHA_ULTIMO_COBRO_TEXTO", d.getFechaUltimoCobro() != null ? fechaSinCero(d.getFechaUltimoCobro()) : "-");
            parametros.put("P_MONTO_TEXTO", moneda(d.getMonto()));
            parametros.put("P_CAPITAL_COBRADO_TEXTO", moneda(d.getCapitalCobrado()));
            parametros.put("P_SALDO_CAPITAL_TEXTO", moneda(d.getSaldoCapital()));
            parametros.put("P_INTERES_DEVENGADO_TEXTO", moneda(d.getInteresDevengado()));
            parametros.put("P_INTERES_COBRADO_TEXTO", moneda(d.getInteresCobrado()));
            parametros.put("P_INTERES_SALDO_TEXTO", moneda(d.getSaldoInteres()));
            parametros.put("P_DESGRAVAMEN_DEVENGADO_TEXTO", moneda(d.getDesgravamenDevengado()));
            parametros.put("P_DESGRAVAMEN_COBRADO_TEXTO", moneda(d.getDesgravamenCobrado()));
            parametros.put("P_DESGRAVAMEN_SALDO_TEXTO", moneda(d.getSaldoDesgravamen()));
            parametros.put("P_SEGURO_DEVENGADO_TEXTO", moneda(d.getSeguroDevengado()));
            parametros.put("P_SEGURO_COBRADO_TEXTO", moneda(d.getSeguroCobrado()));
            parametros.put("P_SEGURO_SALDO_TEXTO", moneda(d.getSaldoSeguro()));
            parametros.put("P_MORA_DEVENGADA_TEXTO", moneda(d.getMoraDevengada()));
            parametros.put("P_MORA_COBRADA_TEXTO", moneda(d.getMoraCobrada()));
            parametros.put("P_MORA_SALDO_TEXTO", moneda(d.getSaldoMora()));
            parametros.put("P_TOTAL_COBRADO_TEXTO", moneda(d.getTotalCobrado()));
            parametros.put("P_TOTAL_POR_COBRAR_TEXTO", moneda(d.getTotalPorCobrar()));
            parametros.put("P_FECHA_CORTE_GUION", fechaGuion(d.getFechaCorte()));
            parametros.put("P_PLAZO_TEXTO", String.valueOf(d.getCuotasPlazo()));
            parametros.put("P_CUOTAS_COBRADAS_TEXTO", String.valueOf(d.getCuotasCobradas()));
            parametros.put("P_CUOTAS_PENDIENTES_TEXTO", String.valueOf(d.getCuotasPendientes()));
            parametros.put("P_CUOTAS_POR_VENCER_TEXTO", String.valueOf(d.getCuotasPorVencer()));
            parametros.put("P_DIVIDENDO_MENSUAL_TEXTO", moneda(d.getDividendoMensual()));
            parametros.put("P_REVERTIDA", DeclaracionPlazoVencido.ESTADO_REVERTIDA == (d.getEstado() != null ? d.getEstado() : 0L));

            // Párrafos con negrita embebida (markup="styled" en el .jrxml): se construyen acá,
            // no en el reporte, porque la negrita cae solo sobre ciertos tramos del texto
            // justificado (diseño 2026-09-30, sobre el Word real del usuario).
            parametros.put("P_PARRAFO_APERTURA",
                "Una vez realizada la revisión en el sistema SAA; y, del análisis del estado del Crédito "
                + "<b>" + escapeHtml(d.getTipoCredito()) + " No. " + escapeHtml(d.getNumeroPrestamoImpreso()) + "</b>"
                + ", otorgado por ASOPREP-FCPC a favor de <b>" + escapeHtml(d.getNombreParticipe()) + "</b>"
                + ", portador de la cédula de identidad <b>No. " + escapeHtml(d.getCedula()) + "</b>, se desprende lo siguiente:");
            parametros.put("P_PARRAFO_CIERRE_1",
                "En virtud de lo expuesto y con el propósito de recuperación de los valores adeudados correspondientes al "
                + "<b>Préstamo " + escapeHtml(d.getTipoCredito()) + " No. " + escapeHtml(d.getNumeroPrestamoImpreso())
                + " se declara en estado de plazo vencido por incumplimiento de pago.</b>");

            // generarReporte() llena con una CONEXIÓN JDBC: como este .jrxml no tiene <query>,
            // el datasource da CERO filas y, con whenNoDataType="AllSectionsNoDetail", Jasper
            // se salta el <detail> entero (title/pageHeader salen, el cuerpo no). Se usa
            // generarReporteDesdeColeccion con UNA fila vacía para forzar exactamente una
            // ejecución del detail — el reporte no lee el bean, todo sale de los parámetros
            // (verificado 2026-09-30: ningún <field> declarado en el .jasper).
            byte[] bytes = reporteService.generarReporteDesdeColeccion("crd", "RPRT_PLVN_MMRN",
                parametros, java.util.Collections.singletonList(new java.util.HashMap<String, Object>()), formato);
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
            parametros.put("P_MONTO_TEXTO", moneda(d.getMonto()));
            parametros.put("P_FECHA_FINAL_TEXTO", fechaLarga(d.getFechaFinal()));
            parametros.put("P_NOMBRE_PARTICIPE", d.getNombreParticipe());
            parametros.put("P_CEDULA", d.getCedula());
            parametros.put("P_CUOTAS_IMPAGAS", d.getLiquidacionCuotasImpagas() != null ? String.valueOf(d.getLiquidacionCuotasImpagas()) : "0");
            parametros.put("P_CUOTAS_IMPAGAS_LETRAS", NumeroALetrasUtil.convertir(d.getLiquidacionCuotasImpagas()));
            parametros.put("P_MES_INICIO_MORA", d.getFechaInicioMora() != null ? MESES[d.getFechaInicioMora().getMonthValue() - 1] : "");
            parametros.put("P_ANIO_INICIO_MORA", d.getFechaInicioMora() != null ? String.valueOf(d.getFechaInicioMora().getYear()) : "");
            parametros.put("P_FECHA_CORTE_GUION", fechaGuion(d.getFechaCorteLiquidacion()));
            parametros.put("P_LQ_SALDO_CAPITAL_TEXTO", moneda(d.getLiquidacionSaldoCapital()));
            parametros.put("P_LQ_INTERES_MORA_TEXTO", moneda(d.getLiquidacionMora()));
            parametros.put("P_LQ_INTERES_VENCIDO_TEXTO", moneda(d.getLiquidacionInteres()));
            parametros.put("P_LQ_DESGRAVAMEN_TEXTO", moneda(d.getLiquidacionDesgravamen()));
            parametros.put("P_LQ_SEGURO_INCENDIO_TEXTO", moneda(d.getLiquidacionSeguro()));
            parametros.put("P_LQ_TOTAL_TEXTO", moneda(d.getLiquidacionTotal()));
            parametros.put("P_REVERTIDA", DeclaracionPlazoVencido.ESTADO_REVERTIDA == (d.getEstado() != null ? d.getEstado() : 0L));

            parametros.put("P_PARRAFO_1",
                "Mediante Memorando Nro. " + escapeHtml(d.getNumeroMemorando()) + " de " + fechaLarga(d.getFechaCorte())
                + ", el jefe de Crédito remite la <b>ORDEN DE COBRO</b> del Préstamo " + escapeHtml(d.getTipoCredito())
                + " No. " + escapeHtml(d.getNumeroPrestamoImpreso()) + " otorgado el " + fechaLarga(d.getFechaInicial())
                + " por el valor de USD " + monedaSinSimbolo(d.getMonto()) + ", con vencimiento el " + fechaLarga(d.getFechaFinal())
                + ", a favor de <b>" + escapeHtml(d.getNombreParticipe()) + "</b>, con cédula de ciudadanía No. <b>"
                + escapeHtml(d.getCedula()) + "</b>; a través del cual, declara la referida obligación de "
                + "<b><i>\"...plazo vencido por incumplimiento de pago.\"</i></b>");
            parametros.put("P_PARRAFO_2",
                "De lo expuesto y con la finalidad de regularizar el registro administrativo y contable de la obligación, "
                + "procedo con la emisión de la liquidación contable, para lo cual se realizó la verificación de la información "
                + "registrada en el sistema SAA, evidenciándose que <b>" + escapeHtml(d.getNombreParticipe()) + "</b>, mantiene "
                + NumeroALetrasUtil.convertir(d.getLiquidacionCuotasImpagas()) + " ("
                + (d.getLiquidacionCuotasImpagas() != null ? d.getLiquidacionCuotasImpagas() : 0) + ") cuotas impagas "
                + "correspondientes al Crédito " + escapeHtml(d.getTipoCredito()) + " No. " + escapeHtml(d.getNumeroPrestamoImpreso())
                + ", registrándose el inicio del estado de mora desde el mes de "
                + (d.getFechaInicioMora() != null ? MESES[d.getFechaInicioMora().getMonthValue() - 1] : "")
                + " de " + (d.getFechaInicioMora() != null ? String.valueOf(d.getFechaInicioMora().getYear()) : "")
                + "; en tal razón, se procede a realizar la siguiente liquidación contable con corte al "
                + fechaLarga(d.getFechaCorteLiquidacion()) + ", conforme el siguiente detalle:");

            // Mismo motivo que en memorando(): sin <query>, generarReporte() (conexión JDBC) da
            // cero filas y Jasper se salta el <detail>. Una fila vacía fuerza la ejecución.
            byte[] bytes = reporteService.generarReporteDesdeColeccion("crd", "RPRT_PLVN_LQDC",
                parametros, java.util.Collections.singletonList(new java.util.HashMap<String, Object>()), formato);
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

    /** "d/M/yyyy", sin ceros a la izquierda — formato de las fechas dentro de la tabla (diseño 2026-09-30). */
    private String fechaSinCero(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return fecha.getDayOfMonth() + "/" + fecha.getMonthValue() + "/" + fecha.getYear();
    }

    /** "dd-MM-yyyy" — formato de "TOTAL POR COBRAR AL ..." / "TOTAL ADEUDADO AL ...". */
    private String fechaGuion(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return String.format("%02d-%02d-%d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
    }

    /**
     * "$90.018,75" — moneda ecuatoriana (punto de miles, coma decimal), formateada en Java y NO
     * delegada a un {@code pattern} de Jasper (evita depender del locale del reporte). Símbolos
     * explícitos, no el locale del JVM — no depende de qué locale tenga instalado el servidor.
     */
    private String moneda(Double valor) {
        return "$" + monedaSinSimbolo(valor);
    }

    /** Igual que {@link #moneda}, sin el "$" — para textos como "por el valor de USD 90.018,75". */
    private String monedaSinSimbolo(Double valor) {
        java.text.DecimalFormatSymbols simbolos = new java.text.DecimalFormatSymbols(java.util.Locale.US);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        java.text.DecimalFormat formato = new java.text.DecimalFormat("#,##0.00", simbolos);
        return formato.format(valor != null ? valor : 0.0);
    }

    /**
     * Escapa para meter texto arbitrario (nombre, cédula) dentro de un textField con
     * {@code markup="styled"}: sin esto, un nombre con "&" o "<" rompería el markup del reporte
     * en vez de imprimirse tal cual.
     */
    private String escapeHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
