package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoAdjuntoDaoService;
import com.saa.ejb.credito.service.TipoAdjuntoService;
import com.saa.model.credito.TipoAdjunto;
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

@Path("tpdj")
public class TipoAdjuntoRest {

    @EJB
    private TipoAdjuntoDaoService tipoAdjuntoDaoService;

    @EJB
    private TipoAdjuntoService tipoAdjuntoService;

    @Context
    private UriInfo context;

    public TipoAdjuntoRest() {
        // Constructor vacío
    }

    /**
     * GET - Obtener todos los registros
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TipoAdjunto> getAll() throws Throwable {
        return tipoAdjuntoDaoService.selectAll(NombreEntidadesCredito.TIPO_ADJUNTO);
    }

    /**
     * GET - Obtener por ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TipoAdjunto getId(@PathParam("id") Long id) throws Throwable {
        return tipoAdjuntoDaoService.selectById(id, NombreEntidadesCredito.TIPO_ADJUNTO);
    }

    /**
     * PUT - Actualizar o crear registro
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoAdjunto put(TipoAdjunto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return tipoAdjuntoService.saveSingle(registro);
    }

    /**
     * POST - Crear registro
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoAdjunto post(TipoAdjunto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return tipoAdjuntoService.saveSingle(registro);
    }

    /**
     * POST - Select by criteria
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Tipo Adjunto");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoAdjuntoService.selectByCriteria(registros))
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
        TipoAdjunto elimina = new TipoAdjunto();
        tipoAdjuntoDaoService.remove(elimina, id);
    }
}
