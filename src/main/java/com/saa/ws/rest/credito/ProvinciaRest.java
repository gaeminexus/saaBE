package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ProvinciaDaoService;
import com.saa.ejb.credito.service.ProvinciaService;
import com.saa.model.credito.Provincia;
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

@Path("prvn")
public class ProvinciaRest {

    @EJB
    private ProvinciaDaoService provinciaDaoService;

    @EJB
    private ProvinciaService provinciaService;

    @Context
    private UriInfo context;

    public ProvinciaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Provincia> getAll() throws Throwable {
        return provinciaDaoService.selectAll(NombreEntidadesCredito.PROVINCIA);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Provincia getId(@PathParam("id") Long id) throws Throwable {
        return provinciaDaoService.selectById(id, NombreEntidadesCredito.PROVINCIA);
    }

    @PUT
    @Consumes("application/json")
    public Provincia put(Provincia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PROVINCIA");
        return provinciaService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public Provincia post(Provincia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PROVINCIA");
        return provinciaService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PROVINCIA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(provinciaService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - PROVINCIA");
        Provincia elimina = new Provincia();
        provinciaDaoService.remove(elimina, id);
    }

}
