package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.ejb.credito.dao.FilialDaoService;
import com.saa.ejb.credito.service.FilialService;
import com.saa.model.credito.Filial;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("flll")
public class FilialRest {
    
    @EJB
    private FilialDaoService filialDaoService;
    
    @EJB
    private FilialService filialService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public FilialRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de Filial.
     * 
     * @return Lista de Filial
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Filial> getAll() throws Throwable {
        return filialDaoService.selectAll(NombreEntidadesCredito.FILIAL);
    }
    
    /**
     * Obtiene un registro de Filial por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto Filial
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public Filial getId(@PathParam("id") Long id) throws Throwable {
        return filialDaoService.selectById(id, NombreEntidadesCredito.FILIAL);
    }
    
    /**
     * Crea o actualiza un registro de Filial (PUT).
     * 
     * @param registro Objeto Filial
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public Filial put(Filial registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE Filial");
        return filialService.saveSingle(registro);
    }
    
    /**
     * Crea o actualiza un registro de Filial (POST).
     * 
     * @param registro Objeto Filial
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public Filial post(Filial registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE Filial");
        return filialService.saveSingle(registro);
    }
    
    /**
     * Consulta registros de Filial por criterios (dummy method para pruebas).
     * 
     * @param test Parámetro de prueba
     * @return Lista de Filial
     * @throws Throwable
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Filial> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DE Filial: " + test);
        return filialDaoService.selectAll(NombreEntidadesCredito.FILIAL);
    }
    
    /**
     * Elimina un registro de Filial por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE Filial");
        Filial elimina = new Filial();
        filialDaoService.remove(elimina, id);
    }
}
