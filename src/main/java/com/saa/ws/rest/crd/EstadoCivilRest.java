package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.EstadoCivilDaoService;
import com.saa.ejb.crd.service.EstadoCivilService;
import com.saa.model.crd.EstadoCivil;
import com.saa.model.crd.NombreEntidadesCredito;

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

@Path("escv")
public class EstadoCivilRest {
    
    @EJB
    private EstadoCivilDaoService estadoCivilDaoService;
    
    @EJB
    private EstadoCivilService estadoCivilService;
    
    @Context
    private UriInfo context;
    
    /**
     * Default constructor.
     */
    public EstadoCivilRest() {
        // TODO Auto-generated constructor stub
    }
    
    /**
     * Retrieves representation of an instance of EstadoCivilRest
     * 
     * @return an instance of List<EstadoCivil>
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<EstadoCivil> lista = estadoCivilDaoService.selectAll(NombreEntidadesCredito.ESTADO_CIVIL);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener estados civiles: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            EstadoCivil estado = estadoCivilDaoService.selectById(id, NombreEntidadesCredito.ESTADO_CIVIL);
            if (estado == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("EstadoCivil con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(estado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener estado civil: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(EstadoCivil registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE EstadoCivil");
        try {
            EstadoCivil resultado = estadoCivilService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar estado civil: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(EstadoCivil registro) {
        System.out.println("LLEGA AL SERVICIO POST DE EstadoCivil");
        try {
            EstadoCivil resultado = estadoCivilService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear estado civil: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de EstadoCivil");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
            		.entity(estadoCivilService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return respuesta;
    }
    
    /**
     * DELETE method for removing an instance of EstadoCivilRest
     * 
     * @param id Identifier of the resource
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE EstadoCivil");
        EstadoCivil elimina = new EstadoCivil();
        estadoCivilDaoService.remove(elimina, id);
    }
}
