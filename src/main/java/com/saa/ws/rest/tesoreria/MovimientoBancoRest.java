package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.MovimientoBancoDaoService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.model.tesoreria.MovimientoBanco;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

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

@Path("mvcb")
public class MovimientoBancoRest {

    @EJB
    private MovimientoBancoDaoService movimientoBancoDaoService;

    @EJB
    private MovimientoBancoService movimientoBancoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public MovimientoBancoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de MovimientoBanco.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MovimientoBanco> getAll() throws Throwable {
        return movimientoBancoDaoService.selectAll(NombreEntidadesTesoreria.MOVIMIENTO_BANCO);
    }

    /**
     * Recupera un registro de MovimientoBanco por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public MovimientoBanco getId(@PathParam("id") Long id) throws Throwable {
        return movimientoBancoDaoService.selectById(id, NombreEntidadesTesoreria.MOVIMIENTO_BANCO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public MovimientoBanco put(MovimientoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT MOVIMIENTO BANCO");
        return movimientoBancoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public MovimientoBanco post(MovimientoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST MOVIMIENTO BANCO");
        return movimientoBancoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MovimientoBancoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MOVIMIENTO_BANCO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(movimientoBancoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de MovimientoBanco por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE MOVIMIENTO BANCO");
        MovimientoBanco elimina = new MovimientoBanco();
        movimientoBancoDaoService.remove(elimina, id);
    }
}
