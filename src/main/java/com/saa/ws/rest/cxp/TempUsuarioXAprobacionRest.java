package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempUsuarioXAprobacionDaoService;
import com.saa.ejb.cxp.service.TempUsuarioXAprobacionService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempUsuarioXAprobacion;

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

@Path("tuxa")
public class TempUsuarioXAprobacionRest {

    @EJB
    private TempUsuarioXAprobacionDaoService TempUsuarioXAprobacionDaoService;

    @EJB
    private TempUsuarioXAprobacionService TempUsuarioXAprobacionService;

    @Context
    private UriInfo context;

    public TempUsuarioXAprobacionRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempUsuarioXAprobacion> getAll() throws Throwable {
        return TempUsuarioXAprobacionDaoService.selectAll(NombreEntidadesPago.TEMP_USUARIO_X_APROBACION);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempUsuarioXAprobacion getId(@PathParam("id") Long id) throws Throwable {
        return TempUsuarioXAprobacionDaoService.selectById(id, NombreEntidadesPago.TEMP_USUARIO_X_APROBACION);
    }
    

    @PUT
    @Consumes("application/json")
    public TempUsuarioXAprobacion put(TempUsuarioXAprobacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_USUARIO_X_APROBACION");
        return TempUsuarioXAprobacionService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TempUsuarioXAprobacion post(TempUsuarioXAprobacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_USUARIO_X_APROBACION");
        return TempUsuarioXAprobacionService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_USUARIO_X_APROBACION");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(TempUsuarioXAprobacionService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_USUARIO_X_APROBACION");
        TempUsuarioXAprobacion elimina = new TempUsuarioXAprobacion();
        TempUsuarioXAprobacionDaoService.remove(elimina, id);
    }
}
 