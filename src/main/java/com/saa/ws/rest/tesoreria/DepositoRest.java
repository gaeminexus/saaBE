package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.DepositoDaoService;
import com.saa.ejb.tesoreria.service.DepositoService;
import com.saa.model.tesoreria.Deposito;
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

@Path("dpst")
public class DepositoRest {

    @EJB
    private DepositoDaoService depositoDaoService;

    @EJB
    private DepositoService depositoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DepositoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Deposito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Deposito> getAll() throws Throwable {
        return depositoDaoService.selectAll(NombreEntidadesTesoreria.DEPOSITO);
    }

    /**
     * Recupera un registro de Deposito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Deposito getId(@PathParam("id") Long id) throws Throwable {
        return depositoDaoService.selectById(id, NombreEntidadesTesoreria.DEPOSITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Deposito put(Deposito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DEPOSITO");
        return depositoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Deposito post(Deposito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DEPOSITO");
        return depositoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de Deposito por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Deposito> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DEPOSITO: " + test);
        return depositoDaoService.selectAll(NombreEntidadesTesoreria.DEPOSITO);
    }

    /**
     * Elimina un registro de Deposito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DEPOSITO");
        Deposito elimina = new Deposito();
        depositoDaoService.remove(elimina, id);
    }
}
