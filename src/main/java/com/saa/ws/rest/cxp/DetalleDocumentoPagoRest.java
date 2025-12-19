package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.DetalleDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.DetalleDocumentoPagoService;
import com.saa.model.cxp.DetalleDocumentoPago;
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

@Path("dtdp")
public class DetalleDocumentoPagoRest {

    @EJB
    private DetalleDocumentoPagoDaoService DetalleDocumentoPagoDaoService;

    @EJB
    private DetalleDocumentoPagoService DetalleDocumentoPagoService;

    @Context
    private UriInfo context;

    public DetalleDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleDocumentoPago> getAll() throws Throwable {
        return DetalleDocumentoPagoDaoService.selectAll(NombreEntidadesPago.DETALLE_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DetalleDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return DetalleDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.DETALLE_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public DetalleDocumentoPago put(DetalleDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE_DOCUMENTO_PAGO");
        return DetalleDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public DetalleDocumentoPago post(DetalleDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE_DOCUMENTO_PAGO");
        return DetalleDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(DetalleDocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE_DOCUMENTO_PAGO");
        DetalleDocumentoPago elimina = new DetalleDocumentoPago();
        DetalleDocumentoPagoDaoService.remove(elimina, id);
    }
}
