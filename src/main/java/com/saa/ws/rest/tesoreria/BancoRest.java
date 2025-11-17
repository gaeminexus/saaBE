package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.BancoDaoService;
import com.saa.ejb.tesoreria.service.BancoService;
import com.saa.model.tesoreria.Banco;
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

@Path("bnco")
public class BancoRest {

    @EJB
    private BancoDaoService bancoDaoService;

    @EJB
    private BancoService bancoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public BancoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Banco.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Banco> getAll() throws Throwable {
        return bancoDaoService.selectAll(NombreEntidadesTesoreria.BANCO);
    }

    /**
     * Recupera un Banco por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Banco getId(@PathParam("id") Long id) throws Throwable {
        return bancoDaoService.selectById(id, NombreEntidadesTesoreria.BANCO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Banco put(Banco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT BANCO");
        return bancoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Banco post(Banco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST BANCO");
        return bancoService.saveSingle(registro);
    }

    /**
     * Selecciona registros según criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Banco> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA BANCO: " + test);
        return bancoDaoService.selectAll(NombreEntidadesTesoreria.BANCO);
    }

    /**
     * Elimina un Banco por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE BANCO");
        Banco elimina = new Banco();
        bancoDaoService.remove(elimina, id);
    }
}
