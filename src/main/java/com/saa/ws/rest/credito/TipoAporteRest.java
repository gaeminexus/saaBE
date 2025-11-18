package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoAporteDaoService;
import com.saa.ejb.credito.service.TipoAporteService;
import com.saa.model.credito.TipoAporte;
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

@Path("tpap")
public class TipoAporteRest {

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private TipoAporteService tipoAporteService;

    @Context
    private UriInfo context;

    public TipoAporteRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoAporte> getAll() throws Throwable {
        return tipoAporteDaoService.selectAll(NombreEntidadesCredito.TIPO_APORTE);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TipoAporte getId(@PathParam("id") Long id) throws Throwable {
        return tipoAporteDaoService.selectById(id, NombreEntidadesCredito.TIPO_APORTE);
    }

    @PUT
    @Consumes("application/json")
    public TipoAporte put(TipoAporte registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TipoAporte");
        return tipoAporteService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public TipoAporte post(TipoAporte registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TipoAporte");
        return tipoAporteService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria TipoAporte");
        Response respuesta;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoAporteService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE TipoAporte");
        TipoAporte elimina = new TipoAporte();
        tipoAporteDaoService.remove(elimina, id);
    }

}
