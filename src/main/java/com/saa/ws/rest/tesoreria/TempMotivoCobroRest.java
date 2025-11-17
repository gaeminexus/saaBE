package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TempMotivoCobroDaoService;
import com.saa.ejb.tesoreria.service.TempMotivoCobroService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempMotivoCobro;

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

@Path("tcmt")
public class TempMotivoCobroRest {

    @EJB
    private TempMotivoCobroDaoService tempMotivoCobroDaoService;

    @EJB
    private TempMotivoCobroService tempMotivoCobroService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempMotivoCobroRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempMotivoCobro.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempMotivoCobro> getAll() throws Throwable {
        return tempMotivoCobroDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_COBRO);
    }

    /**
     * Recupera un registro de TempMotivoCobro por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempMotivoCobro getId(@PathParam("id") Long id) throws Throwable {
        return tempMotivoCobroDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_MOTIVO_COBRO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempMotivoCobro put(TempMotivoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_MOTIVO_COBRO");
        return tempMotivoCobroService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempMotivoCobro post(TempMotivoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_MOTIVO_COBRO");
        return tempMotivoCobroService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TempMotivoCobro por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempMotivoCobro> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TEMP_MOTIVO_COBRO: " + test);
        return tempMotivoCobroDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_COBRO);
    }

    /**
     * Elimina un registro de TempMotivoCobro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_MOTIVO_COBRO");
        TempMotivoCobro elimina = new TempMotivoCobro();
        tempMotivoCobroDaoService.remove(elimina, id);
    }
}
