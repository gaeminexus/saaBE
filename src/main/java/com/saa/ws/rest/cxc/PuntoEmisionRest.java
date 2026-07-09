package com.saa.ws.rest.cxc;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.service.PuntoEmisionService;
import com.saa.model.cxc.PuntoEmision;

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
 * Servicio REST para PuntoEmision
 */
@Path("ptem")
public class PuntoEmisionRest {

    @EJB
    private PuntoEmisionService puntoEmisionService;
    
    @Context
    private UriInfo context;

    public PuntoEmisionRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PuntoEmision> lista = puntoEmisionService.selectAll();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Puntos de Emisión: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PuntoEmision entidad = puntoEmisionService.selectById(id);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Punto de Emisión con ID " + id + " no encontrado")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Punto de Emisión: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getByEstablecimiento/{idEstablecimiento}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByEstablecimiento(@PathParam("idEstablecimiento") Long idEstablecimiento) {
        try {
            List<PuntoEmision> lista = puntoEmisionService.selectByEstablecimiento(idEstablecimiento);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Puntos de Emisión por Establecimiento: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PuntoEmision registro) {
        System.out.println("PUT PuntoEmision");
        try {
            return Response.status(Response.Status.OK)
                .entity(puntoEmisionService.saveSingle(registro))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar Punto de Emisión: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PuntoEmision registro) {
        System.out.println("POST PuntoEmision");
        try {
            return Response.status(Response.Status.CREATED)
                .entity(puntoEmisionService.saveSingle(registro))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al crear Punto de Emisión: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("DELETE PuntoEmision con id: " + id);
        try {
            List<Long> ids = new ArrayList<>();
            ids.add(id);
            puntoEmisionService.remove(ids);
            return Response.status(Response.Status.OK)
                .entity("Punto de Emisión eliminado correctamente")
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar Punto de Emisión: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria PuntoEmision");
        try {
            return Response.status(Response.Status.OK)
                .entity(puntoEmisionService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error en selectByCriteria PuntoEmision: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
