package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempValorImpuestoDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempValorImpuestoDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempValorImpuestoDocumentoPago;

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

@Path("tidp")
public class TempValorImpuestoDocumentoPagoRest {

    @EJB
    private TempValorImpuestoDocumentoPagoDaoService TempValorImpuestoDocumentoPagoDaoService;

    @EJB
    private TempValorImpuestoDocumentoPagoService TempValorImpuestoDocumentoPagoService;

    @Context
    private UriInfo context;

    public TempValorImpuestoDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempValorImpuestoDocumentoPago> getAll() throws Throwable {
        return TempValorImpuestoDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempValorImpuestoDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return TempValorImpuestoDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempValorImpuestoDocumentoPago put(TempValorImpuestoDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO");
        return TempValorImpuestoDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempValorImpuestoDocumentoPago post(TempValorImpuestoDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO");
        return TempValorImpuestoDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempValorImpuestoDocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_VALOR_IMPUESTO_DOCUMENTO_PAGO");
        TempValorImpuestoDocumentoPago elimina = new TempValorImpuestoDocumentoPago();
        TempValorImpuestoDocumentoPagoDaoService.remove(elimina, id);
    }
}
 