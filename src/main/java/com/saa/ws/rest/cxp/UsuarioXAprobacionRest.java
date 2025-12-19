package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.UsuarioXAprobacionDaoService;
import com.saa.ejb.cxp.service.UsuarioXAprobacionService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.UsuarioXAprobacion;

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

@Path("uxap")
public class UsuarioXAprobacionRest {

    @EJB
    private UsuarioXAprobacionDaoService UsuarioXAprobacionDaoService;

    @EJB
    private UsuarioXAprobacionService UsuarioXAprobacionService;

    @Context
    private UriInfo context;

    public UsuarioXAprobacionRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<UsuarioXAprobacion> getAll() throws Throwable {
        return UsuarioXAprobacionDaoService.selectAll(NombreEntidadesPago.USUARIO_X_APROBACION);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public UsuarioXAprobacion getId(@PathParam("id") Long id) throws Throwable {
        return UsuarioXAprobacionDaoService.selectById(id, NombreEntidadesPago.USUARIO_X_APROBACION);
    }
    

    @PUT
    @Consumes("application/json")
    public UsuarioXAprobacion put(UsuarioXAprobacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - USUARIO_X_APROBACION");
        return UsuarioXAprobacionService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public UsuarioXAprobacion post(UsuarioXAprobacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - USUARIO_X_APROBACION");
        return UsuarioXAprobacionService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de USUARIO_X_APROBACION");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(UsuarioXAprobacionService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - USUARIO_X_APROBACION");
        UsuarioXAprobacion elimina = new UsuarioXAprobacion();
        UsuarioXAprobacionDaoService.remove(elimina, id);
    }
}
 