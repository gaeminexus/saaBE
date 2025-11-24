package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CajaFisicaDaoService;
import com.saa.ejb.tesoreria.service.CajaFisicaService;
import com.saa.model.tesoreria.CajaFisica;
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

@Path("cjaa")
public class CajaFisicaRest {

    @EJB
    private CajaFisicaDaoService cajaFisicaDaoService;

    @EJB
    private CajaFisicaService cajaFisicaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CajaFisicaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CajaFisica.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CajaFisica> getAll() throws Throwable {
        return cajaFisicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_FISICA);
    }

    /**
     * Recupera un registro de CajaFisica por ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CajaFisica getId(@PathParam("id") Long id) throws Throwable {
        return cajaFisicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_FISICA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CajaFisica put(CajaFisica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CAJA_FISICA");
        return cajaFisicaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CajaFisica post(CajaFisica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CAJA_FISICA");
        return cajaFisicaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CajaFisicaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CAJA_FISICA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cajaFisicaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
    

    /**
     * Elimina un registro de CajaFisica por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CAJA_FISICA");
        CajaFisica elimina = new CajaFisica();
        cajaFisicaDaoService.remove(elimina, id);
    }
}
