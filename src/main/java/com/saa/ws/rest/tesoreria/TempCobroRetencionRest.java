package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroRetencionDaoService;
import com.saa.ejb.tesoreria.service.TempCobroRetencionService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroRetencion;

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

@Path("tcrt")
public class TempCobroRetencionRest {

    @EJB
    private TempCobroRetencionDaoService tempCobroRetencionDaoService;

    @EJB
    private TempCobroRetencionService tempCobroRetencionService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroRetencionRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroRetencion.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobroRetencion> getAll() throws Throwable {
        return tempCobroRetencionDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_RETENCION);
    }

    /**
     * Recupera un registro de TempCobroRetencion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobroRetencion getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroRetencionDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_RETENCION);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobroRetencion put(TempCobroRetencion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO_RETENCION");
        return tempCobroRetencionService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobroRetencion post(TempCobroRetencion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO_RETENCION");
        return tempCobroRetencionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempCobroRetencionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_COBRO_RETENCION");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempCobroRetencionService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de TempCobroRetencion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO_RETENCION");
        TempCobroRetencion elimina = new TempCobroRetencion();
        tempCobroRetencionDaoService.remove(elimina, id);
    }
}
