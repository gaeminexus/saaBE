package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.DocumentoPagoDaoService;
import com.saa.ejb.cxp.service.DocumentoPagoService;
import com.saa.model.cxp.DocumentoPago;
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

@Path("dcmp")
public class DocumentoPagoRest {

    @EJB
    private DocumentoPagoDaoService DocumentoPagoDaoService;

    @EJB
    private DocumentoPagoService DocumentoPagoService;

    @Context
    private UriInfo context;

    public DocumentoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DocumentoPago> getAll() throws Throwable {
        return DocumentoPagoDaoService.selectAll(NombreEntidadesPago.DOCUMENTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DocumentoPago getId(@PathParam("id") Long id) throws Throwable {
        return DocumentoPagoDaoService.selectById(id, NombreEntidadesPago.DOCUMENTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public DocumentoPago put(DocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - DOCUMENTO_PAGO");
        return DocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public DocumentoPago post(DocumentoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - DOCUMENTO_PAGO");
        return DocumentoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DOCUMENTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(DocumentoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DOCUMENTO_PAGO");
        DocumentoPago elimina = new DocumentoPago();
        DocumentoPagoDaoService.remove(elimina, id);
    }
}
