package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroTarjetaDaoService;
import com.saa.ejb.tesoreria.service.TempCobroTarjetaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroTarjeta;

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

@Path("tctj")
public class TempCobroTarjetaRest {

    @EJB
    private TempCobroTarjetaDaoService tempCobroTarjetaDaoService;

    @EJB
    private TempCobroTarjetaService tempCobroTarjetaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroTarjetaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroTarjeta.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobroTarjeta> getAll() throws Throwable {
        return tempCobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
    }

    /**
     * Recupera un registro de TempCobroTarjeta por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobroTarjeta getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroTarjetaDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobroTarjeta put(TempCobroTarjeta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO_TARJETA");
        return tempCobroTarjetaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobroTarjeta post(TempCobroTarjeta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO_TARJETA");
        return tempCobroTarjetaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempCobroTarjetaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_COBRO_TARJETA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempCobroTarjetaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de TempCobroTarjeta por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO_TARJETA");
        TempCobroTarjeta elimina = new TempCobroTarjeta();
        tempCobroTarjetaDaoService.remove(elimina, id);
    }
}
