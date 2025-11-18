package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.NivelEstudioDaoService;
import com.saa.ejb.credito.service.NivelEstudioService;
import com.saa.model.credito.NivelEstudio;
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

@Path("nvls")
public class NivelEstudioRest {

    @EJB
    private NivelEstudioDaoService nivelEstudioDaoService;

    @EJB
    private NivelEstudioService nivelEstudioService;

    @Context
    private UriInfo context;

    public NivelEstudioRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<NivelEstudio> getAll() throws Throwable {
        return nivelEstudioDaoService.selectAll(NombreEntidadesCredito.NIVEL_ESTUDIO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public NivelEstudio getId(@PathParam("id") Long id) throws Throwable {
        return nivelEstudioDaoService.selectById(id, NombreEntidadesCredito.NIVEL_ESTUDIO);
    }

    @PUT
    @Consumes("application/json")
    public NivelEstudio put(NivelEstudio registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return nivelEstudioService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public NivelEstudio post(NivelEstudio registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return nivelEstudioService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Nivel Estudio");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(nivelEstudioService.selectByCriteria(registros))
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
        NivelEstudio elimina = new NivelEstudio();
        nivelEstudioDaoService.remove(elimina, id);
    }
}
