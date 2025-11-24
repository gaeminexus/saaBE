package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CajaLogicaPorCajaFisicaDaoService;
import com.saa.ejb.tesoreria.service.CajaLogicaPorCajaFisicaService;
import com.saa.model.tesoreria.CajaLogicaPorCajaFisica;
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

@Path("ccxc")
public class CajaLogicaPorCajaFisicaRest {

    @EJB
    private CajaLogicaPorCajaFisicaDaoService cajaLogicaPorCajaFisicaDaoService;

    @EJB
    private CajaLogicaPorCajaFisicaService cajaLogicaPorCajaFisicaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CajaLogicaPorCajaFisicaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CajaLogicaPorCajaFisica.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CajaLogicaPorCajaFisica> getAll() throws Throwable {
        return cajaLogicaPorCajaFisicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_LOGICA_POR_CAJA_FISICA);
    }

    /**
     * Recupera un registro de CajaLogicaPorCajaFisica por ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CajaLogicaPorCajaFisica getId(@PathParam("id") Long id) throws Throwable {
        return cajaLogicaPorCajaFisicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_LOGICA_POR_CAJA_FISICA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CajaLogicaPorCajaFisica put(CajaLogicaPorCajaFisica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CAJA_LOGICA_POR_CAJA_FISICA");
        return cajaLogicaPorCajaFisicaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CajaLogicaPorCajaFisica post(CajaLogicaPorCajaFisica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CAJA_LOGICA_POR_CAJA_FISICA");
        return cajaLogicaPorCajaFisicaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CajaLogicaPorCajaFisicaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CAJA_LOGICA_POR_CAJA_FISICA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cajaLogicaPorCajaFisicaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
    

    /**
     * Elimina un registro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CAJA_LOGICA_POR_CAJA_FISICA");
        CajaLogicaPorCajaFisica elimina = new CajaLogicaPorCajaFisica();
        cajaLogicaPorCajaFisicaDaoService.remove(elimina, id);
    }
}
