package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.ChequeDaoService;
import com.saa.ejb.tesoreria.service.ChequeService;
import com.saa.model.tesoreria.Cheque;
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

@Path("dtch")
public class ChequeRest {

    @EJB
    private ChequeDaoService chequeDaoService;

    @EJB
    private ChequeService chequeService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ChequeRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Cheque.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Cheque> getAll() throws Throwable {
        return chequeDaoService.selectAll(NombreEntidadesTesoreria.CHEQUE);
    }

    /**
     * Recupera un registro de Cheque por ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Cheque getId(@PathParam("id") Long id) throws Throwable {
        return chequeDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUE);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Cheque put(Cheque registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CHEQUE");
        return chequeService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Cheque post(Cheque registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CHEQUE");
        return chequeService.saveSingle(registro);
    }

    /**
     * Selecciona registros por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Cheque> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA CHEQUE: " + test);
        return chequeDaoService.selectAll(NombreEntidadesTesoreria.CHEQUE);
    }

    /**
     * Elimina un registro de Cheque por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CHEQUE");
        Cheque elimina = new Cheque();
        chequeDaoService.remove(elimina, id);
    }
}
