package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.CuotaXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.CuotaXFinanciacionPagoService;
import com.saa.model.cxp.CuotaXFinanciacionPago;
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

@Path("cxdp")
public class CuotaXFinanciacionPagoRest {

    @EJB
    private CuotaXFinanciacionPagoDaoService CuotaXFinanciacionPagoDaoService;

    @EJB
    private CuotaXFinanciacionPagoService CuotaXFinanciacionPagoService;

    @Context
    private UriInfo context;

    public CuotaXFinanciacionPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CuotaXFinanciacionPago> getAll() throws Throwable {
        return CuotaXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.CUOTA_X_FINANCIACION_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CuotaXFinanciacionPago getId(@PathParam("id") Long id) throws Throwable {
        return CuotaXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.CUOTA_X_FINANCIACION_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public CuotaXFinanciacionPago put(CuotaXFinanciacionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - CUOTA_X_FINANCIACION_PAGO");
        return CuotaXFinanciacionPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public CuotaXFinanciacionPago post(CuotaXFinanciacionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - CUOTA_X_FINANCIACION_PAGO");
        return CuotaXFinanciacionPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CUOTA_X_FINANCIACION_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(CuotaXFinanciacionPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - CUOTA_X_FINANCIACION_PAGO");
        CuotaXFinanciacionPago elimina = new CuotaXFinanciacionPago();
        CuotaXFinanciacionPagoDaoService.remove(elimina, id);
    }
}
