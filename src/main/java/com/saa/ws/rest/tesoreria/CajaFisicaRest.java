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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CajaFisica> lista = cajaFisicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_FISICA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cajas físicas: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CajaFisica cajaFisica = cajaFisicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_FISICA);
            if (cajaFisica == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CajaFisica con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cajaFisica).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener caja física: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CajaFisica registro) {
        System.out.println("LLEGA AL SERVICIO PUT CAJA_FISICA");
        try {
            CajaFisica resultado = cajaFisicaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar caja física: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CajaFisica registro) {
        System.out.println("LLEGA AL SERVICIO POST CAJA_FISICA");
        try {
            CajaFisica resultado = cajaFisicaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear caja física: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CAJA_FISICA");
        try {
            CajaFisica elimina = new CajaFisica();
            cajaFisicaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar caja física: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
