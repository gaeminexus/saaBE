package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DireccionPersonaDaoService;
import com.saa.ejb.tesoreria.service.DireccionPersonaService;
import com.saa.model.tsr.DireccionPersona;
import com.saa.model.tsr.NombreEntidadesTesoreria;

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

@Path("pdrc")
public class DireccionPersonaRest {

    @EJB
    private DireccionPersonaDaoService direccionPersonaDaoService;

    @EJB
    private DireccionPersonaService direccionPersonaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DireccionPersonaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DireccionPersona.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DireccionPersona> lista = direccionPersonaDaoService.selectAll(NombreEntidadesTesoreria.DIRECCION_PERSONA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener direcciones de personas: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DireccionPersona direccionPersona = direccionPersonaDaoService.selectById(id, NombreEntidadesTesoreria.DIRECCION_PERSONA);
            if (direccionPersona == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("DireccionPersona con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(direccionPersona).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener dirección de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DireccionPersona registro) {
        System.out.println("LLEGA AL SERVICIO PUT DIRECCION PERSONA");
        try {
            DireccionPersona resultado = direccionPersonaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar dirección de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DireccionPersona registro) {
        System.out.println("LLEGA AL SERVICIO POST DIRECCION PERSONA");
        try {
            DireccionPersona resultado = direccionPersonaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear dirección de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
/**
 * POST method for updating or creating an instance of DireccionPersonaRest
 *
 * @param content representation for the resource
 * @return an HTTP response with content of the updated or created resource.
 */
@POST
@Path("selectByCriteria")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response selectByCriteria(List<DatosBusqueda> registros) {
    System.out.println("selectByCriteria de DIRECCION_PERSONA");
    try {
        return Response.status(Response.Status.OK)
                .entity(direccionPersonaService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON).build();
    } catch (Throwable e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
    }
}

    /**
     * Elimina un registro de DireccionPersona por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DIRECCION PERSONA");
        try {
            DireccionPersona elimina = new DireccionPersona();
            direccionPersonaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar dirección de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
