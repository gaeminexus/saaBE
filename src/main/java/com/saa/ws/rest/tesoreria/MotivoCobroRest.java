package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.MotivoCobroDaoService;
import com.saa.ejb.tesoreria.service.MotivoCobroService;
import com.saa.model.tesoreria.MotivoCobro;
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

@Path("cmtv")
public class MotivoCobroRest {

    @EJB
    private MotivoCobroDaoService motivoCobroDaoService;

    @EJB
    private MotivoCobroService motivoCobroService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public MotivoCobroRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de MotivoCobro.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MotivoCobro> getAll() throws Throwable {
        return motivoCobroDaoService.selectAll(NombreEntidadesTesoreria.MOTIVO_COBRO);
    }

    /**
     * Recupera un registro de MotivoCobro por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public MotivoCobro getId(@PathParam("id") Long id) throws Throwable {
        return motivoCobroDaoService.selectById(id, NombreEntidadesTesoreria.MOTIVO_COBRO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public MotivoCobro put(MotivoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT MOTIVO COBRO");
        return motivoCobroService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public MotivoCobro post(MotivoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST MOTIVO COBRO");
        return motivoCobroService.saveSingle(registro);
    }

    /**
     * Selecciona registros de MotivoCobro por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<MotivoCobro> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA MOTIVO COBRO: " + test);
        return motivoCobroDaoService.selectAll(NombreEntidadesTesoreria.MOTIVO_COBRO);
    }

    /**
     * Elimina un registro de MotivoCobro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE MOTIVO COBRO");
        MotivoCobro elimina = new MotivoCobro();
        motivoCobroDaoService.remove(elimina, id);
    }
}
