package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.DireccionDaoService;
import com.saa.ejb.credito.service.DireccionService;
import com.saa.model.credito.Direccion;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("drcc")
public class DireccionRest {

    @EJB
    private DireccionDaoService direccionDaoService;

    @EJB
    private DireccionService direccionService;

    @Context
    private UriInfo context;

    public DireccionRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Direccion> getAll() throws Throwable {
        return direccionDaoService.selectAll(NombreEntidadesCredito.DIRECCION);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Direccion getId(@PathParam("id") Long id) throws Throwable {
        return direccionDaoService.selectById(id, NombreEntidadesCredito.DIRECCION);
    }

    @PUT
    @Consumes("application/json")
    public Direccion put(Direccion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - DRCC");
        return direccionService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public Direccion post(Direccion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - DRCC");
        return direccionService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DRCC");
        Response respuesta;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(direccionService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DRCC");
        Direccion elimina = new Direccion();
        direccionDaoService.remove(elimina, id);
    }

}
