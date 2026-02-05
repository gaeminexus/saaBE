package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;

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

@Path("prst")
public class PrestamoRest {
    
    @EJB
    private PrestamoDaoService prestamoDaoService;
    
    @EJB
    private PrestamoService prestamoService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public PrestamoRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de Prestamo.
     * 
     * @return Response con lista de Prestamo
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Prestamo> prestamos = prestamoDaoService.selectAll(NombreEntidadesCredito.PRESTAMO);
            return Response.status(Response.Status.OK)
                    .entity(prestamos)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener préstamos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Obtiene un registro de Prestamo por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Prestamo
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            Prestamo prestamo = prestamoDaoService.selectById(id, NombreEntidadesCredito.PRESTAMO);
            if (prestamo == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Prestamo con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(prestamo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Prestamo (PUT).
     * 
     * @param registro Objeto Prestamo
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Prestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE Prestamo");
        try {
            Prestamo resultado = prestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Prestamo (POST).
     * 
     * @param registro Objeto Prestamo
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Prestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST DE Prestamo");
        try {
            Prestamo resultado = prestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Consulta registros de Prestamo por criterios (dummy method para pruebas).
     * 
     * @param test Parámetro de prueba
     * @return Lista de Prestamo
     * @throws Throwable
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Prestamo Cuenta");
        Response respuesta = null;
    	try {
    		respuesta = Response.status(Response.Status.OK).entity(prestamoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    	return respuesta;
    }
    
    /**
     * Elimina un registro de Prestamo por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE Prestamo");
        try {
            Prestamo elimina = new Prestamo();
            prestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
