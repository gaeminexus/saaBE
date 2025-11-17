package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.ejb.credito.dao.TipoIdentificacionDaoService;
import com.saa.ejb.credito.service.TipoIdentificacionService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TipoIdentificacion;

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

@Path("tpdn")
public class TipoIdentificacionRest {
    
    @EJB
    private TipoIdentificacionDaoService tipoIdentificacionDaoService;
    
    @EJB
    private TipoIdentificacionService tipoIdentificacionService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public TipoIdentificacionRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de TipoIdentificacion.
     * 
     * @return Lista de TipoIdentificacion
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoIdentificacion> getAll() throws Throwable {
        return tipoIdentificacionDaoService.selectAll(NombreEntidadesCredito.TIPO_IDENTIFICACION);
    }
    
    /**
     * Obtiene un registro de TipoIdentificacion por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto TipoIdentificacion
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public TipoIdentificacion getId(@PathParam("id") Long id) throws Throwable {
        return tipoIdentificacionDaoService.selectById(id, NombreEntidadesCredito.TIPO_IDENTIFICACION);
    }
    
    /**
     * Crea o actualiza un registro de TipoIdentificacion (PUT).
     * 
     * @param registro Objeto TipoIdentificacion
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public TipoIdentificacion put(TipoIdentificacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE TipoIdentificacion");
        return tipoIdentificacionService.saveSingle(registro);
    }
    
    /**
     * Crea o actualiza un registro de TipoIdentificacion (POST).
     * 
     * @param registro Objeto TipoIdentificacion
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public TipoIdentificacion post(TipoIdentificacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE TipoIdentificacion");
        return tipoIdentificacionService.saveSingle(registro);
    }
    
    /**
     * Consulta registros de TipoIdentificacion por criterios (dummy method para pruebas).
     * 
     * @param test Parámetro de prueba
     * @return Lista de TipoIdentificacion
     * @throws Throwable
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TipoIdentificacion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DE TipoIdentificacion: " + test);
        return tipoIdentificacionDaoService.selectAll(NombreEntidadesCredito.TIPO_IDENTIFICACION);
    }
    
    /**
     * Elimina un registro de TipoIdentificacion por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE TipoIdentificacion");
        TipoIdentificacion elimina = new TipoIdentificacion();
        tipoIdentificacionDaoService.remove(elimina, id);
    }
}
