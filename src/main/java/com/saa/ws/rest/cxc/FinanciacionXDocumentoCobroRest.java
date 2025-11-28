package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.FinanciacionXDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.FinanciacionXDocumentoCobroService;
import com.saa.model.cxc.FinanciacionXDocumentoCobro;
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

@Path("fxdc")
public class FinanciacionXDocumentoCobroRest {

    @EJB
    private FinanciacionXDocumentoCobroDaoService financiacionXDocumentoCobroDaoService;

    @EJB
    private FinanciacionXDocumentoCobroService financiacionXDocumentoCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public FinanciacionXDocumentoCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of FinanciacionXDocumentoCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<FinanciacionXDocumentoCobro> getAll() throws Throwable {
        return financiacionXDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.FINANCIACION_X_DOCUMENTO_COBRO);
    }

    /**
     * Retrieves representation of an instance of FinanciacionXDocumentoCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public FinanciacionXDocumentoCobro getId(@PathParam("id") Long id) throws Throwable {
        return financiacionXDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.FINANCIACION_X_DOCUMENTO_COBRO);
    }

    /**
     * PUT method for updating or creating an instance of FinanciacionXDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response put(FinanciacionXDocumentoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(financiacionXDocumentoCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of FinanciacionXDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Response post(FinanciacionXDocumentoCobro registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(financiacionXDocumentoCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of FinanciacionXDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de FinanciacionXDocumentoCobro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(financiacionXDocumentoCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of FinanciacionXDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        FinanciacionXDocumentoCobro elimina = new FinanciacionXDocumentoCobro();
        financiacionXDocumentoCobroDaoService.remove(elimina, id);
    }

}