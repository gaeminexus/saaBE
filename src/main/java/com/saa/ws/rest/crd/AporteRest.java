package com.saa.ws.rest.crd;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.dto.ResultadoRegistroAporte;
import com.saa.ejb.crd.service.dto.SaldoTipoAporte;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAporte;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.NombreEntidadesCredito;

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

@Path("aprt")
public class AporteRest {

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private AporteService aporteService;

    @EJB
    private SaldoAporteService saldoAporteService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AporteRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Saldos de aportes de una entidad, agrupados por tipo de aporte vigente.
     *
     * El cálculo lo hace la BD con una query agregada. El frontend debe usar ESTE endpoint
     * para los estados de cuenta de aportes; {@code GET /aprt/getAll} queda DEPRECADO para ese
     * uso porque descarga las ~980.000 filas de CRD.APRT y provoca OutOfMemoryError.
     *
     * @param idEntidad Código de la entidad (partícipe)
     * @return 200 con la lista de saldos; una lista vacía NO es error
     */
    @GET
    @Path("/saldosPorEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saldosPorEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO SALDOS POR ENTIDAD - Entidad: " + idEntidad);

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        try {
            if (idEntidad == null || idEntidad <= 0) {
                cuerpo.put("exito", Boolean.FALSE);
                cuerpo.put("mensaje", "Debe indicar una entidad válida");
                cuerpo.put("error", "PARAMETRO_INVALIDO");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
            }

            List<SaldoTipoAporte> saldos = saldoAporteService.saldosPorEntidad(idEntidad);

            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("resultado", saldos);
            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al obtener saldos de aportes: " + e.getMessage());
            e.printStackTrace();
            cuerpo.put("exito", Boolean.FALSE);
            cuerpo.put("mensaje", "Error al obtener los saldos de aportes: " + e.getMessage());
            cuerpo.put("error", "ERROR_INTERNO");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Estado de cuenta de aportes por devengo (§4.2 del plan de devengo de aportes). Contrato
     * CONGELADO — el frontend ya está construido contra él.
     *
     * Una entidad sin movimientos en el rango devuelve 200 con {@code periodos} vacío: NO es
     * un error (pedido 1, "sin aportes" es un resultado válido).
     *
     * @param idEntidad Código de la entidad (partícipe)
     * @param desde     Mes de inicio, formato {@code yyyy-MM} (inclusive)
     * @param hasta     Mes de fin, formato {@code yyyy-MM} (inclusive)
     * @return 200 con el estado de cuenta; 400 si los parámetros son inválidos; 404 si la
     *         entidad no existe; 500 ante cualquier otro error
     */
    @GET
    @Path("/estadoCuenta/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response estadoCuenta(
            @PathParam("idEntidad") Long idEntidad,
            @jakarta.ws.rs.QueryParam("desde") String desde,
            @jakarta.ws.rs.QueryParam("hasta") String hasta) {
        System.out.println("LLEGA AL SERVICIO ESTADO DE CUENTA APORTES - Entidad: " + idEntidad
            + " - Desde: " + desde + " - Hasta: " + hasta);
        try {
            if (idEntidad == null || idEntidad <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Debe indicar una entidad válida").type(MediaType.APPLICATION_JSON).build();
            }
            if (desde == null || desde.trim().isEmpty() || hasta == null || hasta.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Debe indicar desde y hasta (formato yyyy-MM)")
                    .type(MediaType.APPLICATION_JSON).build();
            }

            java.time.LocalDate fechaDesde;
            java.time.LocalDate fechaHasta;
            try {
                fechaDesde = java.time.YearMonth.parse(desde.trim()).atDay(1);
                fechaHasta = java.time.YearMonth.parse(hasta.trim()).atDay(1);
            } catch (java.time.format.DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("desde/hasta deben tener formato yyyy-MM")
                    .type(MediaType.APPLICATION_JSON).build();
            }

            com.saa.ejb.crd.service.dto.EstadoCuentaAportesDTO resultado =
                aporteService.estadoCuenta(idEntidad, fechaDesde, fechaHasta);
            return Response.status(Response.Status.OK)
                .entity(resultado).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
            if (e instanceof com.saa.basico.util.IncomeException
                    && mensaje.startsWith(AporteService.ERR_ENTIDAD_NO_ENCONTRADA)) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(mensaje).type(MediaType.APPLICATION_JSON).build();
            }
            if (e instanceof com.saa.basico.util.IncomeException) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(mensaje).type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener el estado de cuenta: " + mensaje)
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Pago de aportes en ventanilla: genera para el partícipe un aporte YA PAGADO del tipo
     * indicado y sube su saldo disponible de inmediato.
     *
     * La fila nace con saldo 0 y estado PAGADA(4), de modo que el FIFO del proceso Petro
     * nunca la toma como deuda por cobrar.
     *
     * @param solicitud { idEntidad, idTipoAporte, valor, usuario, observacion, fechaTransaccion }
     * @return 201 con el ResultadoRegistroAporte; 400/404/422/500 según el fallo
     */
    @POST
    @Path("/registrarAporte")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarAporte(SolicitudRegistroAporte solicitud) {
        System.out.println("LLEGA AL SERVICIO REGISTRAR APORTE - Entidad: "
            + (solicitud != null ? solicitud.getIdEntidad() : null)
            + " - Tipo: " + (solicitud != null ? solicitud.getIdTipoAporte() : null)
            + " - Valor: " + (solicitud != null ? solicitud.getValor() : null));

        if (solicitud == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe enviar el cuerpo de la solicitud", null);
        }
        if (solicitud.getIdEntidad() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el partícipe (idEntidad)", null);
        }
        if (solicitud.getIdTipoAporte() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el tipo de aporte (idTipoAporte)", null);
        }
        if (solicitud.getValor() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el valor del aporte", null);
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el usuario que registra el aporte", null);
        }

        try {
            ResultadoRegistroAporte resultado = aporteService.registrarAporte(solicitud);

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", "APLICACION");
            cuerpo.put("mensaje", "Aporte registrado por $" + resultado.getValor()
                + " en " + resultado.getNombreTipoAporte()
                + ". Saldo del tipo: $" + resultado.getSaldoTipoAporte());
            cuerpo.put("resultado", resultado);

            return Response.status(Response.Status.CREATED)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al registrar el aporte: " + e.getMessage());
            e.printStackTrace();

            String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
            String codigo = mensaje.contains(":")
                ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

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
    }

    // ------------------------------------------------------------------------
    // Sobre de respuesta y mapeo de errores (mismo convenio que PrestamoRest, §8)
    // ------------------------------------------------------------------------

    /** 422 UNPROCESSABLE ENTITY - no existe en el enum Response.Status de Jakarta REST */
    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    private static final List<String> CODIGOS_400 = Arrays.asList("PARAMETRO_INVALIDO");

    private static final List<String> CODIGOS_404 = Arrays.asList("ENTIDAD_NO_ENCONTRADA");

    private Response respuestaFallo(int status, String mensaje, String codigo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("etapa", "VALIDACION");
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("error", codigo != null && !codigo.isEmpty() ? codigo : mensaje);
        return Response.status(status)
                .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * Retrieves representation of an instance of AporteRest
     *
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Aporte> lista = aporteDaoService.selectAll(NombreEntidadesCredito.APORTE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener aportes: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Aporte aporte = aporteDaoService.selectById(id, NombreEntidadesCredito.APORTE);
            if (aporte == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Aporte con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(aporte).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Aporte registro) {
        System.out.println("LLEGA AL SERVICIO PUT - APORTE");
        try {
            Aporte resultado = aporteService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Aporte registro) {
        System.out.println("LLEGA AL SERVICIO POST - APORTE");
        try {
            Aporte resultado = aporteService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of AporteRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de APORTE");
        Response respuesta = null;
    	try {
    		respuesta = Response.status(Response.Status.OK).entity(aporteService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    	return respuesta;
    }

    /**
     * DELETE method for deleting an instance of AporteRest
     * 
     * @param id identifier for the resource
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - APORTE");
        try {
            Aporte elimina = new Aporte();
            aporteDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene KPIs globales de aportes para el dashboard
     * 
     * @param fechaDesde Fecha inicial (opcional, formato: yyyy-MM-dd)
     * @param fechaHasta Fecha final (opcional, formato: yyyy-MM-dd)
     * @param estadoAporte Estado del aporte (opcional)
     * @return JSON con los KPIs calculados
     */
    @GET
    @Path("/kpis-globales")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKpisGlobales(
            @jakarta.ws.rs.QueryParam("fechaDesde") String fechaDesde,
            @jakarta.ws.rs.QueryParam("fechaHasta") String fechaHasta,
            @jakarta.ws.rs.QueryParam("estadoAporte") Long estadoAporte) {
        try {
            java.time.LocalDateTime fechaDesdeDate = null;
            java.time.LocalDateTime fechaHastaDate = null;
            
            // Parseo de fechas si vienen como parámetros
            if (fechaDesde != null && !fechaDesde.isEmpty()) {
                fechaDesdeDate = java.time.LocalDate.parse(fechaDesde).atStartOfDay();
            }
            if (fechaHasta != null && !fechaHasta.isEmpty()) {
                fechaHastaDate = java.time.LocalDate.parse(fechaHasta).atStartOfDay();
            }
            
            com.saa.model.crd.dto.AporteKpiDTO kpis = aporteDaoService.selectKpisGlobales(
                fechaDesdeDate, 
                fechaHastaDate, 
                estadoAporte
            );
            
            return Response.status(Response.Status.OK)
                    .entity(kpis)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener KPIs globales: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene resumen de aportes por tipo (para gráfico de dona y tarjetas)
     * 
     * @param fechaDesde Fecha inicial (opcional, formato: yyyy-MM-dd)
     * @param fechaHasta Fecha final (opcional, formato: yyyy-MM-dd)
     * @param estadoAporte Estado del aporte (opcional)
     * @return JSON con lista de tipos y sus porcentajes
     */
    @GET
    @Path("/resumen-por-tipo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResumenPorTipo(
            @jakarta.ws.rs.QueryParam("fechaDesde") String fechaDesde,
            @jakarta.ws.rs.QueryParam("fechaHasta") String fechaHasta,
            @jakarta.ws.rs.QueryParam("estadoAporte") Long estadoAporte) {
        try {
            java.time.LocalDateTime fechaDesdeDate = null;
            java.time.LocalDateTime fechaHastaDate = null;
            
            if (fechaDesde != null && !fechaDesde.isEmpty()) {
                fechaDesdeDate = java.time.LocalDate.parse(fechaDesde).atStartOfDay();
            }
            if (fechaHasta != null && !fechaHasta.isEmpty()) {
                fechaHastaDate = java.time.LocalDate.parse(fechaHasta).atStartOfDay();
            }
            
            java.util.List<com.saa.model.crd.dto.AporteResumenTipoDTO> resumen = 
                aporteDaoService.selectResumenPorTipo(fechaDesdeDate, fechaHastaDate, estadoAporte);
            
            return Response.status(Response.Status.OK)
                    .entity(resumen)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener resumen por tipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene top N entidades con mayor impacto por tipo de aporte
     * 
     * @param fechaDesde Fecha inicial (opcional, formato: yyyy-MM-dd)
     * @param fechaHasta Fecha final (opcional, formato: yyyy-MM-dd)
     * @param estadoAporte Estado del aporte (opcional)
     * @param tipoAporteId Tipo de aporte específico (opcional)
     * @param topN Cantidad de entidades a retornar (por defecto 10)
     * @return JSON con lista de top entidades
     */
    @GET
    @Path("/top-entidades")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopEntidades(
            @jakarta.ws.rs.QueryParam("fechaDesde") String fechaDesde,
            @jakarta.ws.rs.QueryParam("fechaHasta") String fechaHasta,
            @jakarta.ws.rs.QueryParam("estadoAporte") Long estadoAporte,
            @jakarta.ws.rs.QueryParam("tipoAporteId") Long tipoAporteId,
            @jakarta.ws.rs.QueryParam("topN") Integer topN) {
        try {
            java.time.LocalDateTime fechaDesdeDate = null;
            java.time.LocalDateTime fechaHastaDate = null;
            
            if (fechaDesde != null && !fechaDesde.isEmpty()) {
                fechaDesdeDate = java.time.LocalDate.parse(fechaDesde).atStartOfDay();
            }
            if (fechaHasta != null && !fechaHasta.isEmpty()) {
                fechaHastaDate = java.time.LocalDate.parse(fechaHasta).atStartOfDay();
            }
            
            // Valor por defecto para topN
            Integer limit = (topN != null && topN > 0) ? topN : 10;
            
            java.util.List<com.saa.model.crd.dto.AporteTopEntidadDTO> topEntidades = 
                aporteDaoService.selectTopEntidades(fechaDesdeDate, fechaHastaDate, estadoAporte, tipoAporteId, limit);
            
            return Response.status(Response.Status.OK)
                    .entity(topEntidades)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener top entidades: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene top N movimientos individuales más grandes
     * 
     * @param fechaDesde Fecha inicial (opcional, formato: yyyy-MM-dd)
     * @param fechaHasta Fecha final (opcional, formato: yyyy-MM-dd)
     * @param estadoAporte Estado del aporte (opcional)
     * @param tipoAporteId Tipo de aporte específico (opcional)
     * @param topN Cantidad de movimientos a retornar (por defecto 10)
     * @return JSON con lista de top movimientos
     */
    @GET
    @Path("/top-movimientos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopMovimientos(
            @jakarta.ws.rs.QueryParam("fechaDesde") String fechaDesde,
            @jakarta.ws.rs.QueryParam("fechaHasta") String fechaHasta,
            @jakarta.ws.rs.QueryParam("estadoAporte") Long estadoAporte,
            @jakarta.ws.rs.QueryParam("tipoAporteId") Long tipoAporteId,
            @jakarta.ws.rs.QueryParam("topN") Integer topN) {
        try {
            java.time.LocalDateTime fechaDesdeDate = null;
            java.time.LocalDateTime fechaHastaDate = null;
            
            if (fechaDesde != null && !fechaDesde.isEmpty()) {
                fechaDesdeDate = java.time.LocalDate.parse(fechaDesde).atStartOfDay();
            }
            if (fechaHasta != null && !fechaHasta.isEmpty()) {
                fechaHastaDate = java.time.LocalDate.parse(fechaHasta).atStartOfDay();
            }
            
            // Valor por defecto para topN
            Integer limit = (topN != null && topN > 0) ? topN : 10;
            
            java.util.List<com.saa.model.crd.dto.AporteTopMovimientoDTO> topMovimientos = 
                aporteDaoService.selectTopMovimientos(fechaDesdeDate, fechaHastaDate, estadoAporte, tipoAporteId, limit);
            
            return Response.status(Response.Status.OK)
                    .entity(topMovimientos)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener top movimientos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

}
