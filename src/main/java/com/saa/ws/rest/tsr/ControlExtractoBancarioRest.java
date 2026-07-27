/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.ControlExtractoBancarioDaoService;
import com.saa.ejb.tsr.service.ControlExtractoBancarioService;
import com.saa.model.tsr.ControlExtractoBancario;
import com.saa.model.tsr.DetalleCumplimientoCuenta;
import com.saa.model.tsr.NombreEntidadesTesoreria;

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
 * @author GaemiSoft
 * <p>REST endpoint para ControlExtractoBancario.
 * Path: /cteb
 * Ademas del CRUD estandar, expone /generar y /recalcular: el tablero de
 * cumplimiento de extractos bancarios se dispara explicitamente desde el
 * frontend, no desde un job programado.</p>
 */
@Path("cteb")
public class ControlExtractoBancarioRest {

    @EJB
    private ControlExtractoBancarioDaoService controlExtractoBancarioDaoService;

    @EJB
    private ControlExtractoBancarioService controlExtractoBancarioService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ControlExtractoBancarioRest() {
    }

    /**
     * Obtiene todos los registros de ControlExtractoBancario.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ControlExtractoBancario> lista = controlExtractoBancarioDaoService
                    .selectAll(NombreEntidadesTesoreria.CONTROL_EXTRACTO_BANCARIO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener control de extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un ControlExtractoBancario por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ControlExtractoBancario control = controlExtractoBancarioDaoService
                    .selectById(id, NombreEntidadesTesoreria.CONTROL_EXTRACTO_BANCARIO);
            if (control == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("ControlExtractoBancario con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(control).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener control de extracto bancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Genera el registro de control para una empresa/periodo si todavia no existe.
     * Accion explicita disparada desde el frontend (ej. boton "Generar periodo").
     */
    @POST
    @Path("/generar/{idEmpresa}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generar(@PathParam("idEmpresa") Long idEmpresa, @PathParam("idPeriodo") Long idPeriodo) {
        System.out.println("LLEGA AL SERVICIO GENERAR - CONTROL_EXTRACTO_BANCARIO con idEmpresa: "
                + idEmpresa + ", idPeriodo: " + idPeriodo);
        try {
            ControlExtractoBancario resultado = controlExtractoBancarioService.generarPeriodo(idEmpresa, idPeriodo);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al generar control de extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recalcula cuantasCargadas y cuantasConciliadas de un periodo ya generado.
     * Accion explicita disparada desde el frontend (ej. boton "Actualizar" del tablero).
     */
    @POST
    @Path("/recalcular/{idEmpresa}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response recalcular(@PathParam("idEmpresa") Long idEmpresa, @PathParam("idPeriodo") Long idPeriodo) {
        System.out.println("LLEGA AL SERVICIO RECALCULAR - CONTROL_EXTRACTO_BANCARIO con idEmpresa: "
                + idEmpresa + ", idPeriodo: " + idPeriodo);
        try {
            ControlExtractoBancario resultado = controlExtractoBancarioService.recalcularPeriodo(idEmpresa, idPeriodo);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al recalcular control de extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Detalle por cuenta bancaria (drill-down) para un empresa/periodo: cuales
     * cuentas ya cargaron su extracto y cuales ya estan conciliadas.
     */
    @GET
    @Path("/detalleCuentas/{idEmpresa}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response detalleCuentas(@PathParam("idEmpresa") Long idEmpresa, @PathParam("idPeriodo") Long idPeriodo) {
        try {
            List<DetalleCumplimientoCuenta> resultado = controlExtractoBancarioService
                    .detalleCuentas(idEmpresa, idPeriodo);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener detalle de cuentas del tablero de cumplimiento: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza un registro existente (PUT) - ej. para editar observaciones manualmente.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ControlExtractoBancario registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CONTROL_EXTRACTO_BANCARIO");
        try {
            ControlExtractoBancario resultado = controlExtractoBancarioService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar control de extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Búsqueda por criterios dinámicos.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CONTROL_EXTRACTO_BANCARIO");
        try {
            List<ControlExtractoBancario> lista = controlExtractoBancarioService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria ControlExtractoBancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina registros por lista de IDs.
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(List<Long> ids) {
        System.out.println("LLEGA AL SERVICIO DELETE - CONTROL_EXTRACTO_BANCARIO");
        try {
            controlExtractoBancarioService.remove(ids);
            return Response.status(Response.Status.OK)
                    .entity("Registros de control eliminados correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar control de extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
