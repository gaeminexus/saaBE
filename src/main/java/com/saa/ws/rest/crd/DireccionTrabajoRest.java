package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.DireccionTrabajoDaoService;
import com.saa.ejb.crd.service.DireccionTrabajoService;
import com.saa.model.crd.DireccionTrabajo;
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

@Path("drtr")
public class DireccionTrabajoRest {

    @EJB
    private DireccionTrabajoDaoService direccionTrabajoDaoService;

    @EJB
    private DireccionTrabajoService direccionTrabajoService;

    @Context
    private UriInfo context;

    public DireccionTrabajoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DireccionTrabajo> lista = direccionTrabajoDaoService.selectAll(NombreEntidadesCredito.DIRECCION_TRABAJO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener direcciones de trabajo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DireccionTrabajo direccionTrabajo = direccionTrabajoDaoService.selectById(id, NombreEntidadesCredito.DIRECCION_TRABAJO);
            if (direccionTrabajo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("DireccionTrabajo con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(direccionTrabajo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener dirección de trabajo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DireccionTrabajo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DIRECCION TRABAJO");
        try {
            DireccionTrabajo resultado = direccionTrabajoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar dirección de trabajo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DireccionTrabajo registro) {
        System.out.println("LLEGA AL SERVICIO POST - DIRECCION TRABAJO");
        try {
            DireccionTrabajo resultado = direccionTrabajoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear dirección de trabajo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DIRECCION TRABAJO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(direccionTrabajoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - DIRECCION TRABAJO");
        try {
            DireccionTrabajo elimina = new DireccionTrabajo();
            direccionTrabajoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar dirección de trabajo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
