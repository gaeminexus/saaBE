package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.ValorImpuestoDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.ValorImpuestoDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ValorImpuestoDocumentoPago;

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

@Path("vidp")
public class ValorImpuestoDocumentoPagoRest {

    @EJB
    private ValorImpuestoDocumentoPagoDaoService ValorImpuestoDocumentoPagoDaoService;

    @EJB
    private ValorImpuestoDocumentoPagoService ValorImpuestoDocumentoPagoService;

    @Context
    private UriInfo context;

    public ValorImpuestoDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ValorImpuestoDocumentoPago> getAll() throws Throwable {
        return ValorImpuestoDocumentoPagoDaoService.selectAll(NombreEntidadesPago.VALOR_IMPUESTO_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ValorImpuestoDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return ValorImpuestoDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.VALOR_IMPUESTO_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public ValorImpuestoDocumentoPago put(ValorImpuestoDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - VALOR_IMPUESTO_DOCUMENTO_PAGO");
        return ValorImpuestoDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public ValorImpuestoDocumentoPago post(ValorImpuestoDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - VALOR_IMPUESTO_DOCUMENTO_PAGO");
        return ValorImpuestoDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de VALOR_IMPUESTO_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(ValorImpuestoDocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - VALOR_IMPUESTO_DOCUMENTO_PAGO");
        ValorImpuestoDocumentoPago elimina = new ValorImpuestoDocumentoPago();
        ValorImpuestoDocumentoPagoDaoService.remove(elimina, id);
    }
}
 