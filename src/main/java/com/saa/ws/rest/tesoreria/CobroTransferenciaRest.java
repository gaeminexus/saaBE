package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CobroTransferenciaDaoService;
import com.saa.ejb.tesoreria.service.CobroTransferenciaService;
import com.saa.model.tesoreria.CobroTransferencia;
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

@Path("ctrn")
public class CobroTransferenciaRest {

    @EJB
    private CobroTransferenciaDaoService cobroTransferenciaDaoService;

    @EJB
    private CobroTransferenciaService cobroTransferenciaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroTransferenciaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroTransferencia.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CobroTransferencia> getAll() throws Throwable {
        return cobroTransferenciaDaoService.selectAll(NombreEntidadesTesoreria.COBRO_TRANSFERENCIA);
    }

    /**
     * Recupera un registro de CobroTransferencia por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CobroTransferencia getId(@PathParam("id") Long id) throws Throwable {
        return cobroTransferenciaDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_TRANSFERENCIA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CobroTransferencia put(CobroTransferencia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO TRANSFERENCIA");
        return cobroTransferenciaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CobroTransferencia post(CobroTransferencia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO TRANSFERENCIA");
        return cobroTransferenciaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CobroTransferenciaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de COBRO_TRANSFERENCIA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cobroTransferenciaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
    /**
     * Elimina un registro de CobroTransferencia por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO TRANSFERENCIA");
        CobroTransferencia elimina = new CobroTransferencia();
        cobroTransferenciaDaoService.remove(elimina, id);
    }
}
