package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.EstadoPrestamoDaoService;
import com.saa.ejb.credito.service.EstadoPrestamoService;
import com.saa.model.credito.EstadoPrestamo;
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

@Path("esps")
public class EstadoPrestamoRest {

    @EJB
    private EstadoPrestamoDaoService estadoPrestamoDaoService;

    @EJB
    private EstadoPrestamoService estadoPrestamoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public EstadoPrestamoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of EstadoPrestamoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<EstadoPrestamo> getAll() throws Throwable {
        return estadoPrestamoDaoService.selectAll(NombreEntidadesCredito.ESTADO_PRESTAMO);
    }

    /**
     * Retrieves representation of an instance of EstadoPrestamoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public EstadoPrestamo getId(@PathParam("id") Long id) throws Throwable {
        return estadoPrestamoDaoService.selectById(id, NombreEntidadesCredito.ESTADO_PRESTAMO);
    }

    /**
     * PUT method for updating or creating an instance of EstadoPrestamoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public EstadoPrestamo put(EstadoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return estadoPrestamoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of EstadoPrestamoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public EstadoPrestamo post(EstadoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return estadoPrestamoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of EstadoPrestamoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Estado Prestamo");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(estadoPrestamoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of EstadoPrestamoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        EstadoPrestamo elimina = new EstadoPrestamo();
        estadoPrestamoDaoService.remove(elimina, id);
    }

}