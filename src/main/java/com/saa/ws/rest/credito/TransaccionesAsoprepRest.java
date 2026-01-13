package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TransaccionesAsoprepDaoService;
import com.saa.ejb.credito.service.TransaccionesAsoprepService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TransaccionesAsoprep;

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

@Path("tras")
public class TransaccionesAsoprepRest {
    
    @EJB
    private TransaccionesAsoprepDaoService transaccionesAsoprepDaoService;
    
    @EJB
    private TransaccionesAsoprepService transaccionesAsoprepService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public TransaccionesAsoprepRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de Transacciones
     * 
     * @return Lista de Transacciones
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TransaccionesAsoprep> lista = transaccionesAsoprepDaoService.selectAll(NombreEntidadesCredito.TRANSACCIONES_ASOPREP);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipos de vivienda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            TransaccionesAsoprep tipo = transaccionesAsoprepDaoService.selectById(id, NombreEntidadesCredito.TRANSACCIONES_ASOPREP);
            if (tipo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Transacciones con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tipo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener Transacciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TransaccionesAsoprep registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE Transacciones");
        try {
            TransaccionesAsoprep resultado = transaccionesAsoprepService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar Transacciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TransaccionesAsoprep registro) {
        System.out.println("LLEGA AL SERVICIO POST DE Transacciones");
        try {
            TransaccionesAsoprep resultado = transaccionesAsoprepService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear Transacciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Transacciones");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(transaccionesAsoprepService.selectByCriteria(registros))
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
     * Elimina un registro de Transacciones por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE Transacciones");
        try {
            TransaccionesAsoprep elimina = new TransaccionesAsoprep();
            transaccionesAsoprepDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar Transacciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
