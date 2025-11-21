package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.HistorialSueldoDaoService;
import com.saa.ejb.credito.service.HistorialSueldoService;
import com.saa.model.credito.HistorialSueldo;
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

@Path("hstr")
public class HistorialSueldoRest {

    @EJB
    private HistorialSueldoDaoService historialSueldoDaoService;

    @EJB
    private HistorialSueldoService historialSueldoService;

    @Context
    private UriInfo context;

    public HistorialSueldoRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<HistorialSueldo> getAll() throws Throwable {
        return historialSueldoDaoService.selectAll(NombreEntidadesCredito.HISTORIAL_SUELDO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public HistorialSueldo getId(@PathParam("id") Long id) throws Throwable {
        return historialSueldoDaoService.selectById(id, NombreEntidadesCredito.HISTORIAL_SUELDO);
    }

    @PUT
    @Consumes("application/json")
    public HistorialSueldo put(HistorialSueldo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - HSTR");
        return historialSueldoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public HistorialSueldo post(HistorialSueldo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - HSTR");
        return historialSueldoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de HSTR");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                .entity(historialSueldoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - HSTR");
        HistorialSueldo elimina = new HistorialSueldo();
        historialSueldoDaoService.remove(elimina, id);
    }
}
