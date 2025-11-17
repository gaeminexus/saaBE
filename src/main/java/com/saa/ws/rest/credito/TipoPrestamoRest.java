package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoPrestamoDaoService;
import com.saa.ejb.credito.service.TipoPrestamoService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TipoPrestamo;

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

@Path("tppr")
public class TipoPrestamoRest {
    
    @EJB
    private TipoPrestamoDaoService tipoPrestamoDaoService;
    
    @EJB
    private TipoPrestamoService tipoPrestamoService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public TipoPrestamoRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de TipoPrestamo.
     * 
     * @return Lista de TipoPrestamo
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoPrestamo> getAll() throws Throwable {
        return tipoPrestamoDaoService.selectAll(NombreEntidadesCredito.TIPO_PRESTAMO);
    }
    
    /**
     * Obtiene un registro de TipoPrestamo por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto TipoPrestamo
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public TipoPrestamo getId(@PathParam("id") Long id) throws Throwable {
        return tipoPrestamoDaoService.selectById(id, NombreEntidadesCredito.TIPO_PRESTAMO);
    }
    
    /**
     * Crea o actualiza un registro de TipoPrestamo (PUT).
     * 
     * @param registro Objeto TipoPrestamo
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public TipoPrestamo put(TipoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE TipoPrestamo");
        return tipoPrestamoService.saveSingle(registro);
    }
    
    /**
     * Crea o actualiza un registro de TipoPrestamo (POST).
     * 
     * @param registro Objeto TipoPrestamo
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public TipoPrestamo post(TipoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE TipoPrestamo");
        return tipoPrestamoService.saveSingle(registro);
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoPrestamo");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoPrestamoService.selectByCriteria(registros))
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
     * Elimina un registro de TipoPrestamo por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE TipoPrestamo");
        TipoPrestamo elimina = new TipoPrestamo();
        tipoPrestamoDaoService.remove(elimina, id);
    }
}
