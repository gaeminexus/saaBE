/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.DetalleExtractoBancarioDaoService;
import com.saa.ejb.tsr.service.DetalleExtractoBancarioService;
import com.saa.model.tsr.DetalleExtractoBancario;
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
 * <p>REST endpoint para DetalleExtractoBancario.
 * Path: /dexb</p>
 */
@Path("dexb")
public class DetalleExtractoBancarioRest {

    @EJB
    private DetalleExtractoBancarioDaoService detalleExtractoBancarioDaoService;

    @EJB
    private DetalleExtractoBancarioService detalleExtractoBancarioService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DetalleExtractoBancarioRest() {
    }

    /**
     * Obtiene todos los registros de DetalleExtractoBancario.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetalleExtractoBancario> lista = detalleExtractoBancarioDaoService
                    .selectAll(NombreEntidadesTesoreria.DETALLE_EXTRACTO_BANCARIO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener detalle de extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un DetalleExtractoBancario por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetalleExtractoBancario detalle = detalleExtractoBancarioDaoService
                    .selectById(id, NombreEntidadesTesoreria.DETALLE_EXTRACTO_BANCARIO);
            if (detalle == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("DetalleExtractoBancario con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(detalle).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener detalle de extracto bancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza un registro existente (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleExtractoBancario registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE_EXTRACTO_BANCARIO");
        try {
            DetalleExtractoBancario resultado = detalleExtractoBancarioService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar detalle de extracto bancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un nuevo registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleExtractoBancario registro) {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE_EXTRACTO_BANCARIO");
        try {
            DetalleExtractoBancario resultado = detalleExtractoBancarioService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear detalle de extracto bancario: " + e.getMessage())
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
        System.out.println("selectByCriteria de DETALLE_EXTRACTO_BANCARIO");
        try {
            List<DetalleExtractoBancario> lista = detalleExtractoBancarioService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria DetalleExtractoBancario: " + e.getMessage())
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
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE_EXTRACTO_BANCARIO");
        try {
            detalleExtractoBancarioService.remove(ids);
            return Response.status(Response.Status.OK)
                    .entity("Detalle de extractos bancarios eliminados correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar detalle de extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
