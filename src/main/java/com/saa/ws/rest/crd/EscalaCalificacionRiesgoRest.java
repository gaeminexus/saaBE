package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.EscalaCalificacionRiesgoDaoService;
import com.saa.ejb.crd.service.EscalaCalificacionRiesgoService;
import com.saa.model.crd.EscalaCalificacionRiesgo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Calificaciones de una escala de riesgo (CRD.ESCR) — P22, SOLO LECTURA.
 *
 * <b>⛔ A propósito, y DISTINTO del molde de bandas (`/rest/bndp`): este REST NO expone
 * POST/PUT/DELETE.</b> Toda escritura pasa por {@code POST /rest/cfcr/guardarConfiguracion}
 * (o {@code /cerrarVigencia}), que valida el CONJUNTO antes de grabar una sola fila —ver el
 * JavaDoc de {@link EscalaCalificacionRiesgoService}.
 *
 * `/rest/bndp` sí expone CRUD suelto porque una banda derivada de acumular períodos no puede
 * quedar en un estado sin sentido con solo agregar o quitar una fila. <b>Una escala de
 * calificación de riesgo SÍ puede quedar inválida con un solo cambio</b> —un hueco entre el día
 * 30 y el día 35 deja una cuota sin calificar, y el G48 sale mal sin ningún error visible—, así
 * que exponer aquí un DELETE o un POST suelto reintroduciría por REST el mismo problema que P22
 * vino a resolver por SQL. <b>No "completar" este REST con esos métodos por simetría con bndp.</b>
 * Decisión del árbitro `omen-saa-1-arb`, 2026-09-07.
 *
 * Contrato para el frontend en docs/logica-negocio/crd/API-CALIFICACION-RIESGO.md.
 */
@Path("escr")
public class EscalaCalificacionRiesgoRest {

    @EJB
    private EscalaCalificacionRiesgoDaoService escalaCalificacionRiesgoDaoService;

    @EJB
    private EscalaCalificacionRiesgoService escalaCalificacionRiesgoService;

    @Context
    private UriInfo context;

    public EscalaCalificacionRiesgoRest() {
    }

    /**
     * GET - Todas las calificaciones, como entidades.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - ESCALA_CALIFICACION_RIESGO");
        try {
            List<EscalaCalificacionRiesgo> lista = escalaCalificacionRiesgoDaoService
                    .selectAll(NombreEntidadesCredito.ESCALA_CALIFICACION_RIESGO);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener calificaciones de riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Una calificación por código.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - ESCALA_CALIFICACION_RIESGO - id: " + id);
        try {
            EscalaCalificacionRiesgo entidad = escalaCalificacionRiesgoDaoService
                    .selectById(id, NombreEntidadesCredito.ESCALA_CALIFICACION_RIESGO);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("EscalaCalificacionRiesgo con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(entidad)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la calificacion de riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Calificaciones de una configuración, con la etiqueta del rango ya armada. Es la
     * lectura útil para la pantalla cuando ya tiene el código de la configuración.
     */
    @GET
    @Path("/getByConfiguracion/{idConfiguracion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByConfiguracion(@PathParam("idConfiguracion") Long idConfiguracion) {
        System.out.println("LLEGA AL SERVICIO GET BY CONFIGURACION - ESCALA_CALIFICACION_RIESGO"
                + " - configuracion: " + idConfiguracion);
        try {
            return Response.status(Response.Status.OK)
                    .entity(escalaCalificacionRiesgoService.selectDetalleByConfiguracion(idConfiguracion))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las calificaciones de la configuracion: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Búsqueda por criterios dinámicos.
     */
    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de ESCALA_CALIFICACION_RIESGO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(escalaCalificacionRiesgoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
