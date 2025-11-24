package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.PagoDaoService;
import com.saa.ejb.tesoreria.service.PagoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.Pago;

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

@Path("pgss")
public class PagoRest {

    @EJB
    private PagoDaoService pagoDaoService;

    @EJB
    private PagoService pagoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public PagoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Pago.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Pago> getAll() throws Throwable {
        return pagoDaoService.selectAll(NombreEntidadesTesoreria.PAGO);
    }

    /**
     * Recupera un registro de Pago por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Pago getId(@PathParam("id") Long id) throws Throwable {
        return pagoDaoService.selectById(id, NombreEntidadesTesoreria.PAGO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Pago put(Pago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT PAGO");
        return pagoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Pago post(Pago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST PAGO");
        return pagoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PagoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PAGO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(pagoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Pago por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE PAGO");
        Pago elimina = new Pago();
        pagoDaoService.remove(elimina, id);
    }
}
