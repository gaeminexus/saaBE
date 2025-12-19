package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempComposicionCuotaInicialPagoDaoService;
import com.saa.ejb.cxp.service.TempComposicionCuotaInicialPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempComposicionCuotaInicialPago;

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

@Path("tcip")
public class TempComposicionCuotaInicialPagoRest {

    @EJB
    private TempComposicionCuotaInicialPagoDaoService TempComposicionCuotaInicialPagoDaoService;

    @EJB
    private TempComposicionCuotaInicialPagoService TempComposicionCuotaInicialPagoService;

    @Context
    private UriInfo context;

    public TempComposicionCuotaInicialPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempComposicionCuotaInicialPago> getAll() throws Throwable {
        return TempComposicionCuotaInicialPagoDaoService.selectAll(NombreEntidadesPago.TEMP_COMPOSICION_CUOTA_INICIAL_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempComposicionCuotaInicialPago getId(@PathParam("id") Long id) throws Throwable {
        return TempComposicionCuotaInicialPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_COMPOSICION_CUOTA_INICIAL_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempComposicionCuotaInicialPago put(TempComposicionCuotaInicialPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_COMPOSICION_CUOTA_INICIAL_PAGO");
        return TempComposicionCuotaInicialPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempComposicionCuotaInicialPago post(TempComposicionCuotaInicialPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_COMPOSICION_CUOTA_INICIAL_PAGO");
        return TempComposicionCuotaInicialPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_COMPOSICION_CUOTA_INICIAL_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempComposicionCuotaInicialPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_COMPOSICION_CUOTA_INICIAL_PAGO");
        TempComposicionCuotaInicialPago elimina = new TempComposicionCuotaInicialPago();
        TempComposicionCuotaInicialPagoDaoService.remove(elimina, id);
    }
}
