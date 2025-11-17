package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CajaFisicaDaoService;
import com.saa.ejb.tesoreria.service.CajaFisicaService;
import com.saa.model.tesoreria.CajaFisica;
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

@Path("cjaa")
public class CajaFisicaRest {

    @EJB
    private CajaFisicaDaoService cajaFisicaDaoService;

    @EJB
    private CajaFisicaService cajaFisicaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CajaFisicaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CajaFisica.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CajaFisica> getAll() throws Throwable {
        return cajaFisicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_FISICA);
    }

    /**
     * Recupera un registro de CajaFisica por ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CajaFisica getId(@PathParam("id") Long id) throws Throwable {
        return cajaFisicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_FISICA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CajaFisica put(CajaFisica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CAJA_FISICA");
        return cajaFisicaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CajaFisica post(CajaFisica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CAJA_FISICA");
        return cajaFisicaService.saveSingle(registro);
    }

    /**
     * Selecciona registros por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<CajaFisica> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA CAJA_FISICA: " + test);
        return cajaFisicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_FISICA);
    }

    /**
     * Elimina un registro de CajaFisica por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CAJA_FISICA");
        CajaFisica elimina = new CajaFisica();
        cajaFisicaDaoService.remove(elimina, id);
    }
}
