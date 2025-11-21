package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.CxcParticipeDaoService;
import com.saa.ejb.credito.service.CxcParticipeService;
import com.saa.model.credito.CxcParticipe;
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

@Path("cxcp")
public class CxcParticipeRest {

    @EJB
    private CxcParticipeDaoService cxcParticipeDaoService;

    @EJB
    private CxcParticipeService cxcParticipeService;

    @Context
    private UriInfo context;

    public CxcParticipeRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CxcParticipe> getAll() throws Throwable {
        return cxcParticipeDaoService.selectAll(NombreEntidadesCredito.CXC_PARTICIPE);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CxcParticipe getId(@PathParam("id") Long id) throws Throwable {
        return cxcParticipeDaoService.selectById(id, NombreEntidadesCredito.CXC_PARTICIPE);
    }

    @PUT
    @Consumes("application/json")
    public CxcParticipe put(CxcParticipe registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - CXCP");
        return cxcParticipeService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public CxcParticipe post(CxcParticipe registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - CXCP");
        return cxcParticipeService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CXCP");
        Response respuesta;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(cxcParticipeService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - CXCP");
        CxcParticipe elimina = new CxcParticipe();
        cxcParticipeDaoService.remove(elimina, id);
    }

}
