package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.BancoExternoDaoService;
import com.saa.ejb.tesoreria.service.BancoExternoService;
import com.saa.model.tesoreria.BancoExterno;
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

@Path("bext")
public class BancoExternoRest {

    @EJB
    private BancoExternoDaoService bancoExternoDaoService;

    @EJB
    private BancoExternoService bancoExternoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public BancoExternoRest() {
        // Constructor vacío
    }

    /**
     * Obtiene todos los registros de BancoExterno.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<BancoExterno> getAll() throws Throwable {
        return bancoExternoDaoService.selectAll(NombreEntidadesTesoreria.BANCO_EXTERNO);
    }

    /**
     * Obtiene un BancoExterno por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public BancoExterno getId(@PathParam("id") Long id) throws Throwable {
        return bancoExternoDaoService.selectById(id, NombreEntidadesTesoreria.BANCO_EXTERNO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public BancoExterno put(BancoExterno registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT BANCO_EXTERNO");
        return bancoExternoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public BancoExterno post(BancoExterno registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST BANCO_EXTERNO");
        return bancoExternoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of BancoExternoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de BANCO_EXTERNO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(bancoExternoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE BANCO_EXTERNO");
        BancoExterno elimina = new BancoExterno();
        bancoExternoDaoService.remove(elimina, id);
    }
}
