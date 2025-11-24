package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.SaldoBancoDaoService;
import com.saa.ejb.tesoreria.service.SaldoBancoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.SaldoBanco;

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

@Path("slcb")
public class SaldoBancoRest {

    @EJB
    private SaldoBancoDaoService saldoBancoDaoService;

    @EJB
    private SaldoBancoService saldoBancoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public SaldoBancoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de SaldoBanco.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<SaldoBanco> getAll() throws Throwable {
        return saldoBancoDaoService.selectAll(NombreEntidadesTesoreria.SALDO_BANCO);
    }

    /**
     * Recupera un registro de SaldoBanco por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public SaldoBanco getId(@PathParam("id") Long id) throws Throwable {
        return saldoBancoDaoService.selectById(id, NombreEntidadesTesoreria.SALDO_BANCO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public SaldoBanco put(SaldoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT SALDO_BANCO");
        return saldoBancoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public SaldoBanco post(SaldoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST SALDO_BANCO");
        return saldoBancoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of SaldoBancoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de SALDO_BANCO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(saldoBancoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de SaldoBanco por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE SALDO_BANCO");
        SaldoBanco elimina = new SaldoBanco();
        saldoBancoDaoService.remove(elimina, id);
    }
}

