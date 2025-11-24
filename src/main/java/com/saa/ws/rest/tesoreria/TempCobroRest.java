package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroDaoService;
import com.saa.ejb.tesoreria.service.TempCobroService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobro;

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

@Path("tcbr")
public class TempCobroRest {

    @EJB
    private TempCobroDaoService tempCobroDaoService;

    @EJB
    private TempCobroService tempCobroService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobro.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobro> getAll() throws Throwable {
        return tempCobroDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO);
    }

    /**
     * Recupera un registro de TempCobro por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobro getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobro put(TempCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO");
        return tempCobroService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobro post(TempCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO");
        return tempCobroService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempCobroRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_COBRO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de TempCobro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO");
        TempCobro elimina = new TempCobro();
        tempCobroDaoService.remove(elimina, id);
    }
}
