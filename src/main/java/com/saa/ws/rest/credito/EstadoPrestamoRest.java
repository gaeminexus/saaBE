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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<EstadoPrestamo> lista = estadoPrestamoDaoService.selectAll(NombreEntidadesCredito.ESTADO_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener estados de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            EstadoPrestamo estado = estadoPrestamoDaoService.selectById(id, NombreEntidadesCredito.ESTADO_PRESTAMO);
            if (estado == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("EstadoPrestamo con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(estado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener estado de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(EstadoPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            EstadoPrestamo resultado = estadoPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar estado de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(EstadoPrestamo registro) {
        System.out.println("LLEGA AL SERVICIO");
        try {
            EstadoPrestamo resultado = estadoPrestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear estado de préstamo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
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
