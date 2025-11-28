package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.TempFinanciacionXDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.TempFinanciacionXDocumentoCobroService;
import com.saa.model.cxc.TempFinanciacionXDocumentoCobro;
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

@Path("tfdc")
public class TempFinanciacionXDocumentoCobroRest {

    @EJB
    private TempFinanciacionXDocumentoCobroDaoService tempFinanciacionXDocumentoCobroDaoService;

    @EJB
    private TempFinanciacionXDocumentoCobroService tempFinanciacionXDocumentoCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TempFinanciacionXDocumentoCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of TempFinanciacionXDocumentoCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempFinanciacionXDocumentoCobro> getAll() throws Throwable {
        return tempFinanciacionXDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_FINANCIACION_X_DOCUMENTO_COBRO);
    }

    /**
     * Retrieves representation of an instance of TempFinanciacionXDocumentoCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempFinanciacionXDocumentoCobro getId(@PathParam("id") Long id) throws Throwable {
        return tempFinanciacionXDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_FINANCIACION_X_DOCUMENTO_COBRO);
    }

    /**
     * PUT method for updating or creating an instance of TempFinanciacionXDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response put(TempFinanciacionXDocumentoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempFinanciacionXDocumentoCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempFinanciacionXDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Response post(TempFinanciacionXDocumentoCobro registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempFinanciacionXDocumentoCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempFinanciacionXDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TempFinanciacionXDocumentoCobro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempFinanciacionXDocumentoCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempFinanciacionXDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        TempFinanciacionXDocumentoCobro elimina = new TempFinanciacionXDocumentoCobro();
        tempFinanciacionXDocumentoCobroDaoService.remove(elimina, id);
    }

}