package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ComentarioDaoService;
import com.saa.ejb.credito.service.ComentarioService;
import com.saa.model.credito.Comentario;
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

@Path("cmnt")
public class ComentarioRest {

    @EJB
    private ComentarioDaoService comentarioDaoService;

    @EJB
    private ComentarioService comentarioService;

    @Context
    private UriInfo context;

    public ComentarioRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Comentario> getAll() throws Throwable {
        return comentarioDaoService.selectAll(NombreEntidadesCredito.COMENTARIO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Comentario getId(@PathParam("id") Long id) throws Throwable {
        return comentarioDaoService.selectById(id, NombreEntidadesCredito.COMENTARIO);
    }

    @PUT
    @Consumes("application/json")
    public Comentario put(Comentario registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - COMENTARIO");
        return comentarioService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public Comentario post(Comentario registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - COMENTARIO");
        return comentarioService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de COMENTARIO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(comentarioService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - COMENTARIO");
        Comentario elimina = new Comentario();
        comentarioDaoService.remove(elimina, id);
    }
}
