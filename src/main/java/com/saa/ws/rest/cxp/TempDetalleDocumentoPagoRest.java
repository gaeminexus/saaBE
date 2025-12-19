package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempDetalleDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempDetalleDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempDetalleDocumentoPago;

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

@Path("tdtp")
public class TempDetalleDocumentoPagoRest {

    @EJB
    private TempDetalleDocumentoPagoDaoService TempDetalleDocumentoPagoDaoService;

    @EJB
    private TempDetalleDocumentoPagoService TempDetalleDocumentoPagoService;

    @Context
    private UriInfo context;

    public TempDetalleDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempDetalleDocumentoPago> getAll() throws Throwable {
        return TempDetalleDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_DETALLE_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempDetalleDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return TempDetalleDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_DETALLE_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempDetalleDocumentoPago put(TempDetalleDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_DETALLE_DOCUMENTO_PAGO");
        return TempDetalleDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempDetalleDocumentoPago post(TempDetalleDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_DETALLE_DOCUMENTO_PAGO");
        return TempDetalleDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_DETALLE_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempDetalleDocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_DETALLE_DOCUMENTO_PAGO");
        TempDetalleDocumentoPago elimina = new TempDetalleDocumentoPago();
        TempDetalleDocumentoPagoDaoService.remove(elimina, id);
    }
}
 