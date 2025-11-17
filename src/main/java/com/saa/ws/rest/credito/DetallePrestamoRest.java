package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.DetallePrestamoDaoService;
import com.saa.ejb.credito.service.DetallePrestamoService;
import com.saa.model.credito.DetallePrestamo;
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

@Path("dtpr")
public class DetallePrestamoRest {

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private DetallePrestamoService detallePrestamoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetallePrestamoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetallePrestamoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetallePrestamo> getAll() throws Throwable {
        return detallePrestamoDaoService.selectAll(NombreEntidadesCredito.DETALLE_PRESTAMO);
    }

    /**
     * Retrieves representation of an instance of DetallePrestamoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DetallePrestamo getId(@PathParam("id") Long id) throws Throwable {
        return detallePrestamoDaoService.selectById(id, NombreEntidadesCredito.DETALLE_PRESTAMO);
    }

    /**
     * PUT method for updating or creating an instance of DetallePrestamoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetallePrestamo put(DetallePrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detallePrestamoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetallePrestamoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetallePrestamo post(DetallePrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detallePrestamoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetallePrestamoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Detalle Prestamo");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detallePrestamoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of DetallePrestamoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetallePrestamo elimina = new DetallePrestamo();
        detallePrestamoDaoService.remove(elimina, id);
    }

}