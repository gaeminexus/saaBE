package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempResumenValorDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempResumenValorDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempResumenValorDocumentoPago;

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

@Path("trdp")
public class TempResumenValorDocumentoPagoRest {

    @EJB
    private TempResumenValorDocumentoPagoDaoService TempResumenValorDocumentoPagoDaoService;

    @EJB
    private TempResumenValorDocumentoPagoService TempResumenValorDocumentoPagoService;

    @Context
    private UriInfo context;

    public TempResumenValorDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempResumenValorDocumentoPago> getAll() throws Throwable {
        return TempResumenValorDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempResumenValorDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return TempResumenValorDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempResumenValorDocumentoPago put(TempResumenValorDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO");
        return TempResumenValorDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempResumenValorDocumentoPago post(TempResumenValorDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO");
        return TempResumenValorDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempResumenValorDocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_RESUMEN_VALOR_DOCUMENTO_PAGO");
        TempResumenValorDocumentoPago elimina = new TempResumenValorDocumentoPago();
        TempResumenValorDocumentoPagoDaoService.remove(elimina, id);
    }
}
 