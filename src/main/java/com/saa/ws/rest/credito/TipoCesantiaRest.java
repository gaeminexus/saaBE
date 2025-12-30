package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoCesantiaDaoService;
import com.saa.ejb.credito.service.TipoCesantiaService;
import com.saa.model.credito.TipoCesantia;
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

@Path("tpcs")
public class TipoCesantiaRest {

    @EJB
    private TipoCesantiaDaoService tipoCesantiaDaoService;

    @EJB
    private TipoCesantiaService tipoCesantiaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TipoCesantiaRest() {
    }

    /**
     * GET ALL
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TipoCesantia> lista = tipoCesantiaDaoService.selectAll(NombreEntidadesCredito.TIPO_CESANTIA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipos de cesantía: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TipoCesantia tipo = tipoCesantiaDaoService.selectById(id, NombreEntidadesCredito.TIPO_CESANTIA);
            if (tipo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TipoCesantia con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tipo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipo de cesantía: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TipoCesantia registro) {
        System.out.println("LLEGA AL SERVICIO PUT TipoCesantia");
        try {
            TipoCesantia resultado = tipoCesantiaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar tipo de cesantía: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TipoCesantia registro) {
        System.out.println("LLEGA AL SERVICIO POST TipoCesantia");
        try {
            TipoCesantia resultado = tipoCesantiaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear tipo de cesantía: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoCesantia");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoCesantiaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE TipoCesantia");
        try {
            TipoCesantia elimina = new TipoCesantia();
            tipoCesantiaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar tipo de cesantía: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
