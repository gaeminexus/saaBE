package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.BancoDaoService;
import com.saa.ejb.tesoreria.service.BancoService;
import com.saa.model.tesoreria.Banco;
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

@Path("bnco")
public class BancoRest {

    @EJB
    private BancoDaoService bancoDaoService;

    @EJB
    private BancoService bancoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public BancoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Banco.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Banco> getAll() throws Throwable {
        return bancoDaoService.selectAll(NombreEntidadesTesoreria.BANCO);
    }

    /**
     * Recupera un Banco por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Banco getId(@PathParam("id") Long id) throws Throwable {
        return bancoDaoService.selectById(id, NombreEntidadesTesoreria.BANCO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Banco put(Banco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT BANCO");
        return bancoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Banco post(Banco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST BANCO");
        return bancoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of BancoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de BANCO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(bancoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un Banco por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE BANCO");
        Banco elimina = new Banco();
        bancoDaoService.remove(elimina, id);
    }
}
