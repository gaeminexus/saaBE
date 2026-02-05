package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.UsuarioPorCajaDaoService;
import com.saa.ejb.tsr.service.UsuarioPorCajaService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.UsuarioPorCaja;

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

@Path("usxc")
public class UsuarioPorCajaRest {

    @EJB
    private UsuarioPorCajaDaoService usuarioPorCajaDaoService;

    @EJB
    private UsuarioPorCajaService usuarioPorCajaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public UsuarioPorCajaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de UsuarioPorCaja.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<UsuarioPorCaja> lista = usuarioPorCajaDaoService.selectAll(NombreEntidadesTesoreria.USUARIO_POR_CAJA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener usuarios por caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de UsuarioPorCaja por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            UsuarioPorCaja usuarioPorCaja = usuarioPorCajaDaoService.selectById(id, NombreEntidadesTesoreria.USUARIO_POR_CAJA);
            if (usuarioPorCaja == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("UsuarioPorCaja con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(usuarioPorCaja).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener usuario por caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(UsuarioPorCaja registro) {
        System.out.println("LLEGA AL SERVICIO PUT - USUARIO_POR_CAJA");
        try {
            UsuarioPorCaja resultado = usuarioPorCajaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar usuario por caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(UsuarioPorCaja registro) {
        System.out.println("LLEGA AL SERVICIO POST - USUARIO_POR_CAJA");
        try {
            UsuarioPorCaja resultado = usuarioPorCajaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear usuario por caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of UsuarioPorCajaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de USUARIO_POR_CAJA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(usuarioPorCajaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de UsuarioPorCaja por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - USUARIO_POR_CAJA");
        try {
            UsuarioPorCaja elimina = new UsuarioPorCaja();
            usuarioPorCajaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar usuario por caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
