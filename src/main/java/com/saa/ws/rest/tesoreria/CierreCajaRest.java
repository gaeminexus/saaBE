package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CierreCajaDaoService;
import com.saa.ejb.tesoreria.service.CierreCajaService;
import com.saa.model.tesoreria.CierreCaja;
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

@Path("crcj")
public class CierreCajaRest {

    @EJB
    private CierreCajaDaoService cierreCajaDaoService;

    @EJB
    private CierreCajaService cierreCajaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CierreCajaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CierreCaja.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CierreCaja> getAll() throws Throwable {
        return cierreCajaDaoService.selectAll(NombreEntidadesTesoreria.CIERRE_CAJA);
    }

    /**
     * Recupera un registro de CierreCaja por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CierreCaja getId(@PathParam("id") Long id) throws Throwable {
        return cierreCajaDaoService.selectById(id, NombreEntidadesTesoreria.CIERRE_CAJA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CierreCaja put(CierreCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CIERRE CAJA");
        return cierreCajaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CierreCaja post(CierreCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CIERRE CAJA");
        return cierreCajaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de CierreCaja por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<CierreCaja> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA CIERRE CAJA: " + test);
        return cierreCajaDaoService.selectAll(NombreEntidadesTesoreria.CIERRE_CAJA);
    }

    /**
     * Elimina un registro de CierreCaja por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CIERRE CAJA");
        CierreCaja elimina = new CierreCaja();
        cierreCajaDaoService.remove(elimina, id);
    }
}
