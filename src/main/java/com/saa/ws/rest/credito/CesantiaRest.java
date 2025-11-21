package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.CesantiaDaoService;
import com.saa.ejb.credito.service.CesantiaService;
import com.saa.model.credito.Cesantia;
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

@Path("csnt")
public class CesantiaRest {

    @EJB
    private CesantiaDaoService cesantiaDaoService;

    @EJB
    private CesantiaService cesantiaService;

    @Context
    private UriInfo context;

    public CesantiaRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Cesantia> getAll() throws Throwable {
        return cesantiaDaoService.selectAll(NombreEntidadesCredito.CESANTIA);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Cesantia getId(@PathParam("id") Long id) throws Throwable {
        return cesantiaDaoService.selectById(id, NombreEntidadesCredito.CESANTIA);
    }

    @PUT
    @Consumes("application/json")
    public Cesantia put(Cesantia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - CESANTIA");
        return cesantiaService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public Cesantia post(Cesantia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - CESANTIA");
        return cesantiaService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CSNT");
        Response respuesta;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(cesantiaService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - CESANTIA");
        Cesantia elimina = new Cesantia();
        cesantiaDaoService.remove(elimina, id);
    }

}
