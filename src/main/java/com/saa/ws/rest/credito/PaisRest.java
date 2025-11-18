package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.PaisDaoService;
import com.saa.ejb.credito.service.PaisService;
import com.saa.model.credito.Pais;
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

@Path("psss")
public class PaisRest {

    @EJB
    private PaisDaoService paisDaoService;

    @EJB
    private PaisService paisService;

    @Context
    private UriInfo context;

    public PaisRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Pais> getAll() throws Throwable {
        return paisDaoService.selectAll(NombreEntidadesCredito.PAIS);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Pais getId(@PathParam("id") Long id) throws Throwable {
        return paisDaoService.selectById(id, NombreEntidadesCredito.PAIS);
    }

    @PUT
    @Consumes("application/json")
    public Pais put(Pais registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT Pais");
        return paisService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public Pais post(Pais registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST Pais");
        return paisService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria Pais");
        Response respuesta;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(paisService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE Pais");
        Pais elimina = new Pais();
        paisDaoService.remove(elimina, id);
    }

}
