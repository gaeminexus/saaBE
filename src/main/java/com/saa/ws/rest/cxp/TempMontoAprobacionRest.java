package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempMontoAprobacionDaoService;
import com.saa.ejb.cxp.service.TempMontoAprobacionService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempMontoAprobacion;

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

@Path("tmna")
public class TempMontoAprobacionRest {

    @EJB
    private TempMontoAprobacionDaoService TempMontoAprobacionDaoService;

    @EJB
    private TempMontoAprobacionService TempMontoAprobacionService;

    @Context
    private UriInfo context;

    public TempMontoAprobacionRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempMontoAprobacion> getAll() throws Throwable {
        return TempMontoAprobacionDaoService.selectAll(NombreEntidadesPago.TEMP_MONTO_APROBACION);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempMontoAprobacion getId(@PathParam("id") Long id) throws Throwable {
        return TempMontoAprobacionDaoService.selectById(id, NombreEntidadesPago.TEMP_MONTO_APROBACION);
    }
    

    @PUT
    @Consumes("application/json")
    public TempMontoAprobacion put(TempMontoAprobacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_MONTO_APROBACION");
        return TempMontoAprobacionService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempMontoAprobacion post(TempMontoAprobacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_MONTO_APROBACION");
        return TempMontoAprobacionService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_MONTO_APROBACION");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempMontoAprobacionService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_MONTO_APROBACION");
        TempMontoAprobacion elimina = new TempMontoAprobacion();
        TempMontoAprobacionDaoService.remove(elimina, id);
    }
}
 