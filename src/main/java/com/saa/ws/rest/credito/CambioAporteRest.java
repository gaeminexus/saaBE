package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.CambioAporteDaoService;
import com.saa.ejb.credito.service.CambioAporteService;
import com.saa.model.credito.CambioAporte;
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

@Path("cmbp")
public class CambioAporteRest {

    @EJB
    private CambioAporteDaoService cambioAporteDaoService;

    @EJB
    private CambioAporteService cambioAporteService;

    @Context
    private UriInfo context;

    public CambioAporteRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CambioAporte> getAll() throws Throwable {
        return cambioAporteDaoService.selectAll(NombreEntidadesCredito.CAMBIO_APORTE);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CambioAporte getId(@PathParam("id") Long id) throws Throwable {
        return cambioAporteDaoService.selectById(id, NombreEntidadesCredito.CAMBIO_APORTE);
    }

    @PUT
    @Consumes("application/json")
    public CambioAporte put(CambioAporte registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - CAMBIO APORTE");
        return cambioAporteService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public CambioAporte post(CambioAporte registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - CAMBIO APORTE");
        return cambioAporteService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CMBP");
        Response respuesta;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(cambioAporteService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - CAMBIO APORTE");
        CambioAporte elimina = new CambioAporte();
        cambioAporteDaoService.remove(elimina, id);
    }

}
