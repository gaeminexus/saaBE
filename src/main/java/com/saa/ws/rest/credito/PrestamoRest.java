package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.PrestamoDaoService;
import com.saa.ejb.credito.service.PrestamoService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.Prestamo;

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
     * @return Lista de Prestamo
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Prestamo> getAll() throws Throwable {
        return prestamoDaoService.selectAll(NombreEntidadesCredito.PRESTAMO);
    }
    
    /**
     * Obtiene un registro de Prestamo por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto Prestamo
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public Prestamo getId(@PathParam("id") Long id) throws Throwable {
        return prestamoDaoService.selectById(id, NombreEntidadesCredito.PRESTAMO);
    }
    
    /**
     * Crea o actualiza un registro de Prestamo (PUT).
     * 
     * @param registro Objeto Prestamo
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public Prestamo put(Prestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE Prestamo");
        return prestamoService.saveSingle(registro);
    }
    
    /**
     * Crea o actualiza un registro de Prestamo (POST).
     * 
     * @param registro Objeto Prestamo
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public Prestamo post(Prestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE Prestamo");
        return prestamoService.saveSingle(registro);
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
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE Prestamo");
        Prestamo elimina = new Prestamo();
        prestamoDaoService.remove(elimina, id);
    }
}
