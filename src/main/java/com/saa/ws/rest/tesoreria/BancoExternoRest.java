package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.BancoExternoDaoService;
import com.saa.ejb.tesoreria.service.BancoExternoService;
import com.saa.model.tesoreria.BancoExterno;
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

@Path("bext")
public class BancoExternoRest {

    @EJB
    private BancoExternoDaoService bancoExternoDaoService;

    @EJB
    private BancoExternoService bancoExternoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public BancoExternoRest() {
        // Constructor vacío
    }

    /**
     * Obtiene todos los registros de BancoExterno.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<BancoExterno> getAll() throws Throwable {
        return bancoExternoDaoService.selectAll(NombreEntidadesTesoreria.BANCO_EXTERNO);
    }

    /**
     * Obtiene un BancoExterno por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public BancoExterno getId(@PathParam("id") Long id) throws Throwable {
        return bancoExternoDaoService.selectById(id, NombreEntidadesTesoreria.BANCO_EXTERNO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public BancoExterno put(BancoExterno registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT BANCO_EXTERNO");
        return bancoExternoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public BancoExterno post(BancoExterno registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST BANCO_EXTERNO");
        return bancoExternoService.saveSingle(registro);
    }

    /**
     * Busca registros por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<BancoExterno> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA BANCO_EXTERNO: " + test);
        return bancoExternoDaoService.selectAll(NombreEntidadesTesoreria.BANCO_EXTERNO);
    }

    /**
     * Elimina un registro por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE BANCO_EXTERNO");
        BancoExterno elimina = new BancoExterno();
        bancoExternoDaoService.remove(elimina, id);
    }
}
