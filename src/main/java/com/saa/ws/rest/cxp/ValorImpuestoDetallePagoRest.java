package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.ValorImpuestoDetallePagoDaoService;
import com.saa.ejb.cxp.service.ValorImpuestoDetallePagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ValorImpuestoDetallePago;

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

@Path("vitp")
public class ValorImpuestoDetallePagoRest {

    @EJB
    private ValorImpuestoDetallePagoDaoService ValorImpuestoDetallePagoDaoService;

    @EJB
    private ValorImpuestoDetallePagoService ValorImpuestoDetallePagoService;

    @Context
    private UriInfo context;

    public ValorImpuestoDetallePagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ValorImpuestoDetallePago> getAll() throws Throwable {
        return ValorImpuestoDetallePagoDaoService.selectAll(NombreEntidadesPago.VALOR_IMPUESTO_DETALLE_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ValorImpuestoDetallePago getId(@PathParam("id") Long id) throws Throwable {
        return ValorImpuestoDetallePagoDaoService.selectById(id, NombreEntidadesPago.VALOR_IMPUESTO_DETALLE_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public ValorImpuestoDetallePago put(ValorImpuestoDetallePago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - VALOR_IMPUESTO_DETALLE_PAGO");
        return ValorImpuestoDetallePagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public ValorImpuestoDetallePago post(ValorImpuestoDetallePago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - VALOR_IMPUESTO_DETALLE_PAGO");
        return ValorImpuestoDetallePagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de VALOR_IMPUESTO_DETALLE_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(ValorImpuestoDetallePagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - VALOR_IMPUESTO_DETALLE_PAGO");
        ValorImpuestoDetallePago elimina = new ValorImpuestoDetallePago();
        ValorImpuestoDetallePagoDaoService.remove(elimina, id);
    }
}
 