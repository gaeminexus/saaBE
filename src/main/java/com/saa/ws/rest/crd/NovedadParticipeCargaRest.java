package com.saa.ws.rest.crd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.NovedadParticipeCargaDaoService;
import com.saa.ejb.crd.service.NovedadParticipeCargaService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.NovedadParticipeCarga;

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

/**
 * REST API para NovedadParticipeCarga (FASE 2 - Procesamiento)
 * Gestiona las novedades encontradas durante el procesamiento de archivos Petrocomercial
 * 
 * Endpoints principales:
 * - /nvpc/getAll - Obtener todas las novedades
 * - /nvpc/getId/{id} - Obtener novedad por ID
 * - /nvpc/getByParticipe/{codigoParticipe} - Novedades de un partícipe específico
 * - /nvpc/getByCargaArchivo/{idCarga} - Novedades de una carga completa
 * - /nvpc/getByTipoNovedad/{tipoNovedad} - Novedades por tipo
 * - /nvpc/estadisticas/{idCarga} - Estadísticas de novedades de una carga
 */
@Path("nvpc")
public class NovedadParticipeCargaRest {

    @EJB
    private NovedadParticipeCargaDaoService novedadParticipeCargaDaoService;

    @EJB
    private NovedadParticipeCargaService novedadParticipeCargaService;

    @Context
    private UriInfo context;

    public NovedadParticipeCargaRest() {}

    /**
     * GET - Obtener todos los registros
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NovedadParticipeCarga> lista = novedadParticipeCargaDaoService.selectAll(NombreEntidadesCredito.NOVEDAD_PARTICIPE_CARGA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener novedades: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Obtener por ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NovedadParticipeCarga novedad = novedadParticipeCargaDaoService.selectById(id, NombreEntidadesCredito.NOVEDAD_PARTICIPE_CARGA);
            if (novedad == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("NovedadParticipeCarga con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(novedad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener novedad: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Obtener novedades por código de partícipe
     */
    @GET
    @Path("/getByParticipe/{codigoParticipe}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByParticipe(@PathParam("codigoParticipe") Long codigoParticipe) {
        try {
            List<NovedadParticipeCarga> lista = novedadParticipeCargaDaoService.selectByParticipe(codigoParticipe);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener novedades del partícipe: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Obtener novedades por carga de archivo (FASE 2)
     * Útil para ver todas las novedades detectadas en un procesamiento específico
     */
    @GET
    @Path("/getByCargaArchivo/{idCarga}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCargaArchivo(@PathParam("idCarga") Long idCarga) {
        try {
            List<DatosBusqueda> criterios = new java.util.ArrayList<>();
            DatosBusqueda criterio = new DatosBusqueda();
            criterio.setCampo("participeXCargaArchivo.detalleCargaArchivo.cargaArchivo.codigo");
            criterio.setTipoComparacion(com.saa.rubros.TipoComandosBusqueda.IGUAL);
            criterio.setTipoDato(com.saa.rubros.TipoDatosBusqueda.LONG);
            criterio.setValor(idCarga.toString());
            criterios.add(criterio);
            
            List<NovedadParticipeCarga> lista = novedadParticipeCargaService.selectByCriteria(criterios);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener novedades de la carga: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Obtener novedades por tipo de novedad (FASE 2)
     * Útil para analizar errores específicos del procesamiento
     * Tipos de novedad según ASPNovedadesCargaArchivo:
     * 2 - PARTICIPE_NO_ENCONTRADO
     * 3 - CODIGO_ROL_DUPLICADO
     * 4 - NOMBRE_ENTIDAD_DUPLICADO
     * 5 - CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE
     * 6 - SIN_DESCUENTOS
     * 7 - DESCUENTOS_INCOMPLETOS
     * 8 - PRESTAMO_NO_ENCONTRADO
     * 9 - MULTIPLES_PRESTAMOS_ACTIVOS
     * 10 - CUOTA_NO_ENCONTRADA
     * 11 - MONTO_INCONSISTENTE
     * 12 - PRODUCTO_NO_MAPEADO
     */
    @GET
    @Path("/getByTipoNovedad/{tipoNovedad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByTipoNovedad(@PathParam("tipoNovedad") Long tipoNovedad) {
        try {
            List<DatosBusqueda> criterios = new java.util.ArrayList<>();
            DatosBusqueda criterio = new DatosBusqueda();
            criterio.setCampo("tipoNovedad");
            criterio.setTipoComparacion(com.saa.rubros.TipoComandosBusqueda.IGUAL);
            criterio.setTipoDato(com.saa.rubros.TipoDatosBusqueda.LONG);
            criterio.setValor(tipoNovedad.toString());
            criterios.add(criterio);
            
            List<NovedadParticipeCarga> lista = novedadParticipeCargaService.selectByCriteria(criterios);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener novedades por tipo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Obtener estadísticas de novedades por carga (FASE 2)
     * Retorna un resumen con la cantidad de novedades por tipo
     */
    @GET
    @Path("/estadisticas/{idCarga}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEstadisticas(@PathParam("idCarga") Long idCarga) {
        try {
            // Obtener todas las novedades de la carga
            List<DatosBusqueda> criterios = new java.util.ArrayList<>();
            DatosBusqueda criterio = new DatosBusqueda();
            criterio.setCampo("participeXCargaArchivo.detalleCargaArchivo.cargaArchivo.codigo");
            criterio.setTipoComparacion(com.saa.rubros.TipoComandosBusqueda.IGUAL);
            criterio.setTipoDato(com.saa.rubros.TipoDatosBusqueda.LONG);
            criterio.setValor(idCarga.toString());
            criterios.add(criterio);
            
            List<NovedadParticipeCarga> novedades = novedadParticipeCargaService.selectByCriteria(criterios);
            
            // Crear mapa de estadísticas
            Map<String, Object> estadisticas = new HashMap<>();
            Map<Long, Integer> porTipo = new HashMap<>();
            
            int totalNovedades = novedades.size();
            double totalDiferencias = 0.0;
            
            for (NovedadParticipeCarga novedad : novedades) {
                Long tipo = novedad.getTipoNovedad();
                porTipo.put(tipo, porTipo.getOrDefault(tipo, 0) + 1);
                
                if (novedad.getMontoDiferencia() != null) {
                    totalDiferencias += novedad.getMontoDiferencia();
                }
            }
            
            estadisticas.put("totalNovedades", totalNovedades);
            estadisticas.put("novedadesPorTipo", porTipo);
            estadisticas.put("totalDiferenciasMonetarias", totalDiferencias);
            estadisticas.put("idCarga", idCarga);
            
            return Response.status(Response.Status.OK).entity(estadisticas).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener estadísticas: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT - Actualizar registro
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(NovedadParticipeCarga registro) {
        System.out.println("LLEGA AL SERVICIO PUT - NOVEDADPARTICIPECARGA");
        try {
            NovedadParticipeCarga resultado = novedadParticipeCargaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar novedad: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - Crear nuevo registro
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(NovedadParticipeCarga registro) {
        System.out.println("LLEGA AL SERVICIO POST - NOVEDADPARTICIPECARGA");
        try {
            NovedadParticipeCarga resultado = novedadParticipeCargaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear novedad: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - Buscar por criterios
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de NOVEDADPARTICIPECARGA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(novedadParticipeCargaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * DELETE - Eliminar registro
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - NOVEDADPARTICIPECARGA");
        try {
            NovedadParticipeCarga elimina = new NovedadParticipeCarga();
            novedadParticipeCargaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar novedad: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
