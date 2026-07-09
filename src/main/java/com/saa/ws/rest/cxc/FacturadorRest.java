package com.saa.ws.rest.cxc;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.service.FacturadorService;
import com.saa.model.cxc.Facturador;

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
 * Servicio REST para Facturador
 */
@Path("fcdr")
public class FacturadorRest {

    @EJB
    private FacturadorService facturadorService;
    
    @Context
    private UriInfo context;

    public FacturadorRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Facturador> lista = facturadorService.selectAll();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Facturadores: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Facturador entidad = facturadorService.selectById(id);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Facturador con ID " + id + " no encontrado")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Facturador: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getByNumDoc/{numDoc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByNumDoc(@PathParam("numDoc") String numDoc) {
        try {
            Facturador entidad = facturadorService.selectByNumDoc(numDoc);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Facturador con número de documento " + numDoc + " no encontrado")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener Facturador: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Facturador registro) {
        System.out.println("PUT Facturador");
        try {
            return Response.status(Response.Status.OK)
                .entity(facturadorService.saveSingle(registro))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar Facturador: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Facturador registro) {
        System.out.println("POST Facturador");
        try {
            return Response.status(Response.Status.CREATED)
                .entity(facturadorService.saveSingle(registro))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al crear Facturador: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("DELETE Facturador con id: " + id);
        try {
            List<Long> ids = new ArrayList<>();
            ids.add(id);
            facturadorService.remove(ids);
            return Response.status(Response.Status.OK)
                .entity("Facturador eliminado correctamente")
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar Facturador: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria Facturador");
        try {
            return Response.status(Response.Status.OK)
                .entity(facturadorService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error en selectByCriteria Facturador: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
