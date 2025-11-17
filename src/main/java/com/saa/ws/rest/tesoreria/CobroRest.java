package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CobroDaoService;
import com.saa.ejb.tesoreria.service.CobroService;
import com.saa.model.tesoreria.Cobro;
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

@Path("cbro")
public class CobroRest {

    @EJB
    private CobroDaoService cobroDaoService;

    @EJB
    private CobroService cobroService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Cobro.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Cobro> getAll() throws Throwable {
        return cobroDaoService.selectAll(NombreEntidadesTesoreria.COBRO);
    }

    /**
     * Recupera un registro de Cobro por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Cobro getId(@PathParam("id") Long id) throws Throwable {
        return cobroDaoService.selectById(id, NombreEntidadesTesoreria.COBRO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Cobro put(Cobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO");
        return cobroService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Cobro post(Cobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO");
        return cobroService.saveSingle(registro);
    }

    /**
     * Selecciona registros de Cobro por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Cobro> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA COBRO: " + test);
        return cobroDaoService.selectAll(NombreEntidadesTesoreria.COBRO);
    }

    /**
     * Elimina un registro de Cobro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO");
        Cobro elimina = new Cobro();
        cobroDaoService.remove(elimina, id);
    }
}
