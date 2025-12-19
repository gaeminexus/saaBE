package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempDocumentoPagoDaoService;
import com.saa.ejb.cxp.service.TempDocumentoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempDocumentoPago;

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

@Path("tdcp")
public class TempDocumentoPagoRest {

    @EJB
    private TempDocumentoPagoDaoService TempDocumentoPagoDaoService;

    @EJB
    private TempDocumentoPagoService TempDocumentoPagoService;

    @Context
    private UriInfo context;

    public TempDocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempDocumentoPago> getAll() throws Throwable {
        return TempDocumentoPagoDaoService.selectAll(NombreEntidadesPago.TEMP_DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempDocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return TempDocumentoPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempDocumentoPago put(TempDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_DOCUMENTO_PAGO");
        return TempDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempDocumentoPago post(TempDocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_DOCUMENTO_PAGO");
        return TempDocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempDocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_DOCUMENTO_PAGO");
        TempDocumentoPago elimina = new TempDocumentoPago();
        TempDocumentoPagoDaoService.remove(elimina, id);
    }
}
 