package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.ChequeDaoService;
import com.saa.ejb.tesoreria.service.ChequeService;
import com.saa.model.tesoreria.Cheque;
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

@Path("dtch")
public class ChequeRest {

    @EJB
    private ChequeDaoService chequeDaoService;

    @EJB
    private ChequeService chequeService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ChequeRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Cheque.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Cheque> getAll() throws Throwable {
        return chequeDaoService.selectAll(NombreEntidadesTesoreria.CHEQUE);
    }

    /**
     * Recupera un registro de Cheque por ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Cheque getId(@PathParam("id") Long id) throws Throwable {
        return chequeDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUE);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Cheque put(Cheque registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CHEQUE");
        return chequeService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Cheque post(Cheque registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CHEQUE");
        return chequeService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ChequeRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CHEQUE");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(chequeService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Cheque por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CHEQUE");
        Cheque elimina = new Cheque();
        chequeDaoService.remove(elimina, id);
    }
}
