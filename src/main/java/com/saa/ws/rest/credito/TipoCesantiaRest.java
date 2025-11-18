package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoCesantiaDaoService;
import com.saa.ejb.credito.service.TipoCesantiaService;
import com.saa.model.credito.TipoCesantia;
import com.saa.model.credito.NombreEntidadesCredito;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("tpcs")
public class TipoCesantiaRest {

    @EJB
    private TipoCesantiaDaoService tipoCesantiaDaoService;

    @EJB
    private TipoCesantiaService tipoCesantiaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TipoCesantiaRest() {
    }

    /**
     * GET ALL
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoCesantia> getAll() throws Throwable {
        return tipoCesantiaDaoService.selectAll(NombreEntidadesCredito.TIPO_CESANTIA);
    }

    /**
     * GET BY ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TipoCesantia getId(@PathParam("id") Long id) throws Throwable {
        return tipoCesantiaDaoService.selectById(id, NombreEntidadesCredito.TIPO_CESANTIA);
    }

    /**
     * PUT - UPDATE
     */
    @PUT
    @Consumes("application/json")
    public TipoCesantia put(TipoCesantia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TipoCesantia");
        return tipoCesantiaService.saveSingle(registro);
    }

    /**
     * POST - CREATE
     */
    @POST
    @Consumes("application/json")
    public TipoCesantia post(TipoCesantia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TipoCesantia");
        return tipoCesantiaService.saveSingle(registro);
    }

    /**
     * POST - SELECT BY CRITERIA
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoCesantia");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoCesantiaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return respuesta;
    }

    /**
     * DELETE
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TipoCesantia");
        TipoCesantia elimina = new TipoCesantia();
        tipoCesantiaDaoService.remove(elimina, id);
    }

}
