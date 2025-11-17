package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.SaldoBancoDaoService;
import com.saa.ejb.tesoreria.service.SaldoBancoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.SaldoBanco;

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

@Path("slcb")
public class SaldoBancoRest {

    @EJB
    private SaldoBancoDaoService saldoBancoDaoService;

    @EJB
    private SaldoBancoService saldoBancoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public SaldoBancoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de SaldoBanco.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<SaldoBanco> getAll() throws Throwable {
        return saldoBancoDaoService.selectAll(NombreEntidadesTesoreria.SALDO_BANCO);
    }

    /**
     * Recupera un registro de SaldoBanco por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public SaldoBanco getId(@PathParam("id") Long id) throws Throwable {
        return saldoBancoDaoService.selectById(id, NombreEntidadesTesoreria.SALDO_BANCO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public SaldoBanco put(SaldoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT SALDO_BANCO");
        return saldoBancoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public SaldoBanco post(SaldoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST SALDO_BANCO");
        return saldoBancoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de SaldoBanco por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<SaldoBanco> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA SALDO_BANCO: " + test);
        return saldoBancoDaoService.selectAll(NombreEntidadesTesoreria.SALDO_BANCO);
    }

    /**
     * Elimina un registro de SaldoBanco por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE SALDO_BANCO");
        SaldoBanco elimina = new SaldoBanco();
        saldoBancoDaoService.remove(elimina, id);
    }
}

