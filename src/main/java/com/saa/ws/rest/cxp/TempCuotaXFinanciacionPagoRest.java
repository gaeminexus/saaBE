package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempCuotaXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.TempCuotaXFinanciacionPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempCuotaXFinanciacionPago;

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

@Path("tcdp")
public class TempCuotaXFinanciacionPagoRest {

    @EJB
    private TempCuotaXFinanciacionPagoDaoService TempCuotaXFinanciacionPagoDaoService;

    @EJB
    private TempCuotaXFinanciacionPagoService TempCuotaXFinanciacionPagoService;

    @Context
    private UriInfo context;

    public TempCuotaXFinanciacionPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCuotaXFinanciacionPago> getAll() throws Throwable {
        return TempCuotaXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.TEMP_CUOTA_X_FINANCIACION_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCuotaXFinanciacionPago getId(@PathParam("id") Long id) throws Throwable {
        return TempCuotaXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_CUOTA_X_FINANCIACION_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempCuotaXFinanciacionPago put(TempCuotaXFinanciacionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_CUOTA_X_FINANCIACION_PAGO");
        return TempCuotaXFinanciacionPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempCuotaXFinanciacionPago post(TempCuotaXFinanciacionPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_CUOTA_X_FINANCIACION_PAGO");
        return TempCuotaXFinanciacionPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_CUOTA_X_FINANCIACION_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempCuotaXFinanciacionPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_CUOTA_X_FINANCIACION_PAGO");
        TempCuotaXFinanciacionPago elimina = new TempCuotaXFinanciacionPago();
        TempCuotaXFinanciacionPagoDaoService.remove(elimina, id);
    }
}
 