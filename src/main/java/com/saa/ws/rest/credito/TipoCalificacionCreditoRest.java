package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoCalificacionCreditoDaoService;
import com.saa.ejb.credito.service.TipoCalificacionCreditoService;
import com.saa.model.credito.TipoCalificacionCredito;
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

@Path("tpcl")
public class TipoCalificacionCreditoRest {

    @EJB
    private TipoCalificacionCreditoDaoService tipoCalificacionCreditoDaoService;

    @EJB
    private TipoCalificacionCreditoService tipoCalificacionCreditoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TipoCalificacionCreditoRest() {
    }

    /**
     * GET ALL
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoCalificacionCredito> getAll() throws Throwable {
        return tipoCalificacionCreditoDaoService.selectAll(
                NombreEntidadesCredito.TIPO_CALIFICACION_CREDITO
        );
    }

    /**
     * GET BY ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TipoCalificacionCredito getId(@PathParam("id") Long id) throws Throwable {
        return tipoCalificacionCreditoDaoService.selectById(
                id,
                NombreEntidadesCredito.TIPO_CALIFICACION_CREDITO
        );
    }

    /**
     * PUT - UPDATE
     */
    @PUT
    @Consumes("application/json")
    public TipoCalificacionCredito put(TipoCalificacionCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TipoCalificacionCredito");
        return tipoCalificacionCreditoService.saveSingle(registro);
    }

    /**
     * POST - CREATE
     */
    @POST
    @Consumes("application/json")
    public TipoCalificacionCredito post(TipoCalificacionCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TipoCalificacionCredito");
        return tipoCalificacionCreditoService.saveSingle(registro);
    }

    /**
     * POST - SELECT BY CRITERIA
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoCalificacionCredito");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoCalificacionCreditoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE TipoCalificacionCredito");
        TipoCalificacionCredito elimina = new TipoCalificacionCredito();
        tipoCalificacionCreditoDaoService.remove(elimina, id);
    }

}
