/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.service.GrupoConciliacionExtractoService;
import com.saa.model.tsr.GrupoConciliacionExtracto;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author GaemiSoft
 * <p>REST endpoint para GrupoConciliacionExtracto.
 * Path: /gcex</p>
 */
@Path("gcex")
public class GrupoConciliacionExtractoRest {

    @EJB
    private GrupoConciliacionExtractoService grupoConciliacionExtractoService;

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            GrupoConciliacionExtracto resultado = grupoConciliacionExtractoService.selectById(id);
            if (resultado == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("GrupoConciliacionExtracto con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener GrupoConciliacionExtracto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        try {
            List<GrupoConciliacionExtracto> lista = grupoConciliacionExtractoService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria GrupoConciliacionExtracto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
