package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TelefonoDireccionDaoService;
import com.saa.ejb.tesoreria.service.TelefonoDireccionService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TelefonoDireccion;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TelefonoDireccion> lista = telefonoDireccionDaoService.selectAll(NombreEntidadesTesoreria.TELEFONO_DIRECCION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener teléfonos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de TelefonoDireccion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TelefonoDireccion telefonoDireccion = telefonoDireccionDaoService.selectById(id, NombreEntidadesTesoreria.TELEFONO_DIRECCION);
            if (telefonoDireccion == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TelefonoDireccion con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(telefonoDireccion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener teléfono: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TelefonoDireccion registro) {
        System.out.println("LLEGA AL SERVICIO PUT TELEFONO_DIRECCION");
        try {
            TelefonoDireccion resultado = telefonoDireccionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar teléfono: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TelefonoDireccion registro) {
        System.out.println("LLEGA AL SERVICIO POST TELEFONO_DIRECCION");
        try {
            TelefonoDireccion resultado = telefonoDireccionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear teléfono: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of TelefonoDireccionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TELEFONO_DIRECCION");
        try {
            return Response.status(Response.Status.OK)
                    .entity(telefonoDireccionService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de TelefonoDireccion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE TELEFONO_DIRECCION");
        try {
            TelefonoDireccion elimina = new TelefonoDireccion();
            telefonoDireccionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar teléfono: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
