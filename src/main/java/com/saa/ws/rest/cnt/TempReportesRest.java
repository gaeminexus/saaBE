package com.saa.ws.rest.cnt;


import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.TempReportesDaoService;
import com.saa.ejb.cnt.service.TempReportesService;
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
     * PUT method for updating or creating an instance of TempReportesRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TempReportes registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_REPORTES");
        try {
            TempReportes resultado = tempReportesService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar reporte temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempReportes registro) {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_REPORTES");
        try {
            TempReportes resultado = tempReportesService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear reporte temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(respuesta)
                .type(MediaType.APPLICATION_JSON).build();
        }
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

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_REPORTES");
        try {
            TempReportes elimina = new TempReportes();
            tempReportesDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar reporte temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
