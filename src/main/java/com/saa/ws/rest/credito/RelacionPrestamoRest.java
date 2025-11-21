package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.RelacionPrestamoDaoService;
import com.saa.ejb.credito.service.RelacionPrestamoService;
import com.saa.model.credito.RelacionPrestamo;
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

@Path("rlpr")
public class RelacionPrestamoRest {

    @EJB
    private RelacionPrestamoDaoService relacionPrestamoDaoService;

    @EJB
    private RelacionPrestamoService relacionPrestamoService;

    @Context
    private UriInfo context;

    public RelacionPrestamoRest() { }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<RelacionPrestamo> getAll() throws Throwable {
        return relacionPrestamoDaoService.selectAll(NombreEntidadesCredito.RELACION_PRESTAMO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public RelacionPrestamo getId(@PathParam("id") Long id) throws Throwable {
        return relacionPrestamoDaoService.selectById(id, NombreEntidadesCredito.RELACION_PRESTAMO);
    }

    @PUT
    @Consumes("application/json")
    public RelacionPrestamo put(RelacionPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - RLPR");
        return relacionPrestamoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public RelacionPrestamo post(RelacionPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - RLPR");
        return relacionPrestamoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de RLPR");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(relacionPrestamoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - RLPR");
        RelacionPrestamo elimina = new RelacionPrestamo();
        relacionPrestamoDaoService.remove(elimina, id);
    }
}
