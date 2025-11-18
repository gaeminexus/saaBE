package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoContratoDaoService;
import com.saa.ejb.credito.service.TipoContratoService;
import com.saa.model.credito.TipoContrato;
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

@Path("tpcn")
public class TipoContratoRest {

    @EJB
    private TipoContratoDaoService tipoContratoDaoService;

    @EJB
    private TipoContratoService tipoContratoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TipoContratoRest() {
    }

    /**
     * GET ALL
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoContrato> getAll() throws Throwable {
        return tipoContratoDaoService.selectAll(NombreEntidadesCredito.TIPO_CONTRATO);
    }

    /**
     * GET BY ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TipoContrato getId(@PathParam("id") Long id) throws Throwable {
        return tipoContratoDaoService.selectById(id, NombreEntidadesCredito.TIPO_CONTRATO);
    }

    /**
     * PUT - UPDATE
     */
    @PUT
    @Consumes("application/json")
    public TipoContrato put(TipoContrato registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TipoContrato");
        return tipoContratoService.saveSingle(registro);
    }

    /**
     * POST - CREATE
     */
    @POST
    @Consumes("application/json")
    public TipoContrato post(TipoContrato registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TipoContrato");
        return tipoContratoService.saveSingle(registro);
    }

    /**
     * POST - SELECT BY CRITERIA
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoContrato");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoContratoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE TipoContrato");
        TipoContrato elimina = new TipoContrato();
        tipoContratoDaoService.remove(elimina, id);
    }

}
