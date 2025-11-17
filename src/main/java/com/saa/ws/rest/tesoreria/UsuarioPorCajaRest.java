package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.UsuarioPorCajaDaoService;
import com.saa.ejb.tesoreria.service.UsuarioPorCajaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.UsuarioPorCaja;

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
    @Produces("application/json")
    public List<UsuarioPorCaja> getAll() throws Throwable {
        return usuarioPorCajaDaoService.selectAll(NombreEntidadesTesoreria.USUARIO_POR_CAJA);
    }

    /**
     * Recupera un registro de UsuarioPorCaja por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public UsuarioPorCaja getId(@PathParam("id") Long id) throws Throwable {
        return usuarioPorCajaDaoService.selectById(id, NombreEntidadesTesoreria.USUARIO_POR_CAJA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public UsuarioPorCaja put(UsuarioPorCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT USUARIO_POR_CAJA");
        return usuarioPorCajaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public UsuarioPorCaja post(UsuarioPorCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST USUARIO_POR_CAJA");
        return usuarioPorCajaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de UsuarioPorCaja por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<UsuarioPorCaja> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA USUARIO_POR_CAJA: " + test);
        return usuarioPorCajaDaoService.selectAll(NombreEntidadesTesoreria.USUARIO_POR_CAJA);
    }

    /**
     * Elimina un registro de UsuarioPorCaja por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE USUARIO_POR_CAJA");
        UsuarioPorCaja elimina = new UsuarioPorCaja();
        usuarioPorCajaDaoService.remove(elimina, id);
    }
}
