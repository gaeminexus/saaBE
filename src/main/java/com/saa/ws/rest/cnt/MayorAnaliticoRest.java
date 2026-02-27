package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.DetalleMayorAnaliticoDaoService;
import com.saa.ejb.cnt.dao.MayorAnaliticoDaoService;
import com.saa.ejb.cnt.service.MayorAnaliticoService;
import com.saa.model.cnt.DetalleMayorAnalitico;
import com.saa.model.cnt.MayorAnalitico;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.ParametrosMayorAnalitico;
import com.saa.model.cnt.RespuestaMayorAnalitico;

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

@Path("myan")
public class MayorAnaliticoRest {

    @EJB
    private MayorAnaliticoDaoService mayorAnaliticoDaoService;

    @EJB
    private MayorAnaliticoService mayorAnaliticoService;
    
    @EJB
    private DetalleMayorAnaliticoDaoService detalleMayorAnaliticoDaoService;

    @Context
    private UriInfo context;

    public MayorAnaliticoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<MayorAnalitico> lista = mayorAnaliticoDaoService.selectAll(NombreEntidadesContabilidad.MAYOR_ANALITICO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            MayorAnalitico mayor = mayorAnaliticoDaoService.selectById(id, NombreEntidadesContabilidad.MAYOR_ANALITICO);
            if (mayor == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Mayor analítico con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(mayor).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(MayorAnalitico registro) {
        System.out.println("LLEGA AL SERVICIO PUT - MAYOR_ANALITICO");
        try {
            MayorAnalitico resultado = mayorAnaliticoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(MayorAnalitico registro) {
        System.out.println("LLEGA AL SERVICIO POST - MAYOR_ANALITICO");
        try {
            MayorAnalitico resultado = mayorAnaliticoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de MAYOR_ANALITICO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(mayorAnaliticoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - Generar reporte de Mayor Analítico (Libro Mayor)
     * Este endpoint genera el reporte detallado línea por línea de movimientos
     * 
     * @param parametros Parámetros para la generación del reporte
     * @return RespuestaMayorAnalitico con el secuencialReporte
     */
    @POST
    @Path("/generarReporte")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarReporte(ParametrosMayorAnalitico parametros) {
        System.out.println("LLEGA AL SERVICIO generarReporte - MAYOR_ANALITICO");
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
            
            // Generar el reporte
            Long secuencialReporte = mayorAnaliticoService.generaReporte(
                parametros.getEmpresa(),
                parametros.getFechaInicio(),
                parametros.getFechaFin(),
                parametros.getCuentaInicio(),
                parametros.getCuentaFin(),
                parametros.getTipoDistribucion() != null ? parametros.getTipoDistribucion() : 0,
                parametros.getCentroInicio(),
                parametros.getCentroFin(),
                parametros.getTipoAcumulacion() != null ? parametros.getTipoAcumulacion() : 0
            );
            
            // Obtener totales generados
            List<MayorAnalitico> cabeceras = mayorAnaliticoDaoService.selectBySecuencia(secuencialReporte);
            
            // Contar total de detalles
            int totalDetalles = 0;
            for (MayorAnalitico cabecera : cabeceras) {
                List<DetalleMayorAnalitico> detalles = detalleMayorAnaliticoDaoService.selectByIdMayorAnalitico(cabecera.getCodigo());
                totalDetalles += detalles.size();
            }
            
            RespuestaMayorAnalitico respuesta = new RespuestaMayorAnalitico(
                secuencialReporte,
                cabeceras.size(),
                totalDetalles,
                "Mayor analítico generado exitosamente"
            );
            
            return Response.status(Response.Status.CREATED)
                .entity(respuesta)
                .type(MediaType.APPLICATION_JSON).build();
                
        } catch (Throwable e) {
            RespuestaMayorAnalitico respuesta = new RespuestaMayorAnalitico();
            respuesta.setExitoso(false);
            respuesta.setMensaje("Error al generar mayor analítico: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(respuesta)
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Consultar cabeceras de un reporte de Mayor Analítico generado
     * 
     * @param secuencialReporte ID del secuencial del reporte
     * @return Lista de cabeceras (MayorAnalitico)
     */
    @GET
    @Path("/resultado/{secuencialReporte}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResultado(@PathParam("secuencialReporte") Long secuencialReporte) {
        System.out.println("LLEGA AL SERVICIO getResultado con secuencialReporte: " + secuencialReporte);
        try {
            List<MayorAnalitico> cabeceras = mayorAnaliticoDaoService.selectBySecuencia(secuencialReporte);
            
            if (cabeceras == null || cabeceras.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("No se encontraron registros para el secuencial " + secuencialReporte)
                    .type(MediaType.APPLICATION_JSON).build();
            }
            
            return Response.status(Response.Status.OK)
                .entity(cabeceras)
                .type(MediaType.APPLICATION_JSON).build();
                
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al consultar resultado: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Consultar detalle de movimientos de una cuenta específica
     * 
     * @param idMayorAnalitico ID de la cabecera (cuenta/centro)
     * @return Lista de movimientos detallados (DetalleMayorAnalitico)
     */
    @GET
    @Path("/detalle/{idMayorAnalitico}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDetalle(@PathParam("idMayorAnalitico") Long idMayorAnalitico) {
        System.out.println("LLEGA AL SERVICIO getDetalle con idMayorAnalitico: " + idMayorAnalitico);
        try {
            List<DetalleMayorAnalitico> detalles = detalleMayorAnaliticoDaoService.selectByIdMayorAnalitico(idMayorAnalitico);
            
            if (detalles == null || detalles.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("No se encontraron movimientos para la cuenta " + idMayorAnalitico)
                    .type(MediaType.APPLICATION_JSON).build();
            }
            
            return Response.status(Response.Status.OK)
                .entity(detalles)
                .type(MediaType.APPLICATION_JSON).build();
                
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al consultar detalle: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * DELETE - Eliminar un reporte completo de Mayor Analítico
     * Elimina la cabecera y todos sus detalles
     * 
     * @param secuencialReporte ID del secuencial del reporte
     * @return Response sin contenido si es exitoso
     */
    @DELETE
    @Path("/resultado/{secuencialReporte}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteResultado(@PathParam("secuencialReporte") Long secuencialReporte) {
        System.out.println("LLEGA AL SERVICIO deleteResultado - MAYOR_ANALITICO con secuencial: " + secuencialReporte);
        try {
            mayorAnaliticoService.eliminaConsultasAnteriores(secuencialReporte);
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
        System.out.println("LLEGA AL SERVICIO DELETE - MAYOR_ANALITICO");
        try {
            MayorAnalitico elimina = new MayorAnalitico();
            mayorAnaliticoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
