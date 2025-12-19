package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempAprobacionXMontoDaoService;
import com.saa.ejb.cxp.service.TempAprobacionXMontoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempAprobacionXMonto;

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

@Path("tapx")
public class TempAprobacionXMontoRest {

    @EJB
    private TempAprobacionXMontoDaoService TempAprobacionXMontoDaoService;

    @EJB
    private TempAprobacionXMontoService TempAprobacionXMontoService;

    @Context
    private UriInfo context;

    public TempAprobacionXMontoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempAprobacionXMonto> getAll() throws Throwable {
        return TempAprobacionXMontoDaoService.selectAll(NombreEntidadesPago.TEMP_APROBACION_X_MONTO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempAprobacionXMonto getId(@PathParam("id") Long id) throws Throwable {
        return TempAprobacionXMontoDaoService.selectById(id, NombreEntidadesPago.TEMP_APROBACION_X_MONTO);
    }
    

    @PUT
    @Consumes("application/json")
    public TempAprobacionXMonto put(TempAprobacionXMonto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_APROBACION_X_MONTO");
        return TempAprobacionXMontoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempAprobacionXMonto post(TempAprobacionXMonto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_APROBACION_X_MONTO");
        return TempAprobacionXMontoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_APROBACION_X_MONTO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempAprobacionXMontoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_APROBACION_X_MONTO");
        TempAprobacionXMonto elimina = new TempAprobacionXMonto();
        TempAprobacionXMontoDaoService.remove(elimina, id);
    }
}
