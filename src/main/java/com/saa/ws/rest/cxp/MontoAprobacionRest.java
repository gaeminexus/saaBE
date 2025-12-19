package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.MontoAprobacionDaoService;
import com.saa.ejb.cxp.service.MontoAprobacionService;
import com.saa.model.cxp.MontoAprobacion;
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

@Path("mnap")
public class MontoAprobacionRest {

    @EJB
    private MontoAprobacionDaoService MontoAprobacionDaoService;

    @EJB
    private MontoAprobacionService MontoAprobacionService;

    @Context
    private UriInfo context;

    public MontoAprobacionRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MontoAprobacion> getAll() throws Throwable {
        return MontoAprobacionDaoService.selectAll(NombreEntidadesPago.MONTO_APROBACION);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public MontoAprobacion getId(@PathParam("id") Long id) throws Throwable {
        return MontoAprobacionDaoService.selectById(id, NombreEntidadesPago.MONTO_APROBACION);
    }
    

    @PUT
    @Consumes("application/json")
    public MontoAprobacion put(MontoAprobacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - MONTO_APROBACION");
        return MontoAprobacionService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public MontoAprobacion post(MontoAprobacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - MONTO_APROBACION");
        return MontoAprobacionService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MONTO_APROBACION");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(MontoAprobacionService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - MONTO_APROBACION");
        MontoAprobacion elimina = new MontoAprobacion();
        MontoAprobacionDaoService.remove(elimina, id);
    }
}
