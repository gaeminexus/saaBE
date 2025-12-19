package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempPagosArbitrariosXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.TempPagosArbitrariosXFinanciacionPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempPagosArbitrariosXFinanciacionPago;

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

@Path("tpfp")
public class TempPagosArbitrariosXFinanciacionPagoRest {

    @EJB
    private TempPagosArbitrariosXFinanciacionPagoDaoService TempPagosArbitrariosXFinanciacionPagoDaoService;

    @EJB
    private TempPagosArbitrariosXFinanciacionPagoService TempPagosArbitrariosXFinanciacionPagoService;

    @Context
    private UriInfo context;

    public TempPagosArbitrariosXFinanciacionPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempPagosArbitrariosXFinanciacionPago> getAll() throws Throwable {
        return TempPagosArbitrariosXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempPagosArbitrariosXFinanciacionPago getId(@PathParam("id") Long id) throws Throwable {
        return TempPagosArbitrariosXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempPagosArbitrariosXFinanciacionPago put(TempPagosArbitrariosXFinanciacionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        return TempPagosArbitrariosXFinanciacionPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempPagosArbitrariosXFinanciacionPago post(TempPagosArbitrariosXFinanciacionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        return TempPagosArbitrariosXFinanciacionPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempPagosArbitrariosXFinanciacionPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        TempPagosArbitrariosXFinanciacionPago elimina = new TempPagosArbitrariosXFinanciacionPago();
        TempPagosArbitrariosXFinanciacionPagoDaoService.remove(elimina, id);
    }
}
 