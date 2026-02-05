package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.MayorizacionDaoService;
import com.saa.ejb.cnt.service.MayorizacionService;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.NombreEntidadesContabilidad;

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

@Path("myrz")
public class MayorizacionRest {

    @EJB
    private MayorizacionDaoService mayorizacionDaoService;

    @EJB
    private MayorizacionService mayorizacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public MayorizacionRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of MayorizacionRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Mayorizacion> lista = mayorizacionDaoService.selectAll(NombreEntidadesContabilidad.MAYORIZACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener mayorizaciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Mayorizacion mayorizacion = mayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.MAYORIZACION);
            if (mayorizacion == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Mayorización con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(mayorizacion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT method for updating or creating an instance of MayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Mayorizacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - MAYORIZACION");
        try {
            Mayorizacion resultado = mayorizacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Mayorizacion registro) {
        System.out.println("LLEGA AL SERVICIO POST - MAYORIZACION");
        try {
            Mayorizacion resultado = mayorizacionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of MayorizacionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de MAYORIZACION");
        try {
            return Response.status(Response.Status.OK)
                    .entity(mayorizacionService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - MAYORIZACION");
        try {
            Mayorizacion elimina = new Mayorizacion();
            mayorizacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar mayorización: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
