package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.MoraPrestamoDaoService;
import com.saa.ejb.credito.service.MoraPrestamoService;
import com.saa.model.credito.MoraPrestamo;
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

@Path("mrpr")
public class MoraPrestamoRest {

    @EJB
    private MoraPrestamoDaoService moraPrestamoDaoService;

    @EJB
    private MoraPrestamoService moraPrestamoService;

    @Context
    private UriInfo context;

    public MoraPrestamoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MoraPrestamo> getAll() throws Throwable {
        return moraPrestamoDaoService.selectAll(NombreEntidadesCredito.MORA_PRESTAMO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public MoraPrestamo getId(@PathParam("id") Long id) throws Throwable {
        return moraPrestamoDaoService.selectById(id, NombreEntidadesCredito.MORA_PRESTAMO);
    }

    @PUT
    @Consumes("application/json")
    public MoraPrestamo put(MoraPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - MORAPRESTAMO");
        return moraPrestamoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public MoraPrestamo post(MoraPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - MORAPRESTAMO");
        return moraPrestamoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MORAPRESTAMO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(moraPrestamoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - MORAPRESTAMO");
        MoraPrestamo elimina = new MoraPrestamo();
        moraPrestamoDaoService.remove(elimina, id);
    }

}
