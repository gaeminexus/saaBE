package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.TempResumenValorDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.TempResumenValorDocumentoCobroService;
import com.saa.model.cxc.TempResumenValorDocumentoCobro;
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

@Path("trdc")
public class TempResumenValorDocumentoCobroRest {

    @EJB
    private TempResumenValorDocumentoCobroDaoService tempResumenValorDocumentoCobroDaoService;

    @EJB
    private TempResumenValorDocumentoCobroService tempResumenValorDocumentoCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TempResumenValorDocumentoCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of TempResumenValorDocumentoCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempResumenValorDocumentoCobro> getAll() throws Throwable {
        return tempResumenValorDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_RESUMEN_VALOR_DOCUMENTO_COBRO);
    }

    /**
     * Retrieves representation of an instance of TempResumenValorDocumentoCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempResumenValorDocumentoCobro getId(@PathParam("id") Long id) throws Throwable {
        return tempResumenValorDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_RESUMEN_VALOR_DOCUMENTO_COBRO);
    }

    /**
     * PUT method for updating or creating an instance of TempResumenValorDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response put(TempResumenValorDocumentoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempResumenValorDocumentoCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempResumenValorDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Response post(TempResumenValorDocumentoCobro registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempResumenValorDocumentoCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempResumenValorDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TempResumenValorDocumentoCobro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempResumenValorDocumentoCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempResumenValorDocumentoCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        TempResumenValorDocumentoCobro elimina = new TempResumenValorDocumentoCobro();
        tempResumenValorDocumentoCobroDaoService.remove(elimina, id);
    }

}