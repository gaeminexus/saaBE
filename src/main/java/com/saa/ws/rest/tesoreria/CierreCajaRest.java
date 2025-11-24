package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CierreCajaDaoService;
import com.saa.ejb.tesoreria.service.CierreCajaService;
import com.saa.model.tesoreria.CierreCaja;
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

@Path("crcj")
public class CierreCajaRest {

    @EJB
    private CierreCajaDaoService cierreCajaDaoService;

    @EJB
    private CierreCajaService cierreCajaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CierreCajaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CierreCaja.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CierreCaja> getAll() throws Throwable {
        return cierreCajaDaoService.selectAll(NombreEntidadesTesoreria.CIERRE_CAJA);
    }

    /**
     * Recupera un registro de CierreCaja por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CierreCaja getId(@PathParam("id") Long id) throws Throwable {
        return cierreCajaDaoService.selectById(id, NombreEntidadesTesoreria.CIERRE_CAJA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CierreCaja put(CierreCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CIERRE CAJA");
        return cierreCajaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CierreCaja post(CierreCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CIERRE CAJA");
        return cierreCajaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CierreCajaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CIERRE_CAJA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cierreCajaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de CierreCaja por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CIERRE CAJA");
        CierreCaja elimina = new CierreCaja();
        cierreCajaDaoService.remove(elimina, id);
    }
}
