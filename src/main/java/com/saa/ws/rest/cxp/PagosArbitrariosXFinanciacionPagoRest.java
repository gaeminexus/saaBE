package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.PagosArbitrariosXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.PagosArbitrariosXFinanciacionPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.PagosArbitrariosXFinanciacionPago;

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

@Path("pafp")
public class PagosArbitrariosXFinanciacionPagoRest {

    @EJB
    private PagosArbitrariosXFinanciacionPagoDaoService PagosArbitrariosXFinanciacionPagoDaoService;

    @EJB
    private PagosArbitrariosXFinanciacionPagoService PagosArbitrariosXFinanciacionPagoService;

    @Context
    private UriInfo context;

    public PagosArbitrariosXFinanciacionPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PagosArbitrariosXFinanciacionPago> getAll() throws Throwable {
        return PagosArbitrariosXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public PagosArbitrariosXFinanciacionPago getId(@PathParam("id") Long id) throws Throwable {
        return PagosArbitrariosXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public PagosArbitrariosXFinanciacionPago put(PagosArbitrariosXFinanciacionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        return PagosArbitrariosXFinanciacionPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public PagosArbitrariosXFinanciacionPago post(PagosArbitrariosXFinanciacionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        return PagosArbitrariosXFinanciacionPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(PagosArbitrariosXFinanciacionPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        PagosArbitrariosXFinanciacionPago elimina = new PagosArbitrariosXFinanciacionPago();
        PagosArbitrariosXFinanciacionPagoDaoService.remove(elimina, id);
    }
}
