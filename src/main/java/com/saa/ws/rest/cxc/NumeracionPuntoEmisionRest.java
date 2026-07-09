package com.saa.ws.rest.cxc;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.service.NumeracionPuntoEmisionService;
import com.saa.model.cxc.NumeracionPuntoEmision;

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
 * Servicio REST para NumeracionPuntoEmision
 */
@Path("nxpe")
public class NumeracionPuntoEmisionRest {

    @EJB
    private NumeracionPuntoEmisionService numeracionService;
    
    @Context
    private UriInfo context;

    public NumeracionPuntoEmisionRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NumeracionPuntoEmision> lista = numeracionService.selectAll();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Numeraciones: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NumeracionPuntoEmision entidad = numeracionService.selectById(id);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Numeración con ID " + id + " no encontrada")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Numeración: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getByPuntoEmision/{idPuntoEmision}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByPuntoEmision(@PathParam("idPuntoEmision") Long idPuntoEmision) {
        try {
            List<NumeracionPuntoEmision> lista = numeracionService.selectByPuntoEmision(idPuntoEmision);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Numeraciones por Punto de Emisión: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getByPuntoEmisionTipo/{idPuntoEmision}/{tipoDoc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByPuntoEmisionTipo(@PathParam("idPuntoEmision") Long idPuntoEmision, 
                                          @PathParam("tipoDoc") String tipoDoc) {
        try {
            NumeracionPuntoEmision entidad = numeracionService.selectByPuntoEmisionTipo(idPuntoEmision, tipoDoc);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Numeración no encontrada para punto de emisión " + idPuntoEmision + " y tipo " + tipoDoc)
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Numeración: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getSiguienteNumero/{idPuntoEmision}/{tipoDoc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSiguienteNumero(@PathParam("idPuntoEmision") Long idPuntoEmision, 
                                       @PathParam("tipoDoc") String tipoDoc) {
        try {
            Long siguienteNumero = numeracionService.obtenerSiguienteNumero(idPuntoEmision, tipoDoc);
            return Response.status(Response.Status.OK).entity(siguienteNumero).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener siguiente número: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(NumeracionPuntoEmision registro) {
        System.out.println("PUT NumeracionPuntoEmision");
        try {
            return Response.status(Response.Status.OK)
                .entity(numeracionService.saveSingle(registro))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar Numeración: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(NumeracionPuntoEmision registro) {
        System.out.println("POST NumeracionPuntoEmision");
        try {
            return Response.status(Response.Status.CREATED)
                .entity(numeracionService.saveSingle(registro))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al crear Numeración: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("DELETE NumeracionPuntoEmision con id: " + id);
        try {
            List<Long> ids = new ArrayList<>();
            ids.add(id);
            numeracionService.remove(ids);
            return Response.status(Response.Status.OK)
                .entity("Numeración eliminada correctamente")
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar Numeración: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria NumeracionPuntoEmision");
        try {
            return Response.status(Response.Status.OK)
                .entity(numeracionService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error en selectByCriteria NumeracionPuntoEmision: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
