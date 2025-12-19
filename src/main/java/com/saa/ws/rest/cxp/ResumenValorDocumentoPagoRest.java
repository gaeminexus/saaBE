package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.ResumenValorDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.ResumenValorDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ResumenValorDocumentoPago;

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

@Path("rvdp")
public class ResumenValorDocumentoPagoRest {

    @EJB
    private ResumenValorDocumentoPagoDaoService ResumenValorDocumentoPagoDaoService;

    @EJB
    private ResumenValorDocumentoPagoService ResumenValorDocumentoPagoService;

    @Context
    private UriInfo context;

    public ResumenValorDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ResumenValorDocumentoPago> getAll() throws Throwable {
        return ResumenValorDocumentoPagoDaoService.selectAll(NombreEntidadesPago.RESUMEN_VALOR_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ResumenValorDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return ResumenValorDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.RESUMEN_VALOR_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public ResumenValorDocumentoPago put(ResumenValorDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - RESUMEN_VALOR_DOCUMENTO_PAGO");
        return ResumenValorDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public ResumenValorDocumentoPago post(ResumenValorDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - RESUMEN_VALOR_DOCUMENTO_PAGO");
        return ResumenValorDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de RESUMEN_VALOR_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(ResumenValorDocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - RESUMEN_VALOR_DOCUMENTO_PAGO");
        ResumenValorDocumentoPago elimina = new ResumenValorDocumentoPago();
        ResumenValorDocumentoPagoDaoService.remove(elimina, id);
    }
}
