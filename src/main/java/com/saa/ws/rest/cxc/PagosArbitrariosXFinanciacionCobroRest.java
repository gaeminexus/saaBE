package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.PagosArbitrariosXFinanciacionCobroDaoService;
import com.saa.ejb.cxc.service.PagosArbitrariosXFinanciacionCobroService;
import com.saa.model.cxc.PagosArbitrariosXFinanciacionCobro;
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

@Path("pafc")
public class PagosArbitrariosXFinanciacionCobroRest {

    @EJB
    private PagosArbitrariosXFinanciacionCobroDaoService pagosArbitrariosXFinanciacionCobroDaoService;

    @EJB
    private PagosArbitrariosXFinanciacionCobroService pagosArbitrariosXFinanciacionCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PagosArbitrariosXFinanciacionCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of PagosArbitrariosXFinanciacionCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PagosArbitrariosXFinanciacionCobro> getAll() throws Throwable {
        return pagosArbitrariosXFinanciacionCobroDaoService.selectAll(NombreEntidadesCobro.PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO);
    }

    /**
     * Retrieves representation of an instance of PagosArbitrariosXFinanciacionCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public PagosArbitrariosXFinanciacionCobro getId(@PathParam("id") Long id) throws Throwable {
        return pagosArbitrariosXFinanciacionCobroDaoService.selectById(id, NombreEntidadesCobro.PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO);
    }

    /**
     * PUT method for updating or creating an instance of PagosArbitrariosXFinanciacionCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response put(PagosArbitrariosXFinanciacionCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(pagosArbitrariosXFinanciacionCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of PagosArbitrariosXFinanciacionCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Response post(PagosArbitrariosXFinanciacionCobro registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(pagosArbitrariosXFinanciacionCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of PagosArbitrariosXFinanciacionCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PagosArbitrariosXFinanciacionCobro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(pagosArbitrariosXFinanciacionCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of PagosArbitrariosXFinanciacionCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        PagosArbitrariosXFinanciacionCobro elimina = new PagosArbitrariosXFinanciacionCobro();
        pagosArbitrariosXFinanciacionCobroDaoService.remove(elimina, id);
    }

}