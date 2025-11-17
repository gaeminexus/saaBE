package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CuentaBancariaDaoService;
import com.saa.ejb.tesoreria.service.CuentaBancariaService;
import com.saa.model.tesoreria.CuentaBancaria;
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

@Path("cnbc")
public class CuentaBancariaRest {

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private CuentaBancariaService cuentaBancariaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CuentaBancariaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CuentaBancaria.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CuentaBancaria> getAll() throws Throwable {
        return cuentaBancariaDaoService.selectAll(NombreEntidadesTesoreria.CUENTA_BANCARIA);
    }

    /**
     * Recupera un registro de CuentaBancaria por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CuentaBancaria getId(@PathParam("id") Long id) throws Throwable {
        return cuentaBancariaDaoService.selectById(id, NombreEntidadesTesoreria.CUENTA_BANCARIA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CuentaBancaria put(CuentaBancaria registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CUENTA BANCARIA");
        return cuentaBancariaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CuentaBancaria post(CuentaBancaria registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CUENTA BANCARIA");
        return cuentaBancariaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de CuentaBancaria por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<CuentaBancaria> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA CUENTA BANCARIA: " + test);
        return cuentaBancariaDaoService.selectAll(NombreEntidadesTesoreria.CUENTA_BANCARIA);
    }

    /**
     * Elimina un registro de CuentaBancaria por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CUENTA BANCARIA");
        CuentaBancaria elimina = new CuentaBancaria();
        cuentaBancariaDaoService.remove(elimina, id);
    }
}
