package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.PerfilEconomicoDaoService;
import com.saa.ejb.credito.service.PerfilEconomicoService;
import com.saa.model.credito.PerfilEconomico;
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

@Path("prec")
public class PerfilEconomicoRest {

    @EJB
    private PerfilEconomicoDaoService perfilEconomicoDaoService;

    @EJB
    private PerfilEconomicoService perfilEconomicoService;

    @Context
    private UriInfo context;

    public PerfilEconomicoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PerfilEconomico> getAll() throws Throwable {
        return perfilEconomicoDaoService.selectAll(NombreEntidadesCredito.PERFIL_ECONOMICO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public PerfilEconomico getId(@PathParam("id") Long id) throws Throwable {
        return perfilEconomicoDaoService.selectById(id, NombreEntidadesCredito.PERFIL_ECONOMICO);
    }

    @PUT
    @Consumes("application/json")
    public PerfilEconomico put(PerfilEconomico registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PERFIL ECONOMICO");
        return perfilEconomicoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public PerfilEconomico post(PerfilEconomico registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PERFIL ECONOMICO");
        return perfilEconomicoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PERFIL ECONOMICO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(perfilEconomicoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - PERFIL ECONOMICO");
        PerfilEconomico elimina = new PerfilEconomico();
        perfilEconomicoDaoService.remove(elimina, id);
    }
}
