package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.ImpuestoXGrupoPagoDaoService;
import com.saa.ejb.cxp.service.ImpuestoXGrupoPagoService;
import com.saa.model.cxp.ImpuestoXGrupoPago;
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

@Path("ixgp")
public class ImpuestoXGrupoPagoRest {

    @EJB
    private ImpuestoXGrupoPagoDaoService ImpuestoXGrupoPagoDaoService;

    @EJB
    private ImpuestoXGrupoPagoService ImpuestoXGrupoPagoService;

    @Context
    private UriInfo context;

    public ImpuestoXGrupoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ImpuestoXGrupoPago> getAll() throws Throwable {
        return ImpuestoXGrupoPagoDaoService.selectAll(NombreEntidadesPago.IMPUESTO_X_GRUPO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ImpuestoXGrupoPago getId(@PathParam("id") Long id) throws Throwable {
        return ImpuestoXGrupoPagoDaoService.selectById(id, NombreEntidadesPago.IMPUESTO_X_GRUPO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public ImpuestoXGrupoPago put(ImpuestoXGrupoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - IMPUESTO_X_GRUPO_PAGO");
        return ImpuestoXGrupoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public ImpuestoXGrupoPago post(ImpuestoXGrupoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - IMPUESTO_X_GRUPO_PAGO");
        return ImpuestoXGrupoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de IMPUESTO_X_GRUPO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(ImpuestoXGrupoPagoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - IMPUESTO_X_GRUPO_PAGO");
        ImpuestoXGrupoPago elimina = new ImpuestoXGrupoPago();
        ImpuestoXGrupoPagoDaoService.remove(elimina, id);
    }
}
