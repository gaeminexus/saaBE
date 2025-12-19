package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.ProposicionPagoXCuotaDaoService;
import com.saa.ejb.cxp.service.ProposicionPagoXCuotaService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ProposicionPagoXCuota;

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

@Path("prpd")
public class ProposicionPagoXCuotaRest {

    @EJB
    private ProposicionPagoXCuotaDaoService ProposicionPagoXCuotaDaoService;

    @EJB
    private ProposicionPagoXCuotaService ProposicionPagoXCuotaService;

    @Context
    private UriInfo context;

    public ProposicionPagoXCuotaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ProposicionPagoXCuota> getAll() throws Throwable {
        return ProposicionPagoXCuotaDaoService.selectAll(NombreEntidadesPago.PROPOSICION_PAGO_X_CUOTA);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ProposicionPagoXCuota getId(@PathParam("id") Long id) throws Throwable {
        return ProposicionPagoXCuotaDaoService.selectById(id, NombreEntidadesPago.PROPOSICION_PAGO_X_CUOTA);
    }
    

    @PUT
    @Consumes("application/json")
    public ProposicionPagoXCuota put(ProposicionPagoXCuota registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PROPOSICION_PAGO_X_CUOTA");
        return ProposicionPagoXCuotaService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public ProposicionPagoXCuota post(ProposicionPagoXCuota registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PROPOSICION_PAGO_X_CUOTA");
        return ProposicionPagoXCuotaService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PROPOSICION_PAGO_X_CUOTA");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(ProposicionPagoXCuotaService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - PROPOSICION_PAGO_X_CUOTA");
        ProposicionPagoXCuota elimina = new ProposicionPagoXCuota();
        ProposicionPagoXCuotaDaoService.remove(elimina, id);
    }
}
