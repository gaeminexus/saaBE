package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.DireccionDaoService;
import com.saa.ejb.credito.service.DireccionService;
import com.saa.model.credito.Direccion;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("drcc")
public class DireccionRest {

    @EJB
    private DireccionDaoService direccionDaoService;

    @EJB
    private DireccionService direccionService;

    @Context
    private UriInfo context;

    public DireccionRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Direccion> lista = direccionDaoService.selectAll(NombreEntidadesCredito.DIRECCION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener direcciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Direccion direccion = direccionDaoService.selectById(id, NombreEntidadesCredito.DIRECCION);
            if (direccion == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Direccion con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(direccion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener dirección: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Path("/getByParent/{idParent}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByParent(@PathParam("idParent") Long idParent) {
        try {
            List<Direccion> lista = direccionDaoService.selectByParent(idParent);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener direcciones por parent: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Direccion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DRCC");
        try {
            Direccion resultado = direccionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar dirección: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Direccion registro) {
        System.out.println("LLEGA AL SERVICIO POST - DRCC");
        try {
            Direccion resultado = direccionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear dirección: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DRCC");
        Response respuesta;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(direccionService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DRCC");
        try {
            Direccion elimina = new Direccion();
            direccionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar dirección: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
