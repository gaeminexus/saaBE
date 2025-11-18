package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoParticipeDaoService;
import com.saa.ejb.credito.service.TipoParticipeService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TipoParticipe;

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

@Path("tppc")
public class TipoParticipeRest {

    @EJB
    private TipoParticipeDaoService tipoParticipeDaoService;

    @EJB
    private TipoParticipeService tipoParticipeService;

    @Context
    private UriInfo context;

    public TipoParticipeRest() {
        // Constructor vacío
    }

    /**
     * GET - Obtener todos los registros
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TipoParticipe> getAll() throws Throwable {
        return tipoParticipeDaoService.selectAll(NombreEntidadesCredito.TIPO_PARTICIPE);
    }

    /**
     * GET - Obtener por ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TipoParticipe getId(@PathParam("id") Long id) throws Throwable {
        return tipoParticipeDaoService.selectById(id, NombreEntidadesCredito.TIPO_PARTICIPE);
    }

    /**
     * PUT - Actualizar o crear registro
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoParticipe put(TipoParticipe registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return tipoParticipeService.saveSingle(registro);
    }

    /**
     * POST - Crear registro
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoParticipe post(TipoParticipe registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return tipoParticipeService.saveSingle(registro);
    }

    /**
     * POST - Select by criteria
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Tipo Participe");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoParticipeService.selectByCriteria(registros))
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

    /**
     * DELETE - Eliminar registro por ID
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        TipoParticipe elimina = new TipoParticipe();
        tipoParticipeDaoService.remove(elimina, id);
    }
}
