package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.DocumentoCreditoDaoService;
import com.saa.ejb.credito.service.DocumentoCreditoService;
import com.saa.model.credito.DocumentoCredito;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("dcmn")
public class DocumentoCreditoRest {

    @EJB
    private DocumentoCreditoDaoService documentoCreditoDaoService;

    @EJB
    private DocumentoCreditoService documentoCreditoService;

    @Context
    private UriInfo context;

    public DocumentoCreditoRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DocumentoCredito> getAll() throws Throwable {
        return documentoCreditoDaoService.selectAll(NombreEntidadesCredito.DOCUMENTO_CREDITO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DocumentoCredito getId(@PathParam("id") Long id) throws Throwable {
        return documentoCreditoDaoService.selectById(id, NombreEntidadesCredito.DOCUMENTO_CREDITO);
    }

    @PUT
    @Consumes("application/json")
    public DocumentoCredito put(DocumentoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - DCMN");
        return documentoCreditoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public DocumentoCredito post(DocumentoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - DCMN");
        return documentoCreditoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DCMN");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                .entity(documentoCreditoService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - DCMN");
        DocumentoCredito elimina = new DocumentoCredito();
        documentoCreditoDaoService.remove(elimina, id);
    }
}
