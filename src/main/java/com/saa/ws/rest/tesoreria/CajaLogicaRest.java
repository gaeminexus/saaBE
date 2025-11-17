package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CajaLogicaDaoService;
import com.saa.ejb.tesoreria.service.CajaLogicaService;
import com.saa.model.tesoreria.CajaLogica;
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

@Path("cjcn")
public class CajaLogicaRest {

    @EJB
    private CajaLogicaDaoService cajaLogicaDaoService;

    @EJB
    private CajaLogicaService cajaLogicaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CajaLogicaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CajaLogica.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CajaLogica> getAll() throws Throwable {
        return cajaLogicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_LOGICA);
    }

    /**
     * Recupera un registro de CajaLogica por ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CajaLogica getId(@PathParam("id") Long id) throws Throwable {
        return cajaLogicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_LOGICA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CajaLogica put(CajaLogica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CAJA_LOGICA");
        return cajaLogicaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CajaLogica post(CajaLogica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CAJA_LOGICA");
        return cajaLogicaService.saveSingle(registro);
    }

    /**
     * Selecciona registros por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<CajaLogica> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA CAJA_LOGICA: " + test);
        return cajaLogicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_LOGICA);
    }

    /**
     * Elimina un registro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CAJA_LOGICA");
        CajaLogica elimina = new CajaLogica();
        cajaLogicaDaoService.remove(elimina, id);
    }
}
