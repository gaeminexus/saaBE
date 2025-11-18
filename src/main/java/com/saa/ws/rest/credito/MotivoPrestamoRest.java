package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.MotivoPrestamoDaoService;
import com.saa.ejb.credito.service.MotivoPrestamoService;
import com.saa.model.credito.MotivoPrestamo;
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

@Path("mtvp")
public class MotivoPrestamoRest {

    @EJB
    private MotivoPrestamoDaoService motivoPrestamoDaoService;

    @EJB
    private MotivoPrestamoService motivoPrestamoService;

    @Context
    private UriInfo context;

    public MotivoPrestamoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MotivoPrestamo> getAll() throws Throwable {
        return motivoPrestamoDaoService.selectAll(NombreEntidadesCredito.MOTIVO_PRESTAMO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public MotivoPrestamo getId(@PathParam("id") Long id) throws Throwable {
        return motivoPrestamoDaoService.selectById(id, NombreEntidadesCredito.MOTIVO_PRESTAMO);
    }

    @PUT
    @Consumes("application/json")
    public MotivoPrestamo put(MotivoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return motivoPrestamoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public MotivoPrestamo post(MotivoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return motivoPrestamoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Motivo Prestamo");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(motivoPrestamoService.selectByCriteria(registros))
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
        MotivoPrestamo elimina = new MotivoPrestamo();
        motivoPrestamoDaoService.remove(elimina, id);
    }
}
