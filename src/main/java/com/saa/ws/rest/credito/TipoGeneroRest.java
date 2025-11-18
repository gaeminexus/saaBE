package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoGeneroDaoService;
import com.saa.ejb.credito.service.TipoGeneroService;
import com.saa.model.credito.TipoGenero;
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

@Path("tpgn")
public class TipoGeneroRest {

    @EJB
    private TipoGeneroDaoService tipoGeneroDaoService;

    @EJB
    private TipoGeneroService tipoGeneroService;

    @Context
    private UriInfo context;

    public TipoGeneroRest() {
        // Constructor vacío
    }

    /**
     * GET - Obtener todos los registros
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TipoGenero> getAll() throws Throwable {
        return tipoGeneroDaoService.selectAll(NombreEntidadesCredito.TIPO_GENERO);
    }

    /**
     * GET - Obtener por ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TipoGenero getId(@PathParam("id") Long id) throws Throwable {
        return tipoGeneroDaoService.selectById(id, NombreEntidadesCredito.TIPO_GENERO);
    }

    /**
     * PUT - Actualizar o crear registro
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoGenero put(TipoGenero registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return tipoGeneroService.saveSingle(registro);
    }

    /**
     * POST - Crear registro
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoGenero post(TipoGenero registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return tipoGeneroService.saveSingle(registro);
    }

    /**
     * POST - Select by criteria
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Tipo Genero");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoGeneroService.selectByCriteria(registros))
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
        TipoGenero elimina = new TipoGenero();
        tipoGeneroDaoService.remove(elimina, id);
    }
}
