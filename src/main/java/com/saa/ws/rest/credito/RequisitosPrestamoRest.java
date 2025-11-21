package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.RequisitosPrestamoDaoService;
import com.saa.ejb.credito.service.RequisitosPrestamoService;
import com.saa.model.credito.RequisitosPrestamo;
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

@Path("rqpr")
public class RequisitosPrestamoRest {

    @EJB
    private RequisitosPrestamoDaoService requisitosPrestamoDaoService;

    @EJB
    private RequisitosPrestamoService requisitosPrestamoService;

    @Context
    private UriInfo context;

    public RequisitosPrestamoRest() { }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<RequisitosPrestamo> getAll() throws Throwable {
        return requisitosPrestamoDaoService.selectAll(NombreEntidadesCredito.REQUISITOS_PRESTAMO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public RequisitosPrestamo getId(@PathParam("id") Long id) throws Throwable {
        return requisitosPrestamoDaoService.selectById(id, NombreEntidadesCredito.REQUISITOS_PRESTAMO);
    }

    @PUT
    @Consumes("application/json")
    public RequisitosPrestamo put(RequisitosPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - RQPR");
        return requisitosPrestamoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public RequisitosPrestamo post(RequisitosPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - RQPR");
        return requisitosPrestamoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de RQPR");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(requisitosPrestamoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - RQPR");
        RequisitosPrestamo elimina = new RequisitosPrestamo();
        requisitosPrestamoDaoService.remove(elimina, id);
    }
}
