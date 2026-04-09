package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.service.AporteService;
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

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AporteRest() {
        // TODO Auto-generated constructor stub
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
