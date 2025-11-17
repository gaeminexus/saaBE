package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CobroChequeDaoService;
import com.saa.ejb.tesoreria.service.CobroChequeService;
import com.saa.model.tesoreria.CobroCheque;
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

@Path("cchq")
public class CobroChequeRest {

    @EJB
    private CobroChequeDaoService cobroChequeDaoService;

    @EJB
    private CobroChequeService cobroChequeService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroChequeRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroCheque.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CobroCheque> getAll() throws Throwable {
        return cobroChequeDaoService.selectAll(NombreEntidadesTesoreria.COBRO_CHEQUE);
    }

    /**
     * Recupera un registro de CobroCheque por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CobroCheque getId(@PathParam("id") Long id) throws Throwable {
        return cobroChequeDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_CHEQUE);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CobroCheque put(CobroCheque registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO CHEQUE");
        return cobroChequeService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CobroCheque post(CobroCheque registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO CHEQUE");
        return cobroChequeService.saveSingle(registro);
    }

    /**
     * Selecciona registros de CobroCheque por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<CobroCheque> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA COBRO CHEQUE: " + test);
        return cobroChequeDaoService.selectAll(NombreEntidadesTesoreria.COBRO_CHEQUE);
    }

    /**
     * Elimina un registro de CobroCheque por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO CHEQUE");
        CobroCheque elimina = new CobroCheque();
        cobroChequeDaoService.remove(elimina, id);
    }
}
