package com.saa.ws.rest.cnt;


import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.TempReportesDaoService;
import com.saa.ejb.cnt.service.TempReportesService;
import com.saa.ejb.cnt.estadofinanciero.EncabezadoEstadoFinanciero;
import com.saa.ejb.cnt.estadofinanciero.SolicitudEstadoFinanciero;
import com.saa.ejb.cnt.estructura.ArchivoEstructura;
import com.saa.ejb.cnt.estructura.ResultadoEstructura;
import com.saa.ejb.cnt.service.EstadoFinancieroOficialService;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.ParametrosBalance;
import com.saa.model.cnt.RespuestaBalance;
import com.saa.model.cnt.TempReportes;

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

@Path("dtmt")
public class TempReportesRest {

    @EJB
    private TempReportesDaoService tempReportesDaoService;

    @EJB
    private TempReportesService tempReportesService;

    @EJB
    private EstadoFinancieroOficialService estadoFinancieroOficialService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TempReportesRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of TempReportesRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempReportes> lista = tempReportesDaoService.selectAll(NombreEntidadesContabilidad.TEMP_REPORTES);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener reportes temporales: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of TempReportesRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getDesc")
//    public List<TempReportes> getDesc() throws Throwable {
//        return tempReportesDaoService.selectOrderDesc();
//    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of TempReportesRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getTest")
//    public Response getTest() throws Throwable {
//        return Response.status(200).entity(tempReportesDaoService.selectOrderDesc()).build();
//    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TempReportes temp = tempReportesDaoService.selectById(id, NombreEntidadesContabilidad.TEMP_REPORTES);
            if (temp == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Reporte temporal con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(temp).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener reporte temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of TempReportesRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TEMP_REPORTES");
        try {
            return Response.status(Response.Status.OK)
                    .entity(tempReportesService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - Generar balance contable por rango de fechas
     * Este endpoint genera los registros temporales del balance
     * 
     * @param parametros Parámetros para la generación del balance
     * @return RespuestaBalance con el idEjecucion y total de registros
     */
    @POST
    @Path("generarBalance")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarBalance(ParametrosBalance parametros) {
        System.out.println("LLEGA AL SERVICIO generarBalance - TEMP_REPORTES");
        try {
            // Validaciones
            if (parametros.getFechaInicio() == null || parametros.getFechaFin() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Las fechas de inicio y fin son obligatorias")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            
            if (parametros.getFechaInicio().isAfter(parametros.getFechaFin())) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("La fecha de inicio no puede ser mayor a la fecha fin")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            
            if (parametros.getEmpresa() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El ID de empresa es obligatorio")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            
            // C19b (D-031): reporte de renglones propios (B17, flujo de caja) — mismo endpoint y mismo resultado en
            // DTMT; ademas devuelve suma de control, advertencias y el nombre del archivo si tiene salida en archivo.
            if (tempReportesService.esReporteDeRenglones(parametros.getEmpresa(), parametros.getCodigoAlterno())) {
                ResultadoEstructura r = tempReportesService.calculaRenglones(parametros);
                Long id = tempReportesService.reporteRenglones(r);
                if (parametros.getEliminarSaldosCero() != null && parametros.getEliminarSaldosCero()) {
                    tempReportesDaoService.deleteBySaldosCeroIdEjecucion(id);
                }
                RespuestaBalance respuesta = new RespuestaBalance(id, tempReportesDaoService.selectBySecuencia(id).size(),
                        "Reporte " + (r.isPreliminar() ? "PRELIMINAR " : "") + "generado exitosamente");
                respuesta.setSumaControl(r.getSumaControl());
                respuesta.setAdvertencias(r.getAdvertencias());
                respuesta.setNombreArchivo(r.getNombreArchivo());
                respuesta.setPreliminar(r.isPreliminar());
                return Response.status(Response.Status.CREATED).entity(respuesta).type(MediaType.APPLICATION_JSON).build();
            }

            Long idEjecucion = null;

            // Determinar tipo de reporte según parámetros
            if (parametros.getReporteDistribuido() != null && parametros.getReporteDistribuido()) {
                // Reporte distribuido: plan de cuentas por centros de costo
                idEjecucion = tempReportesService.reportePlanCentroRangoFecha(
                    parametros.getFechaFin(),
                    parametros.getFechaInicio(),
                    parametros.getEmpresa(),
                    parametros.getCodigoAlterno(),
                    parametros.getAcumulacion() != null ? parametros.getAcumulacion() : 0
                );
            } else if (parametros.getIncluyeCentrosCosto() != null && parametros.getIncluyeCentrosCosto()) {
                // Reporte con centros de costo
                idEjecucion = tempReportesService.reporteCentroRangoFecha(
                    parametros.getFechaFin(),
                    parametros.getFechaInicio(),
                    parametros.getEmpresa(),
                    parametros.getCodigoAlterno(),
                    parametros.getAcumulacion() != null ? parametros.getAcumulacion() : 0
                );
            } else {
                // Reporte estándar
                idEjecucion = tempReportesService.reporteRangoFecha(
                    parametros.getFechaFin(),
                    parametros.getFechaInicio(),
                    parametros.getEmpresa(),
                    parametros.getCodigoAlterno(),
                    parametros.getAcumulacion() != null ? parametros.getAcumulacion() : 0
                );
            }
            
            // Eliminar registros con saldo cero si se solicita
            if (parametros.getEliminarSaldosCero() != null && parametros.getEliminarSaldosCero()) {
                tempReportesDaoService.deleteBySaldosCeroIdEjecucion(idEjecucion);
            }
            
            // Obtener total de registros generados
            List<TempReportes> registros = tempReportesDaoService.selectBySecuencia(idEjecucion);
            
            RespuestaBalance respuesta = new RespuestaBalance(
                idEjecucion,
                registros.size(),
                "Balance generado exitosamente"
            );
            
            return Response.status(Response.Status.CREATED)
                .entity(respuesta)
                .type(MediaType.APPLICATION_JSON).build();
                
        } catch (Throwable e) {
            RespuestaBalance respuesta = new RespuestaBalance();
            respuesta.setExitoso(false);
            respuesta.setMensaje("Error al generar balance: " + e.getMessage());
            // C17: mes no cerrado para un reporte que se presenta (B17) = error del pedido, no del servidor
            boolean mesNoCerrado = e.getMessage() != null && e.getMessage().startsWith("REPORTE_MES_NO_CERRADO");
            return Response.status(mesNoCerrado ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR)
                .entity(respuesta)
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * C19b (D-031) — Salida en ARCHIVO de un reporte del motor con formato de archivo (RPRTFRAR = 1: TXT de la
     * Superintendencia, p. ej. el B17). Mismo cuerpo que /generarBalance (fechaFin = corte; preliminar = true para
     * generarlo sin el mes cerrado: B17M3968ddMMaaaa_PRELIMINAR.txt). Devuelve el .txt en ISO-8859-1, CRLF.
     */
    @POST
    @Path("generarArchivo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
    public Response generarArchivo(ParametrosBalance parametros) {
        try {
            // por codigoEstructura (B17) valida calculaRenglones; por alterno se rechaza antes el balance sobre el plan
            if (parametros.getCodigoAlterno() != null
                    && !tempReportesService.esReporteDeRenglones(parametros.getEmpresa(), parametros.getCodigoAlterno())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"mensaje\": \"El tipo de reporte no tiene salida en archivo (solo los de renglones propios con formato de archivo)\"}")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            ResultadoEstructura r = tempReportesService.calculaRenglones(parametros);
            if (r.getContenidoArchivo() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"mensaje\": \"El reporte " + r.getNombre() + " no tiene formato de archivo (RPRTFRAR)\"}")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.ok(ArchivoEstructura.bytes(r.getContenidoArchivo()))
                    .header("Content-Disposition", "attachment; filename=\"" + r.getNombreArchivo() + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .type("text/plain; charset=ISO-8859-1").build();
        } catch (Throwable e) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("mensaje", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(m).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ── C19b: estados financieros oficiales sobre el motor (antes /esfo) ─────────────────────────────

    /** Encabezado institucional: razon social, RUC, nombre del estado, periodo, PRELIMINAR/DEFINITIVO, firmas. */
    @POST
    @Path("estadoOficial/encabezado")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response estadoOficialEncabezado(SolicitudEstadoFinanciero s) {
        try {
            return Response.ok(estadoFinancieroOficialService.encabezado(s)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return errorOficial(e);
        }
    }

    /** Encabezado + filas (con Resultado del Ejercicio y comparativo) en JSON. */
    @POST
    @Path("estadoOficial/datos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response estadoOficialDatos(SolicitudEstadoFinanciero s) {
        try {
            return Response.ok(estadoFinancieroOficialService.datos(s)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return errorOficial(e);
        }
    }

    /** PDF (por defecto) o EXCEL del estado oficial, con el nombre del estado y su periodo. */
    @POST
    @Path("estadoOficial/imprimir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ "application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", MediaType.APPLICATION_JSON })
    public Response estadoOficialImprimir(SolicitudEstadoFinanciero s) {
        try {
            EncabezadoEstadoFinanciero e = estadoFinancieroOficialService.encabezado(s);
            byte[] archivo = estadoFinancieroOficialService.imprimir(s);
            boolean pdf = s.getFormato() == null || "PDF".equalsIgnoreCase(s.getFormato().trim());
            String nombre = e.getNombreArchivo() + (pdf ? ".pdf" : ".xlsx");
            return Response.ok(archivo)
                    .header("Content-Disposition", "attachment; filename=\"" + nombre + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .type(pdf ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .build();
        } catch (Throwable e) {
            return errorOficial(e);
        }
    }

    private Response errorOficial(Throwable e) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("mensaje", e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(m).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * GET - Consultar resultado de un balance generado
     * 
     * @param idEjecucion ID de la ejecución del balance
     * @return Lista de registros del balance
     */
    @GET
    @Path("/resultado/{idEjecucion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResultado(@PathParam("idEjecucion") Long idEjecucion) {
        System.out.println("LLEGA AL SERVICIO getResultado con idEjecucion: " + idEjecucion);
        try {
            List<TempReportes> registros = tempReportesDaoService.selectBySecuencia(idEjecucion);
            
            if (registros == null || registros.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("No se encontraron registros para la ejecución " + idEjecucion)
                    .type(MediaType.APPLICATION_JSON).build();
            }
            
            return Response.status(Response.Status.OK)
                .entity(registros)
                .type(MediaType.APPLICATION_JSON).build();
                
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al consultar resultado: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * DELETE - Eliminar registros de una ejecución de balance
     * 
     * @param idEjecucion ID de la ejecución a eliminar
     * @return Response sin contenido si es exitoso
     */
    @DELETE
    @Path("/resultado/{idEjecucion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteResultado(@PathParam("idEjecucion") Long idEjecucion) {
        System.out.println("LLEGA AL SERVICIO deleteResultado - TEMP_REPORTES con id: " + idEjecucion);
        try {
            tempReportesService.removeBySecuencia(idEjecucion);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar resultado: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

}
