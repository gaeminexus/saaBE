package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TransferenciaDaoService;
import com.saa.ejb.tesoreria.service.TransferenciaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.Transferencia;

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

@Path("trns")
public class TransferenciaRest {

    @EJB
    private TransferenciaDaoService transferenciaDaoService;

    @EJB
    private TransferenciaService transferenciaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TransferenciaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Transferencia.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Transferencia> getAll() throws Throwable {
        return transferenciaDaoService.selectAll(NombreEntidadesTesoreria.TRANSFERENCIA);
    }

    /**
     * Recupera un registro de Transferencia por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Transferencia getId(@PathParam("id") Long id) throws Throwable {
        return transferenciaDaoService.selectById(id, NombreEntidadesTesoreria.TRANSFERENCIA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Transferencia put(Transferencia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TRANSFERENCIA");
        return transferenciaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Transferencia post(Transferencia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TRANSFERENCIA");
        return transferenciaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de Transferencia por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Transferencia> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TRANSFERENCIA: " + test);
        return transferenciaDaoService.selectAll(NombreEntidadesTesoreria.TRANSFERENCIA);
    }

    /**
     * Elimina un registro de Transferencia por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TRANSFERENCIA");
        Transferencia elimina = new Transferencia();
        transferenciaDaoService.remove(elimina, id);
    }
}
