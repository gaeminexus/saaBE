package com.saa.ws.rest.crd;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.AfectacionValoresParticipeCargaDaoService;
import com.saa.ejb.crd.service.AfectacionValoresParticipeCargaService;
import com.saa.ejb.crd.serviceImpl.AfectacionValoresParticipeCargaServiceImpl;
import com.saa.model.crd.AfectacionValoresParticipeCarga;
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

/**
 * REST API para AfectacionValoresParticipeCarga (FASE 2 - Resolución de Novedades)
 * 
 * Esta API permite al frontend registrar las decisiones del usuario sobre
 * cómo afectar manualmente los valores cuando hay novedades en el procesamiento.
 * 
 * FLUJO:
 * 1. Frontend obtiene novedades pendientes (desde NovedadParticipeCargaRest)
 * 2. Usuario indica manualmente: préstamo, cuota y valores a afectar
 * 3. Frontend envía la afectación a través de este REST (POST)
 * 4. Sistema calcula automáticamente las diferencias
 * 5. Al reprocesar, el sistema usa esta tabla como referencia
 * 
 * Endpoints principales:
 * - POST /avpc - Crear nueva afectación (desde frontend)
 * - GET /avpc/getByNovedad/{idNovedad} - Obtener afectaciones de una novedad
 * - GET /avpc/getByPrestamo/{idPrestamo} - Afectaciones de un préstamo
 * - GET /avpc/getByCuota/{idCuota} - Afectaciones de una cuota
 * - PUT /avpc - Actualizar afectación existente
 * - DELETE /avpc/{id} - Eliminar afectación
 * 
 * @author Sistema SAA
 * @since 2026-03-25
 */
@Path("avpc")
public class AfectacionValoresParticipeCargaRest {

    @EJB
    private AfectacionValoresParticipeCargaDaoService afectacionDaoService;

    @EJB
    private AfectacionValoresParticipeCargaService afectacionService;

    @Context
    private UriInfo context;

    public AfectacionValoresParticipeCargaRest() {}

    /**
     * GET - Obtener todas las afectaciones
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AfectacionValoresParticipeCarga> lista = afectacionDaoService.selectAll(NombreEntidadesCredito.AFECTACION_VALORES_PARTICIPE_CARGA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener afectaciones: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
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
            AfectacionValoresParticipeCarga afectacion = afectacionDaoService.selectById(id, NombreEntidadesCredito.AFECTACION_VALORES_PARTICIPE_CARGA);
            if (afectacion == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("AfectacionValoresParticipeCarga con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(afectacion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener afectación: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Obtener afectaciones por novedad
     * Este endpoint es CRÍTICO para que el frontend sepa si una novedad
     * ya tiene afectaciones manuales registradas
     */
    @GET
    @Path("/getByNovedad/{idNovedad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByNovedad(@PathParam("idNovedad") Long idNovedad) {
        try {
            AfectacionValoresParticipeCargaServiceImpl serviceImpl = (AfectacionValoresParticipeCargaServiceImpl) afectacionService;
            List<AfectacionValoresParticipeCarga> lista = serviceImpl.selectByNovedad(idNovedad);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener afectaciones de la novedad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Obtener afectaciones por préstamo
     * Útil para ver todas las afectaciones manuales de un préstamo específico
     */
    @GET
    @Path("/getByPrestamo/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByPrestamo(@PathParam("idPrestamo") Long idPrestamo) {
        try {
            AfectacionValoresParticipeCargaServiceImpl serviceImpl = (AfectacionValoresParticipeCargaServiceImpl) afectacionService;
            List<AfectacionValoresParticipeCarga> lista = serviceImpl.selectByPrestamo(idPrestamo);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener afectaciones del préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Obtener afectaciones por cuota
     * Útil para ver si una cuota específica tiene afectaciones manuales
     */
    @GET
    @Path("/getByCuota/{idCuota}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCuota(@PathParam("idCuota") Long idCuota) {
        try {
            AfectacionValoresParticipeCargaServiceImpl serviceImpl = (AfectacionValoresParticipeCargaServiceImpl) afectacionService;
            List<AfectacionValoresParticipeCarga> lista = serviceImpl.selectByCuota(idCuota);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener afectaciones de la cuota: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - Crear nueva afectación
     * Este es el endpoint principal que usa el frontend para registrar
     * las decisiones manuales del usuario sobre cómo afectar valores
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(AfectacionValoresParticipeCarga registro) {
        System.out.println("========================================");
        System.out.println("LLEGA AL SERVICIO POST - AFECTACIONVALORESPARTICIPECARGA");
        System.out.println("Novedad: " + (registro.getNovedadParticipeCarga() != null ? registro.getNovedadParticipeCarga().getCodigo() : null));
        System.out.println("Préstamo: " + (registro.getPrestamo() != null ? registro.getPrestamo().getCodigo() : null));
        System.out.println("Cuota: " + (registro.getDetallePrestamo() != null ? registro.getDetallePrestamo().getCodigo() : null));
        System.out.println("Valor a afectar: " + registro.getValorAfectar());
        System.out.println("========================================");
        
        try {
            // Establecer fecha de afectación si no viene
            if (registro.getFechaAfectacion() == null) {
                registro.setFechaAfectacion(LocalDate.now());
            }
            
            // Calcular automáticamente las diferencias
            registro.calcularDiferencias();
            
            // Guardar
            AfectacionValoresParticipeCarga resultado = afectacionService.saveSingle(registro);
            
            System.out.println("✅ Afectación creada exitosamente con código: " + resultado.getCodigo());
            
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            System.err.println("❌ Error al crear afectación: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear afectación: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT - Actualizar afectación existente
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(AfectacionValoresParticipeCarga registro) {
        System.out.println("LLEGA AL SERVICIO PUT - AFECTACIONVALORESPARTICIPECARGA");
        try {
            // Recalcular diferencias
            registro.calcularDiferencias();
            
            AfectacionValoresParticipeCarga resultado = afectacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar afectación: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - Buscar por criterios
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de AFECTACIONVALORESPARTICIPECARGA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(afectacionService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * DELETE - Eliminar afectación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - AFECTACIONVALORESPARTICIPECARGA");
        try {
            AfectacionValoresParticipeCarga elimina = new AfectacionValoresParticipeCarga();
            afectacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar afectación: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - Crear múltiples afectaciones en lote
     * Útil cuando el usuario necesita distribuir valores entre varias cuotas
     */
    @POST
    @Path("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postBatch(List<AfectacionValoresParticipeCarga> afectaciones) {
        System.out.println("========================================");
        System.out.println("LLEGA AL SERVICIO POST BATCH - AFECTACIONVALORESPARTICIPECARGA");
        System.out.println("Cantidad de afectaciones: " + afectaciones.size());
        System.out.println("========================================");
        
        try {
            List<AfectacionValoresParticipeCarga> resultados = new java.util.ArrayList<>();
            
            for (AfectacionValoresParticipeCarga afectacion : afectaciones) {
                // Establecer fecha si no viene
                if (afectacion.getFechaAfectacion() == null) {
                    afectacion.setFechaAfectacion(LocalDate.now());
                }
                
                // Calcular diferencias
                afectacion.calcularDiferencias();
                
                // Guardar
                AfectacionValoresParticipeCarga resultado = afectacionService.saveSingle(afectacion);
                resultados.add(resultado);
            }
            
            System.out.println("✅ " + resultados.size() + " afectaciones creadas exitosamente");
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("totalCreados", resultados.size());
            respuesta.put("afectaciones", resultados);
            
            return Response.status(Response.Status.CREATED).entity(respuesta).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            System.err.println("❌ Error al crear afectaciones en lote: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear afectaciones en lote: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET - Validar si una novedad ya tiene afectaciones registradas
     * Retorna información resumida para el frontend
     */
    @GET
    @Path("/validar/{idNovedad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validarAfectaciones(@PathParam("idNovedad") Long idNovedad) {
        try {
            AfectacionValoresParticipeCargaServiceImpl serviceImpl = (AfectacionValoresParticipeCargaServiceImpl) afectacionService;
            List<AfectacionValoresParticipeCarga> afectaciones = serviceImpl.selectByNovedad(idNovedad);
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("tieneAfectaciones", !afectaciones.isEmpty());
            respuesta.put("cantidadAfectaciones", afectaciones.size());
            respuesta.put("idNovedad", idNovedad);
            
            if (!afectaciones.isEmpty()) {
                double totalAfectado = afectaciones.stream()
                        .mapToDouble(a -> a.getValorAfectar() != null ? a.getValorAfectar() : 0.0)
                        .sum();
                respuesta.put("totalValorAfectado", totalAfectado);
            }
            
            return Response.status(Response.Status.OK).entity(respuesta).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al validar afectaciones: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
