package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TempCobroRetencionDaoService;
import com.saa.ejb.tesoreria.service.TempCobroRetencionService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroRetencion;

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

@Path("tcrt")
public class TempCobroRetencionRest {

    @EJB
    private TempCobroRetencionDaoService tempCobroRetencionDaoService;

    @EJB
    private TempCobroRetencionService tempCobroRetencionService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroRetencionRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroRetencion.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobroRetencion> getAll() throws Throwable {
        return tempCobroRetencionDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_RETENCION);
    }

    /**
     * Recupera un registro de TempCobroRetencion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobroRetencion getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroRetencionDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_RETENCION);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobroRetencion put(TempCobroRetencion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO_RETENCION");
        return tempCobroRetencionService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobroRetencion post(TempCobroRetencion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO_RETENCION");
        return tempCobroRetencionService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TempCobroRetencion por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempCobroRetencion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TEMP_COBRO_RETENCION: " + test);
        return tempCobroRetencionDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_RETENCION);
    }

    /**
     * Elimina un registro de TempCobroRetencion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO_RETENCION");
        TempCobroRetencion elimina = new TempCobroRetencion();
        tempCobroRetencionDaoService.remove(elimina, id);
    }
}
