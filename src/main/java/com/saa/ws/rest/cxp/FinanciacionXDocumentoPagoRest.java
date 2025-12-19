package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.FinanciacionXDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.FinanciacionXDocumentoPagoService;
import com.saa.model.cxp.FinanciacionXDocumentoPago;
import com.saa.model.cxp.NombreEntidadesPago;

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

@Path("fxdp")
public class FinanciacionXDocumentoPagoRest {

    @EJB
    private FinanciacionXDocumentoPagoDaoService FinanciacionXDocumentoPagoDaoService;

    @EJB
    private FinanciacionXDocumentoPagoService FinanciacionXDocumentoPagoService;

    @Context
    private UriInfo context;

    public FinanciacionXDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<FinanciacionXDocumentoPago> getAll() throws Throwable {
        return FinanciacionXDocumentoPagoDaoService.selectAll(NombreEntidadesPago.FINANCIACION_X_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public FinanciacionXDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return FinanciacionXDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.FINANCIACION_X_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public FinanciacionXDocumentoPago put(FinanciacionXDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - FINANCIACION_X_DOCUMENTO_PAGO");
        return FinanciacionXDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public FinanciacionXDocumentoPago post(FinanciacionXDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - FINANCIACION_X_DOCUMENTO_PAGO");
        return FinanciacionXDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de FINANCIACION_X_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(FinanciacionXDocumentoPagoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - FINANCIACION_X_DOCUMENTO_PAGO");
        FinanciacionXDocumentoPago elimina = new FinanciacionXDocumentoPago();
        FinanciacionXDocumentoPagoDaoService.remove(elimina, id);
    }
}
