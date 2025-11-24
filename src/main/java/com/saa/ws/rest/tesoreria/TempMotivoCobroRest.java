package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempMotivoCobroDaoService;
import com.saa.ejb.tesoreria.service.TempMotivoCobroService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempMotivoCobro;

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

@Path("tcmt")
public class TempMotivoCobroRest {

    @EJB
    private TempMotivoCobroDaoService tempMotivoCobroDaoService;

    @EJB
    private TempMotivoCobroService tempMotivoCobroService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempMotivoCobroRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempMotivoCobro.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempMotivoCobro> getAll() throws Throwable {
        return tempMotivoCobroDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_COBRO);
    }

    /**
     * Recupera un registro de TempMotivoCobro por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempMotivoCobro getId(@PathParam("id") Long id) throws Throwable {
        return tempMotivoCobroDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_MOTIVO_COBRO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempMotivoCobro put(TempMotivoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_MOTIVO_COBRO");
        return tempMotivoCobroService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempMotivoCobro post(TempMotivoCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_MOTIVO_COBRO");
        return tempMotivoCobroService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempMotivoCobroRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_MOTIVO_COBRO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempMotivoCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
    /**
     * Elimina un registro de TempMotivoCobro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_MOTIVO_COBRO");
        TempMotivoCobro elimina = new TempMotivoCobro();
        tempMotivoCobroDaoService.remove(elimina, id);
    }
}
