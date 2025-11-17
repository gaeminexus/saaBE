package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.GrupoCajaDaoService;
import com.saa.ejb.tesoreria.service.GrupoCajaService;
import com.saa.model.tesoreria.GrupoCaja;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

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

@Path("cjin")
public class GrupoCajaRest {

    @EJB
    private GrupoCajaDaoService grupoCajaDaoService;

    @EJB
    private GrupoCajaService grupoCajaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public GrupoCajaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de GrupoCaja.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<GrupoCaja> getAll() throws Throwable {
        return grupoCajaDaoService.selectAll(NombreEntidadesTesoreria.GRUPO_CAJA);
    }

    /**
     * Recupera un registro de GrupoCaja por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public GrupoCaja getId(@PathParam("id") Long id) throws Throwable {
        return grupoCajaDaoService.selectById(id, NombreEntidadesTesoreria.GRUPO_CAJA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public GrupoCaja put(GrupoCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT GRUPO CAJA");
        return grupoCajaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public GrupoCaja post(GrupoCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST GRUPO CAJA");
        return grupoCajaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de GrupoCaja por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<GrupoCaja> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA GRUPO CAJA: " + test);
        return grupoCajaDaoService.selectAll(NombreEntidadesTesoreria.GRUPO_CAJA);
    }

    /**
     * Elimina un registro de GrupoCaja por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE GRUPO CAJA");
        GrupoCaja elimina = new GrupoCaja();
        grupoCajaDaoService.remove(elimina, id);
    }
}
