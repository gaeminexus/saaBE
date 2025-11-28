package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.ResumenValorDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.ResumenValorDocumentoCobroService;
import com.saa.model.cxc.ResumenValorDocumentoCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

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

@Path("rvdc")
public class ResumenValorDocumentoCobroRest {

    @EJB
    private ResumenValorDocumentoCobroDaoService resumenValorDocumentoCobroDaoService;

    @EJB
    private ResumenValorDocumentoCobroService resumenValorDocumentoCobroService;

    @Context
    private UriInfo context;

    public ResumenValorDocumentoCobroRest() {
        // TODO Auto-generated constructor stub
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ResumenValorDocumentoCobro> getAll() throws Throwable {
        return resumenValorDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.RESUMEN_VALOR_DOCUMENTO_COBRO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ResumenValorDocumentoCobro getId(@PathParam("id") Long id) throws Throwable {
        return resumenValorDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.RESUMEN_VALOR_DOCUMENTO_COBRO);
    }

    @PUT
    @Consumes("application/json")
    public Response put(ResumenValorDocumentoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(resumenValorDocumentoCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @POST
    @Consumes("application/json")
    public Response post(ResumenValorDocumentoCobro registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(resumenValorDocumentoCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de ResumenValorDocumentoCobro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(resumenValorDocumentoCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        ResumenValorDocumentoCobro elimina = new ResumenValorDocumentoCobro();
        resumenValorDocumentoCobroDaoService.remove(elimina, id);
    }
}
