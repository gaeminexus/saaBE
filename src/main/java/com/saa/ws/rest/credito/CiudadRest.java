package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.CiudadDaoService;
import com.saa.ejb.credito.service.CiudadService;
import com.saa.model.credito.Ciudad;
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

@Path("cddd")
public class CiudadRest {

    @EJB
    private CiudadDaoService ciudadDaoService;

    @EJB
    private CiudadService ciudadService;

    @Context
    private UriInfo context;

    public CiudadRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Ciudad> getAll() throws Throwable {
        return ciudadDaoService.selectAll(NombreEntidadesCredito.CIUDAD);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Ciudad getId(@PathParam("id") Long id) throws Throwable {
        return ciudadDaoService.selectById(id, NombreEntidadesCredito.CIUDAD);
    }

    @PUT
    @Consumes("application/json")
    public Ciudad put(Ciudad registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - CIUDAD");
        return ciudadService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public Ciudad post(Ciudad registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - CIUDAD");
        return ciudadService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CIUDAD");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(ciudadService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - CIUDAD");
        Ciudad elimina = new Ciudad();
        ciudadDaoService.remove(elimina, id);
    }
}
