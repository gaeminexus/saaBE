package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempValorImpuestoDetallePagoDaoService;
import com.saa.ejb.cxp.service.TempValorImpuestoDetallePagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempValorImpuestoDetallePago;

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

@Path("titp")
public class TempValorImpuestoDetallePagoRest {

    @EJB
    private TempValorImpuestoDetallePagoDaoService TempValorImpuestoDetallePagoDaoService;

    @EJB
    private TempValorImpuestoDetallePagoService TempValorImpuestoDetallePagoService;

    @Context
    private UriInfo context;

    public TempValorImpuestoDetallePagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempValorImpuestoDetallePago> getAll() throws Throwable {
        return TempValorImpuestoDetallePagoDaoService.selectAll(NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DETALLE_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempValorImpuestoDetallePago getId(@PathParam("id") Long id) throws Throwable {
        return TempValorImpuestoDetallePagoDaoService.selectById(id, NombreEntidadesPago.TEMP_VALOR_IMPUESTO_DETALLE_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempValorImpuestoDetallePago put(TempValorImpuestoDetallePago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_VALOR_IMPUESTO_DETALLE_PAGO");
        return TempValorImpuestoDetallePagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempValorImpuestoDetallePago post(TempValorImpuestoDetallePago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_VALOR_IMPUESTO_DETALLE_PAGO");
        return TempValorImpuestoDetallePagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_VALOR_IMPUESTO_DETALLE_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempValorImpuestoDetallePagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_VALOR_IMPUESTO_DETALLE_PAGO");
        TempValorImpuestoDetallePago elimina = new TempValorImpuestoDetallePago();
        TempValorImpuestoDetallePagoDaoService.remove(elimina, id);
    }
}
 