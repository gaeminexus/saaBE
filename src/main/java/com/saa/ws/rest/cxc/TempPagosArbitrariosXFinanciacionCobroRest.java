package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.TempPagosArbitrariosXFinanciacionCobroDaoService;
import com.saa.ejb.cxc.service.TempPagosArbitrariosXFinanciacionCobroService;
import com.saa.model.cxc.TempPagosArbitrariosXFinanciacionCobro;
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

@Path("tpfc")
public class TempPagosArbitrariosXFinanciacionCobroRest {

    @EJB
    private TempPagosArbitrariosXFinanciacionCobroDaoService tempPagosArbitrariosXFinanciacionCobroDaoService;

    @EJB
    private TempPagosArbitrariosXFinanciacionCobroService tempPagosArbitrariosXFinanciacionCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TempPagosArbitrariosXFinanciacionCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of TempPagosArbitrariosXFinanciacionCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempPagosArbitrariosXFinanciacionCobro> lista = tempPagosArbitrariosXFinanciacionCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TempPagosArbitrariosXFinanciacionCobro registro = tempPagosArbitrariosXFinanciacionCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TempPagosArbitrariosXFinanciacionCobro registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            TempPagosArbitrariosXFinanciacionCobro resultado = tempPagosArbitrariosXFinanciacionCobroService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempPagosArbitrariosXFinanciacionCobro registro) {
        System.out.println("LLEGA AL POST");
        try {
            TempPagosArbitrariosXFinanciacionCobro resultado = tempPagosArbitrariosXFinanciacionCobroService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TempPagosArbitrariosXFinanciacionCobro");
        try {
            return Response.status(Response.Status.OK)
                    .entity(tempPagosArbitrariosXFinanciacionCobroService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE");
        try {
            TempPagosArbitrariosXFinanciacionCobro elimina = new TempPagosArbitrariosXFinanciacionCobro();
            tempPagosArbitrariosXFinanciacionCobroDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}