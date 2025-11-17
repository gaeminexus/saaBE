package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TelefonoDireccionDaoService;
import com.saa.ejb.tesoreria.service.TelefonoDireccionService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TelefonoDireccion;

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
import jakarta.ws.rs.core.UriInfo;

@Path("pcnt")
public class TelefonoDireccionRest {

    @EJB
    private TelefonoDireccionDaoService telefonoDireccionDaoService;

    @EJB
    private TelefonoDireccionService telefonoDireccionService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TelefonoDireccionRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TelefonoDireccion.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TelefonoDireccion> getAll() throws Throwable {
        return telefonoDireccionDaoService.selectAll(NombreEntidadesTesoreria.TELEFONO_DIRECCION);
    }

    /**
     * Recupera un registro de TelefonoDireccion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TelefonoDireccion getId(@PathParam("id") Long id) throws Throwable {
        return telefonoDireccionDaoService.selectById(id, NombreEntidadesTesoreria.TELEFONO_DIRECCION);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TelefonoDireccion put(TelefonoDireccion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TELEFONO_DIRECCION");
        return telefonoDireccionService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TelefonoDireccion post(TelefonoDireccion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TELEFONO_DIRECCION");
        return telefonoDireccionService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TelefonoDireccion por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TelefonoDireccion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TELEFONO_DIRECCION: " + test);
        return telefonoDireccionDaoService.selectAll(NombreEntidadesTesoreria.TELEFONO_DIRECCION);
    }

    /**
     * Elimina un registro de TelefonoDireccion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TELEFONO_DIRECCION");
        TelefonoDireccion elimina = new TelefonoDireccion();
        telefonoDireccionDaoService.remove(elimina, id);
    }
}
