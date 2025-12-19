package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempFinanciacionXDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempFinanciacionXDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempFinanciacionXDocumentoPago;

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

@Path("tfdp")
public class TempFinanciacionXDocumentoPagoRest {

    @EJB
    private TempFinanciacionXDocumentoPagoDaoService TempFinanciacionXDocumentoPagoDaoService;

    @EJB
    private TempFinanciacionXDocumentoPagoService TempFinanciacionXDocumentoPagoService;

    @Context
    private UriInfo context;

    public TempFinanciacionXDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempFinanciacionXDocumentoPago> getAll() throws Throwable {
        return TempFinanciacionXDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_FINANCIACION_X_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempFinanciacionXDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return TempFinanciacionXDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_FINANCIACION_X_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempFinanciacionXDocumentoPago put(TempFinanciacionXDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_FINANCIACION_X_DOCUMENTO_PAGO");
        return TempFinanciacionXDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempFinanciacionXDocumentoPago post(TempFinanciacionXDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_FINANCIACION_X_DOCUMENTO_PAGO");
        return TempFinanciacionXDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_FINANCIACION_X_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempFinanciacionXDocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_FINANCIACION_X_DOCUMENTO_PAGO");
        TempFinanciacionXDocumentoPago elimina = new TempFinanciacionXDocumentoPago();
        TempFinanciacionXDocumentoPagoDaoService.remove(elimina, id);
    }
}
 