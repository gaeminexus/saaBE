package com.saa.ws.rest.cxc;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.service.EstablecimientoService;
import com.saa.model.cxc.Establecimiento;

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
 * Servicio REST para Establecimiento
 */
@Path("estb")
public class EstablecimientoRest {

    @EJB
    private EstablecimientoService establecimientoService;
    
    @Context
    private UriInfo context;

    public EstablecimientoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Establecimiento> lista = establecimientoService.selectAll();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Establecimientos: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Establecimiento entidad = establecimientoService.selectById(id);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Establecimiento con ID " + id + " no encontrado")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Establecimiento: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getByFacturador/{idFacturador}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByFacturador(@PathParam("idFacturador") Long idFacturador) {
        try {
            List<Establecimiento> lista = establecimientoService.selectByFacturador(idFacturador);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Establecimientos por Facturador: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getMatriz/{idFacturador}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMatriz(@PathParam("idFacturador") Long idFacturador) {
        try {
            Establecimiento entidad = establecimientoService.selectMatriz(idFacturador);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Establecimiento matriz no encontrado para el facturador " + idFacturador)
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Establecimiento matriz: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Establecimiento registro) {
        System.out.println("PUT Establecimiento");
        try {
            return Response.status(Response.Status.OK)
                .entity(establecimientoService.saveSingle(registro))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar Establecimiento: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Establecimiento registro) {
        System.out.println("POST Establecimiento");
        try {
            return Response.status(Response.Status.CREATED)
                .entity(establecimientoService.saveSingle(registro))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al crear Establecimiento: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("DELETE Establecimiento con id: " + id);
        try {
            List<Long> ids = new ArrayList<>();
            ids.add(id);
            establecimientoService.remove(ids);
            return Response.status(Response.Status.OK)
                .entity("Establecimiento eliminado correctamente")
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar Establecimiento: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria Establecimiento");
        try {
            return Response.status(Response.Status.OK)
                .entity(establecimientoService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error en selectByCriteria Establecimiento: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
