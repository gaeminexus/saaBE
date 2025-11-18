package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ProfesionDaoService;
import com.saa.ejb.credito.service.ProfesionService;
import com.saa.model.credito.Profesion;
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

@Path("prfs")
public class ProfesionRest {

    @EJB
    private ProfesionDaoService profesionDaoService;

    @EJB
    private ProfesionService profesionService;

    @Context
    private UriInfo context;

    public ProfesionRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Profesion> getAll() throws Throwable {
        return profesionDaoService.selectAll(NombreEntidadesCredito.PROFESION);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Profesion getId(@PathParam("id") Long id) throws Throwable {
        return profesionDaoService.selectById(id, NombreEntidadesCredito.PROFESION);
    }

    @PUT
    @Consumes("application/json")
    public Profesion put(Profesion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return profesionService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public Profesion post(Profesion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return profesionService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Profesión");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(profesionService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE");
        Profesion elimina = new Profesion();
        profesionDaoService.remove(elimina, id);
    }
}
