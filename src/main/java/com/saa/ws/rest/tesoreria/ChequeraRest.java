package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.ChequeraDaoService;
import com.saa.ejb.tesoreria.service.ChequeraService;
import com.saa.model.tesoreria.Chequera;
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

@Path("chqr")
public class ChequeraRest {

    @EJB
    private ChequeraDaoService chequeraDaoService;

    @EJB
    private ChequeraService chequeraService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ChequeraRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Chequera.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Chequera> getAll() throws Throwable {
        return chequeraDaoService.selectAll(NombreEntidadesTesoreria.CHEQUERA);
    }

    /**
     * Recupera un registro de Chequera por ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Chequera getId(@PathParam("id") Long id) throws Throwable {
        return chequeraDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUERA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Chequera put(Chequera registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CHEQUERA");
        return chequeraService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Chequera post(Chequera registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CHEQUERA");
        return chequeraService.saveSingle(registro);
    }

    /**
     * Selecciona registros por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Chequera> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA CHEQUERA: " + test);
        return chequeraDaoService.selectAll(NombreEntidadesTesoreria.CHEQUERA);
    }

    /**
     * Elimina un registro de Chequera por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CHEQUERA");
        Chequera elimina = new Chequera();
        chequeraDaoService.remove(elimina, id);
    }
}
