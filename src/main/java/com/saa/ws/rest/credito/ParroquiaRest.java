package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ParroquiaDaoService;
import com.saa.ejb.credito.service.ParroquiaService;
import com.saa.model.credito.Parroquia;
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

@Path("prrq")
public class ParroquiaRest {

    @EJB
    private ParroquiaDaoService parroquiaDaoService;

    @EJB
    private ParroquiaService parroquiaService;

    @Context
    private UriInfo context;

    public ParroquiaRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Parroquia> getAll() throws Throwable {
        return parroquiaDaoService.selectAll(NombreEntidadesCredito.PARROQUIA);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Parroquia getId(@PathParam("id") Long id) throws Throwable {
        return parroquiaDaoService.selectById(id, NombreEntidadesCredito.PARROQUIA);
    }

    @PUT
    @Consumes("application/json")
    public Parroquia put(Parroquia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PRRQ");
        return parroquiaService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public Parroquia post(Parroquia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PRRQ");
        return parroquiaService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PRRQ");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                .entity(parroquiaService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - PRRQ");
        Parroquia elimina = new Parroquia();
        parroquiaDaoService.remove(elimina, id);
    }
}
